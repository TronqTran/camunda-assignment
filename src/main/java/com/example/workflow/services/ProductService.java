package com.example.workflow.services;

import com.example.workflow.models.Product;
import com.example.workflow.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public Product save(Product product) {
        productRepository.save(product);
        return product;
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
}
