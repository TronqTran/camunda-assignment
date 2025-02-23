package com.example.workflow.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotifyOutOfStockService implements JavaDelegate {
    private final RestTemplate restTemplate = new RestTemplate();
    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        String customerName = (String) delegateExecution.getVariable("customerName");
        String notificationApiUrl = "http://localhost:8080/order/notifications/out-of-stock";

        Map<String, String> payload = new HashMap<>();
        payload.put("message", "Xin lỗi " + customerName + ", sản phẩm bạn đặt đã hết hàng.");

        ResponseEntity<String> response = restTemplate.postForEntity(notificationApiUrl, payload, String.class);

        System.out.println("Thông báo gửi đến khách hàng: " + response.getBody());
    }
}
