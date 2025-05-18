package com.example.orderprocessing.service;

import com.example.orderprocessing.enums.OrderEvent;
import com.example.orderprocessing.enums.OrderStatus;
import com.example.orderprocessing.model.Order;
import com.example.orderprocessing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowStateMachineService {

    @Qualifier("orderStateMachineFactory")
    private final StateMachineFactory<OrderStatus, OrderEvent> orderStateMachineFactory;
    private final OrderRepository orderRepository; // To persist state changes

    // This service manages the lifecycle of state machines for orders.

    public StateMachine<OrderStatus, OrderEvent> initializeStateMachine(Order order) {
        // Create a unique machine ID, e.g., based on order ID
        String machineId = "orderSM-" + order.getId();
        StateMachine<OrderStatus, OrderEvent> stateMachine = orderStateMachineFactory.getStateMachine(machineId);

        // Stop the machine if it's already running (e.g., from a previous instance or in-memory cache)
        if (stateMachine.getState() != null && !stateMachine.isComplete()) {
            stateMachine.stop();
        }

        // Reset the state machine to the order's current persisted state
        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(new StateMachineInterceptorAdapter<>() {
                        // This interceptor updates the Order entity's status after a successful state transition.
                        @Override
                        public void postStateChange(State<OrderStatus, OrderEvent> state, Message<OrderEvent> message,
                                                    Transition<OrderStatus, OrderEvent> transition,
                                                    StateMachine<OrderStatus, OrderEvent> stateMachine,
                                                    StateMachine<OrderStatus, OrderEvent> rootStateMachine) {
                            log.debug("Interceptor: Order {} transitioned to state {}", order.getId(), state.getId());
                            if (order.getStatus() != state.getId()) {
                                order.setStatus(state.getId());
                                orderRepository.save(order); // Persist the new status
                                log.info("Order {} status updated to {} in database.", order.getId(), state.getId());
                            }
                        }

                        @Override
                        public Exception stateMachineError(StateMachine<OrderStatus, OrderEvent> machine, Exception exception) {
                            log.error("Error in Order StateMachine (ID: {}) for Order {}: {}", machine.getId(), order.getId(), exception.getMessage(), exception);
                            // Here you could potentially transition the order to an ERROR state or take other actions
                            return exception; // Propagate the exception
                        }
                    });
                    // Reset the state of the machine instance
                    sma.resetStateMachine(new DefaultStateMachineContext<>(order.getStatus(), null, null, null, null, machineId));
                });

        // Start the state machine instance
        stateMachine.start();
        log.info("Order StateMachine (ID: {}) initialized and started for Order ID: {} in state: {}", machineId, order.getId(), stateMachine.getState().getId());
        return stateMachine;
    }


    @Transactional // Important: ensures that event sending and DB updates are atomic if possible
    public boolean sendEvent(Order order, OrderEvent event, String reason) {
        StateMachine<OrderStatus, OrderEvent> stateMachine = initializeStateMachine(order); // Ensures SM is current

        Map<String, Object> headers = new HashMap<>();
        headers.put("ORDER_ID", order.getId()); // Pass order ID for actions/guards
        if (reason != null) {
            headers.put("REASON", reason);
        }

        // Special handling for ALL_TASKS_COMPLETED guard in OrderStateMachineConfig
        if (OrderEvent.ALL_TASKS_COMPLETED.equals(event)) {
            // Here, the OrderService (caller) should ensure all tasks are indeed completed.
            // We are passing a flag via message header for the guard.
            headers.put("allTasksCompleted", true); // This is what the guard will check
        }

        Message<OrderEvent> message = MessageBuilder
                .withPayload(event)
                .copyHeaders(headers)
                .build();

        log.info("Sending event {} to Order StateMachine (ID: {}) for Order ID: {} with current state: {}",
                event, stateMachine.getId(), order.getId(), stateMachine.getState().getId());

        boolean eventAccepted = stateMachine.sendEvent(message);

        if (eventAccepted) {
            log.info("Event {} accepted by Order StateMachine (ID: {}) for Order ID: {}. New state: {}",
                    event, stateMachine.getId(), order.getId(), stateMachine.getState().getId());
            // The interceptor should have handled saving the new state to the Order entity.
        } else {
            log.warn("Event {} NOT accepted by Order StateMachine (ID: {}) for Order ID: {} in state: {}",
                    event, stateMachine.getId(), order.getId(), stateMachine.getState().getId());
        }

        // Stop the machine instance after processing to free up resources
        // This is a common pattern for per-entity state machines that are not long-lived in memory.
        // The state is persisted in the Order entity.
        stateMachine.stop();
        log.debug("Order StateMachine (ID: {}) stopped for Order ID: {}", stateMachine.getId(), order.getId());

        return eventAccepted;
    }
}