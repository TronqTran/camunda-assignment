package com.example.workflow.delegates;


import com.example.workflow.emuns.OrderStatus;
import com.example.workflow.models.Order;
import com.example.workflow.models.Product;
import com.example.workflow.repositories.ProductRepository;
import com.example.workflow.services.OrderService;
import com.example.workflow.services.ProductService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class CheckInventoryTaskDelegate implements JavaDelegate {
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;
    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        Long orderId = (Long) delegateExecution.getVariable("orderId"); // Lấy orderId từ process variables
        Order order = orderService.findById(orderId).orElse(null);

        if (order == null) {
            throw new RuntimeException("Order ID " + orderId + " không tồn tại!");
        }

        List<Map<String, Object>> orderProducts = (List<Map<String, Object>>) delegateExecution.getVariable("orderProducts");
        boolean enoughStock = true;

        for (Map<String, Object> productData : orderProducts) {
            Long productId = ((Number) productData.get("productId")).longValue();
            int requestedQuantity = (int) productData.get("requestedQuantity");

            Product product = productService.findById(productId).orElse(null);
            if (product == null || product.getStockQuantity() < requestedQuantity) {
                enoughStock = false;
                break;
            }
        }

        if (enoughStock) {
            order.setStatus(OrderStatus.PROCESSING);
            orderService.save(order);
            System.out.println("Order " + orderId + " chuyển trạng thái: PROCESSING");
        } else {
            order.setStatus(OrderStatus.REJECTED);
            orderService.save(order);
            System.out.println("Order " + orderId + " bị từ chối do không đủ hàng.");
        }
        delegateExecution.setVariable("enoughStock", enoughStock);
    }
}
