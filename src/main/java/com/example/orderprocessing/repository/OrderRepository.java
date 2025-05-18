package com.example.orderprocessing.repository;

import com.example.orderprocessing.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // You can add custom query methods here if needed
    // For example:
    // List<Order> findByStatus(OrderStatus status);
    // Optional<Order> findByCustomerId(String customerId); // If you add customerId
}