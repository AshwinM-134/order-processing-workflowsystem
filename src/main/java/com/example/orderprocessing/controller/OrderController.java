package com.example.orderprocessing.controller;

import com.example.orderprocessing.dto.*;
import com.example.orderprocessing.model.Order;
import com.example.orderprocessing.model.Task;
import com.example.orderprocessing.service.OrderService;
import com.example.orderprocessing.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final TaskService taskService; // For fetching tasks related to an order

    // --- DTO Mapping Utilities (could be moved to a dedicated mapper class) ---
    private OrderDto convertToDto(Order order) {
        if (order == null) return null;
        List<TaskDto> taskDtos = order.getTasks() != null ?
                order.getTasks().stream().map(this::convertTaskToDto).collect(Collectors.toList()) :
                List.of();
        return new OrderDto(
                order.getId(),
                order.getStatus(),
                order.getCreatedDate(),
                order.getUpdatedDate(),
                order.getMetadata(),
                taskDtos
        );
    }

    private TaskDto convertTaskToDto(Task task) {
        if (task == null) return null;
        return new TaskDto(
                task.getId(),
                task.getOrder() != null ? task.getOrder().getId() : null,
                task.getTaskType(),
                task.getStatus(),
                task.getCreatedDate(),
                task.getUpdatedDate(),
                task.getCompletedDate()
        );
    }

    // --- CRUD Endpoints ---
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        // Add validation for createOrderRequest if needed (e.g., using @Valid)
        Order order = orderService.createOrder(createOrderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        List<OrderDto> orderDtos = orderService.getAllOrders().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(orderDtos);
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderDto> updateOrderMetadata(@PathVariable Long orderId, @RequestBody UpdateOrderRequest updateOrderRequest) {
        try {
            Order updatedOrder = orderService.updateOrder(orderId, updateOrderRequest);
            return ResponseEntity.ok(convertToDto(updatedOrder));
        } catch (RuntimeException e) { // Replace with specific exceptions
            return ResponseEntity.notFound().build();
        }
    }

    // --- State Machine Event Endpoints ---
    @PostMapping("/{orderId}/event")
    public ResponseEntity<?> sendOrderEvent(@PathVariable Long orderId, @RequestBody OrderEventRequest eventRequest) {
        try {
            // TODO: Add authorization/validation: Who can send which event?
            boolean success = orderService.sendOrderEvent(orderId, eventRequest.getEvent(), eventRequest.getReason());
            if (success) {
                Order updatedOrder = orderService.getOrderById(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found after event processing: " + orderId));
                return ResponseEntity.ok(convertToDto(updatedOrder));
            } else {
                // Event not accepted by state machine in its current state
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Event " + eventRequest.getEvent() + " not accepted for order " + orderId + " in its current state.");
            }
        } catch (RuntimeException e) { // Catch exceptions like OrderNotFound
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event: " + e.getMessage());
        }
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<?> completeOrder(@PathVariable Long orderId) {
        try {
            Order completedOrder = orderService.completeOrder(orderId);
            return ResponseEntity.ok(convertToDto(completedOrder));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error completing order: " + e.getMessage());
        }
    }


    // --- Task related endpoints (scoped under order) ---
    @GetMapping("/{orderId}/tasks")
    public ResponseEntity<List<TaskDto>> getTasksForOrder(@PathVariable Long orderId) {
        // Check if order exists first
        if (orderService.getOrderById(orderId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<TaskDto> taskDtos = taskService.getTasksForOrder(orderId).stream()
                .map(this::convertTaskToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(taskDtos);
    }
}
