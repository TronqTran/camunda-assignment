package com.example.workflow.delegates;

import com.example.workflow.emuns.OrderStatus;
import com.example.workflow.models.Order;
import com.example.workflow.services.OrderService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompleteOrderTaskDelegate implements JavaDelegate {

    @Autowired
    private OrderService orderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        System.out.println("Hoàn thành đơn hàng...");

        Long orderId = (Long) delegateExecution.getVariable("orderId");
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found!"));

        // Cập nhật trạng thái đơn hàng thành COMPLETED
        order.setStatus(OrderStatus.COMPLETED);
        orderService.save(order);

        System.out.println("Đơn hàng " + orderId + " đã hoàn thành.");
    }
}
