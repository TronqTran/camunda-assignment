package com.example.workflow.services;

import com.example.workflow.models.OrderProduct;
import com.example.workflow.repositories.OrderProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderProductService {
    @Autowired
    private OrderProductRepository orderProductRepository;

    public OrderProduct save(OrderProduct orderProduct) {
        orderProductRepository.save(orderProduct);
        return orderProduct;
    }
}
