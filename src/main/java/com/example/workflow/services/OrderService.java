package com.example.workflow.services;

import com.example.workflow.models.Order;
import com.example.workflow.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    public Order save(Order order) {
        orderRepository.save(order);
        return order;
    }
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }
}
