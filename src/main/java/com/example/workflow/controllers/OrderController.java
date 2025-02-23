package com.example.workflow.controllers;

import com.example.workflow.emuns.OrderStatus;
import com.example.workflow.models.Order;
import com.example.workflow.models.OrderProduct;
import com.example.workflow.models.Product;
import com.example.workflow.services.OrderService;
import com.example.workflow.services.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;

    @PostMapping("/start")
    public ResponseEntity<?> startOrder() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("orderProcess");

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .list();

        if (tasks.isEmpty()) {
            return ResponseEntity.badRequest().body("Process started but no user task found!");
        }

        String taskId = tasks.get(0).getId();

        return ResponseEntity.ok(Map.of(
                "message", "Order process started!",
                "nextTaskId", taskId,
                "processInstanceId", processInstance.getId()
        ));
    }

    @PostMapping("/{taskId}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable("taskId") String taskId, @RequestBody Order order) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return ResponseEntity.badRequest().body("Task ID " + taskId + " not found or already completed.");
            }

            String processInstanceId = task.getProcessInstanceId();

            // Kiểm tra tồn kho và tạo đơn hàng
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", order.getCustomerName());

            List<Map<String, Object>> orderProductsList = new ArrayList<>();
            List<OrderProduct> orderProducts = new ArrayList<>();

            Order newOrder = new Order();
            newOrder.setCustomerName(order.getCustomerName());
            newOrder.setCreatedAt(LocalDateTime.now());
            newOrder.setStatus(OrderStatus.PENDING);

            for (OrderProduct orderProduct : order.getOrderProducts()) {
                Optional<Product> productOpt = productService.findById(orderProduct.getProduct().getId());

                Product product = productOpt.get();


                // Lưu OrderProduct vào DB
                OrderProduct newOrderProduct = new OrderProduct();
                newOrderProduct.setOrder(newOrder);
                newOrderProduct.setProduct(product);
                newOrderProduct.setRequestedQuantity(orderProduct.getRequestedQuantity());
                orderProducts.add(newOrderProduct);

                // Lưu vào Camunda
                Map<String, Object> productData = new HashMap<>();
                productData.put("productId", product.getId());
                productData.put("name", product.getName());
                productData.put("requestedQuantity", orderProduct.getRequestedQuantity());
                productData.put("stockQuantity", product.getStockQuantity());
                orderProductsList.add(productData);
            }

            newOrder.setOrderProducts(orderProducts);
            orderService.save(newOrder);

            variables.put("orderProducts", orderProductsList);
            variables.put("orderId", newOrder.getId());
            taskService.complete(taskId, variables);

            List<Task> nextTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .orderByTaskCreateTime()
                    .desc()
                    .list();

            if (nextTasks.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "Out of stock! Order rejected.",
                        "taskId", taskId,
                        "processInstanceId", processInstanceId
                ));
            }

            String nextTaskId = nextTasks.get(0).getId();

            return ResponseEntity.ok(Map.of(
                    "message", "Order confirmed and saved!",
                    "orderId", newOrder.getId(),
                    "nextTaskId", nextTaskId,
                    "processInstanceId", processInstanceId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error completing task " + taskId + ": " + e.getMessage());
        }
    }



    @PostMapping("/notifications/out-of-stock")
    public ResponseEntity<?> notifyOutOfStock(@RequestBody Map<String, Object> notification) {
        System.out.println("Gửi thông báo đến khách hàng: " + notification.get("message"));
        return ResponseEntity.ok("Thông báo đã được gửi!");
    }

    @PostMapping("/{taskId}/choose-payment")
    public ResponseEntity<?> choosePayment(@PathVariable("taskId") String taskId, @RequestParam("paymentMethod") String paymentMethod) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return ResponseEntity.badRequest().body("Task ID " + taskId + " not found.");
        }
        String processInstanceId = task.getProcessInstanceId();

        // Đặt biến paymentMethod vào quy trình
        Map<String, Object> variables = new HashMap<>();
        variables.put("paymentMethod", paymentMethod);

        // Hoàn thành task hiện tại
        taskService.complete(taskId, variables);

        // Truy vấn task tiếp theo của cùng processInstance
        List<Task> nextTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        if (nextTasks.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "taskId", taskId,
                    "message", "Order confirmed! No next task found.",
                    "processInstanceId", processInstanceId
            ));
        }

        String nextTaskId = nextTasks.get(0).getId();

        // Trả về nextTaskId nếu tồn tại
        return ResponseEntity.ok(Map.of(
                "message", "Payment method selected: " + paymentMethod,
                "nextTaskId", nextTaskId,
                "processInstanceId", processInstanceId
        ));
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable("taskId") String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return ResponseEntity.badRequest().body("Task ID " + taskId + " not found.");
        }

        String processInstanceId = task.getProcessInstanceId();

        // Lấy orderId từ process variables
        HistoricVariableInstance orderIdVar = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("orderId")
                .singleResult();

        if (orderIdVar == null) {
            return ResponseEntity.badRequest().body("Order ID not found in process instance.");
        }

        Long orderId = (Long) orderIdVar.getValue();

        // Cập nhật trạng thái đơn hàng trong database
        Optional<Order> orderOpt = orderService.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(OrderStatus.COMPLETED);  // Cập nhật trạng thái đơn hàng
            orderService.save(order);
        } else {
            return ResponseEntity.badRequest().body("Order not found.");
        }

        // Hoàn thành User Task
        taskService.complete(taskId);

        return ResponseEntity.ok(Map.of(
                "message", "Order has been completed!",
                "orderId", orderId,
                "orderStatus", "COMPLETED",
                "taskId", taskId
        ));
    }


}
