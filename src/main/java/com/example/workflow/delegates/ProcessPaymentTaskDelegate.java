package com.example.workflow.delegates;

import com.example.workflow.emuns.OrderStatus;
import com.example.workflow.models.Order;
import com.example.workflow.services.OrderService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ProcessPaymentTaskDelegate implements JavaDelegate {
    @Autowired
    private OrderService orderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        Long orderId = (Long) delegateExecution.getVariable("orderId"); // Lấy orderId từ process variables
        Order order = orderService.findById(orderId).orElse(null);

        if (order == null) {
            throw new RuntimeException("Order ID " + orderId + " không tồn tại!");
        }

        int retryCount = delegateExecution.hasVariable("retryCount") ?
                (int) delegateExecution.getVariable("retryCount") : 0;

        // Gọi API thanh toán (giả lập ngẫu nhiên)
        Random random = new Random();
        boolean paymentSuccess = random.nextBoolean();

        delegateExecution.setVariable("paymentSuccess", paymentSuccess);
        delegateExecution.setVariable("retryCount", retryCount);

        if (paymentSuccess) {
            System.out.println("Thanh toán thành công, chuyển sang giao hàng.");
            order.setStatus(OrderStatus.PAYMENT);
            orderService.save(order);
        } else {
            retryCount++;
            delegateExecution.setVariable("retryCount", retryCount);

            if (retryCount < 3) {
                System.out.println("Thanh toán thất bại, thử lại lần " + retryCount);
            } else {
                System.out.println("Thanh toán thất bại sau 3 lần thử, hủy đơn.");
                order.setStatus(OrderStatus.CANCELLED);
                orderService.save(order);
            }
        }
    }
}

