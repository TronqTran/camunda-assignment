package com.example.workflow.delegates;

import com.example.workflow.models.Product;
import com.example.workflow.services.ProductService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProcessOrderCODDelegate implements JavaDelegate {

    @Autowired
    private ProductService productService;


    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        System.out.println("Xử lý đơn hàng COD...");

        List<Map<String, Object>> orderProducts = (List<Map<String, Object>>) delegateExecution.getVariable("orderProducts");

        for (Map<String, Object> productData : orderProducts) {
            Long productId = ((Number) productData.get("productId")).longValue();
            int requestedQuantity = (int) productData.get("requestedQuantity");

            Product product = productService.findById(productId).orElse(null);
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() - requestedQuantity);
                productService.save(product);
            }
        }

        System.out.println("Cập nhật kho thành công!");
    }
}
