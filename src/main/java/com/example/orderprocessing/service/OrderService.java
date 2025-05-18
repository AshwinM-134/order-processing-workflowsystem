package com.example.orderprocessing.service;

import com.example.orderprocessing.dto.CreateOrderRequest;
import com.example.orderprocessing.dto.UpdateOrderRequest;
import com.example.orderprocessing.enums.OrderEvent;
import com.example.orderprocessing.enums.OrderStatus;
import com.example.orderprocessing.enums.TaskType;
import com.example.orderprocessing.model.Order;
import com.example.orderprocessing.model.Task;
import com.example.orderprocessing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener; // Add this import
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WorkflowStateMachineService workflowStateMachineService;
    private final TaskService taskService; // To create initial tasks

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setMetadata(request.getMetadata());
        // Initial status is CREATED by default in Order entity constructor
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        // Initialize and start the state machine for this new order
        workflowStateMachineService.initializeStateMachine(savedOrder);
        // No event is sent here yet, creation IS the initial state.
        // An explicit PROCESS_ORDER event will be sent by the controller/client.

        // Create initial set of tasks based on order type or metadata (example)
        // This is a simplified example; in a real app, task creation might be more dynamic
        // or triggered by the state machine itself.
        Stream.of(TaskType.VALIDATE_ORDER_DETAILS, TaskType.PROCESS_PAYMENT, TaskType.CHECK_INVENTORY)
                .forEach(taskType -> taskService.createTask(savedOrder, taskType));

        log.info("Initial tasks created for order ID: {}", savedOrder.getId());
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateOrder(Long orderId, UpdateOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId)); // Replace with specific exception

        if (request.getMetadata() != null) {
            order.setMetadata(request.getMetadata());
        }
        // Updating status should go through the state machine
        Order updatedOrder = orderRepository.save(order);
        log.info("Order metadata updated for ID: {}", updatedOrder.getId());
        return updatedOrder;
    }

    // Method to trigger an event on the order's state machine
    // This is the primary way to change an order's status
    @Transactional
    public boolean sendOrderEvent(Long orderId, OrderEvent event, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId)); // Specific exception
        log.info("Attempting to send event {} to order {}", event, orderId);
        return workflowStateMachineService.sendEvent(order, event, reason);
    }


    // Convenience method, often called by the controller
    @Transactional
    public Order completeOrder(Long orderId) {
        if(sendOrderEvent(orderId, OrderEvent.COMPLETE_ORDER, null)){
            return getOrderById(orderId).orElseThrow(() -> new RuntimeException("Order disappeared after completion event: " + orderId));
        }
        throw new IllegalStateException("Could not transition order " + orderId + " to COMPLETED.");
    }

    // Example of how tasks being completed could trigger an order state change.
    // This method would be called by the TaskService when a task is completed.
    @Async // Make the listener asynchronous to avoid blocking the task completion transaction
    @EventListener
    @Transactional // This listener method will also run in a transaction
    public void handleTaskCompletion(TaskService.TaskCompletedApplicationEvent event) {
        Long orderId = event.getOrderId();
        Long taskId = event.getTaskId();

        log.info("Listener: Task {} completed for order {}. Checking if all tasks are done.", taskId, orderId);

        // It's good practice to re-fetch the order to ensure we have the latest state,
        // especially since this listener might run in a separate transaction or after some delay.
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found for task completion event: " + orderId));

        // Only proceed if the order is still in a state where it expects tasks to be completed (e.g., IN_PROGRESS)
        if (order.getStatus() == OrderStatus.IN_PROGRESS || order.getStatus() == OrderStatus.ON_HOLD) {
            if (taskService.areAllTasksCompletedForOrder(orderId)) {
                log.info("All tasks completed for order {}. Sending ALL_TASKS_COMPLETED event to order state machine.", orderId);
                // Send event to Order State Machine
                // The 'reason' parameter is null here, could be adapted if needed
                boolean eventSent = sendOrderEvent(orderId, OrderEvent.ALL_TASKS_COMPLETED, null);
                if (!eventSent) {
                    log.warn("Failed to send ALL_TASKS_COMPLETED event for order {} after all tasks were reported complete.", orderId);
                    // Potentially add retry logic or error handling here
                }
            } else {
                log.info("Order {} still has pending/in-progress tasks after task {} completion.", orderId, taskId);
            }
        } else {
            log.warn("Received task completion event for order {} (task {}), but order is in state {}, not expecting task completions.",
                    orderId, taskId, order.getStatus());
        }
    }
}