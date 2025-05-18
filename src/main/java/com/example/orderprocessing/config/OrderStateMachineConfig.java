package com.example.orderprocessing.config;

import com.example.orderprocessing.enums.OrderEvent;
import com.example.orderprocessing.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

@Slf4j
@Configuration
@EnableStateMachineFactory(name = "orderStateMachineFactory") // Named factory for clarity
public class OrderStateMachineConfig extends EnumStateMachineConfigurerAdapter<OrderStatus, OrderEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderStatus, OrderEvent> config) throws Exception {
        config
                .withConfiguration()
                .autoStartup(false) // State machines will be started manually when needed
                .listener(orderStateMachineListener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<OrderStatus, OrderEvent> states) throws Exception {
        states
                .withStates()
                .initial(OrderStatus.CREATED)
                .states(EnumSet.allOf(OrderStatus.class))
                .end(OrderStatus.COMPLETED)
                .end(OrderStatus.CANCELLED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStatus, OrderEvent> transitions) throws Exception {
        transitions
                // From CREATED
                .withExternal()
                .source(OrderStatus.CREATED).target(OrderStatus.PAYMENT_PENDING).event(OrderEvent.PROCESS_ORDER)
                .action(processOrderAction())
                .and()
                .withExternal()
                .source(OrderStatus.CREATED).target(OrderStatus.CANCELLED).event(OrderEvent.CANCEL_ORDER)
                .action(cancelOrderAction("Order cancelled at creation"))
                .and()

                // From PAYMENT_PENDING
                .withExternal()
                .source(OrderStatus.PAYMENT_PENDING).target(OrderStatus.IN_PROGRESS).event(OrderEvent.PAYMENT_SUCCESSFUL)
                .action(paymentSuccessfulAction())
                .and()
                .withExternal()
                .source(OrderStatus.PAYMENT_PENDING).target(OrderStatus.PAYMENT_FAILED).event(OrderEvent.PAYMENT_FAILED)
                .action(paymentFailedAction())
                .and()
                .withExternal()
                .source(OrderStatus.PAYMENT_PENDING).target(OrderStatus.CANCELLED).event(OrderEvent.CANCEL_ORDER)
                .action(cancelOrderAction("Order cancelled during payment pending"))
                .and()

                // From PAYMENT_FAILED (e.g., retry or cancel)
                .withExternal()
                .source(OrderStatus.PAYMENT_FAILED).target(OrderStatus.PAYMENT_PENDING).event(OrderEvent.RETRY_PAYMENT)
                .action(retryPaymentAction())
                .and()
                .withExternal()
                .source(OrderStatus.PAYMENT_FAILED).target(OrderStatus.CANCELLED).event(OrderEvent.CANCEL_ORDER)
                .action(cancelOrderAction("Order cancelled after payment failure"))
                .and()

                // From IN_PROGRESS (This is where tasks would typically be managed)
                .withExternal()
                .source(OrderStatus.IN_PROGRESS).target(OrderStatus.READY_FOR_SHIPMENT).event(OrderEvent.ALL_TASKS_COMPLETED)
                .guard(allTasksCompletedGuard()) // Example guard
                .action(readyForShipmentAction())
                .and()
                .withExternal()
                .source(OrderStatus.IN_PROGRESS).target(OrderStatus.CANCELLED).event(OrderEvent.CANCEL_ORDER)
                .action(cancelOrderAction("Order cancelled during progress"))
                .and()
                .withExternal()
                .source(OrderStatus.IN_PROGRESS).target(OrderStatus.ON_HOLD).event(OrderEvent.PLACE_ON_HOLD)
                .action(placeOnHoldAction())
                .and()

                // From ON_HOLD
                .withExternal()
                .source(OrderStatus.ON_HOLD).target(OrderStatus.IN_PROGRESS).event(OrderEvent.RESUME_ORDER)
                .action(resumeOrderAction())
                .and()
                .withExternal()
                .source(OrderStatus.ON_HOLD).target(OrderStatus.CANCELLED).event(OrderEvent.CANCEL_ORDER)
                .action(cancelOrderAction("Order cancelled while on hold"))
                .and()

                // From READY_FOR_SHIPMENT
                .withExternal()
                .source(OrderStatus.READY_FOR_SHIPMENT).target(OrderStatus.SHIPPED).event(OrderEvent.SHIP_ORDER)
                .action(shipOrderAction())
                .and()
                .withExternal()
                .source(OrderStatus.READY_FOR_SHIPMENT).target(OrderStatus.CANCELLED).event(OrderEvent.CANCEL_ORDER) // e.g. last minute cancellation
                .action(cancelOrderAction("Order cancelled before shipment"))
                .and()

                // From SHIPPED
                .withExternal()
                .source(OrderStatus.SHIPPED).target(OrderStatus.DELIVERED).event(OrderEvent.DELIVER_ORDER)
                .action(deliverOrderAction())
                .and()
                // No cancellation from SHIPPED typically, but could be a return/refund process (out of scope for simple cancel)

                // From DELIVERED
                .withExternal()
                .source(OrderStatus.DELIVERED).target(OrderStatus.COMPLETED).event(OrderEvent.COMPLETE_ORDER)
                .action(completeOrderAction());

        // No transitions from COMPLETED or CANCELLED as they are end states.
    }

    @Bean
    public StateMachineListener<OrderStatus, OrderEvent> orderStateMachineListener() {
        return new StateMachineListenerAdapter<OrderStatus, OrderEvent>() {
            @Override
            public void stateChanged(State<OrderStatus, OrderEvent> from, State<OrderStatus, OrderEvent> to) {
                log.info("Order state changed from {} to {}",
                        (from != null ? from.getId() : "null"),
                        (to != null ? to.getId() : "null"));
            }
            public void eventNotAccepted(OrderEvent event) {
                log.warn("Order event {} not accepted by the state machine in its current state.", event);
            }

            @Override
            public void stateMachineError(org.springframework.statemachine.StateMachine<OrderStatus, OrderEvent> stateMachine, Exception exception) {
                log.error("Order StateMachine error for machine ID {}: {}", stateMachine.getId(), exception.getMessage(), exception);
            }

            @Override
            public void transition(org.springframework.statemachine.transition.Transition<OrderStatus, OrderEvent> transition) {
                if (transition.getSource() != null && transition.getTarget() != null) {
                    log.debug("Order transition: Source: {}, Target: {}, Event: {}, Kind: {}",
                            transition.getSource().getId(),
                            transition.getTarget().getId(),
                            transition.getTrigger() != null ? transition.getTrigger().getEvent() : "N/A",
                            transition.getKind());
                }
            }
        };
    }

    // --- Action Beans ---
    // These actions would typically interact with services to update the order entity,
    // trigger task creation, interact with payment gateways, etc.
    // For simplicity, they are just logging actions here.

    @Bean
    public Action<OrderStatus, OrderEvent> processOrderAction() {
        return context -> log.info("ACTION: Processing order for event: {}. Order ID from extended state: {}", context.getEvent(), context.getExtendedState().get("ORDER_ID", Long.class));
    }

    @Bean
    public Action<OrderStatus, OrderEvent> paymentSuccessfulAction() {
        return context -> log.info("ACTION: Payment successful for order: {}. Triggering task creation (conceptually).", context.getExtendedState().get("ORDER_ID", Long.class));
        // Here you might interact with TaskStateMachineService to create initial tasks
    }

    @Bean
    public Action<OrderStatus, OrderEvent> paymentFailedAction() {
        return context -> log.warn("ACTION: Payment failed for order: {}.", context.getExtendedState().get("ORDER_ID", Long.class));
    }

    @Bean
    public Action<OrderStatus, OrderEvent> retryPaymentAction() {
        return context -> log.info("ACTION: Retrying payment for order: {}.", context.getExtendedState().get("ORDER_ID", Long.class));
    }

    @Bean
    public Action<OrderStatus, OrderEvent> readyForShipmentAction() {
        return context -> log.info("ACTION: Order {} is ready for shipment.", context.getExtendedState().get("ORDER_ID", Long.class));
    }

    @Bean
    public Action<OrderStatus, OrderEvent> placeOnHoldAction() {
        return context -> log.info("ACTION: Placing order {} on hold.", context.getExtendedState().get("ORDER_ID", Long.class));
    }

    @Bean
    public Action<OrderStatus, OrderEvent> resumeOrderAction() {
        return context -> log.info("ACTION: Resuming order {}.", context.getExtendedState().get("ORDER_ID", Long.class));
    }

    @Bean
    public Action<OrderStatus, OrderEvent> shipOrderAction() {
        return context -> log.info("ACTION: Shipping order: {}.", context.getExtendedState().get("ORDER_ID", Long.class));
    }

    @Bean
    public Action<OrderStatus, OrderEvent> deliverOrderAction() {
        return context -> log.info("ACTION: Delivering order: {}.", context.getExtendedState().get("ORDER_ID", Long.class));
    }

    @Bean
    public Action<OrderStatus, OrderEvent> completeOrderAction() {
        return context -> log.info("ACTION: Completing order: {}.", context.getExtendedState().get("ORDER_ID", Long.class));
    }

    // Parameterized action for cancellation
    public Action<OrderStatus, OrderEvent> cancelOrderAction(String reason) {
        return context -> {
            log.warn("ACTION: Cancelling order: {}. Reason: {}", context.getExtendedState().get("ORDER_ID", Long.class), reason);
            // Store cancellation reason in order metadata if needed
            // context.getExtendedState().getVariables().put("cancellationReason", reason);
        };
    }

    // --- Guard Beans ---
    // Guards are conditions that must be met for a transition to occur.

    @Bean
    public Guard<OrderStatus, OrderEvent> allTasksCompletedGuard() {
        return context -> {
            Long orderId = context.getExtendedState().get("ORDER_ID", Long.class);
            // In a real scenario, you'd check the TaskService or database
            // to see if all tasks for this orderId are actually COMPLETED.
            // For this example, we'll assume it's true if the event is sent.
            // You could also pass a flag via event headers or extended state.
            boolean tasksCompleted = Boolean.TRUE.equals(context.getMessageHeader("allTasksCompleted"));
            log.info("GUARD: Checking if all tasks are completed for order {}: {}", orderId, tasksCompleted);
            return tasksCompleted; // Example: true
        };
    }
}