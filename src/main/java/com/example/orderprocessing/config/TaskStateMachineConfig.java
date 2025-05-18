package com.example.orderprocessing.config;

import com.example.orderprocessing.enums.TaskEvent;
import com.example.orderprocessing.enums.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

@Slf4j
@Configuration
@EnableStateMachineFactory(name = "taskStateMachineFactory") // Named factory for tasks
public class TaskStateMachineConfig extends EnumStateMachineConfigurerAdapter<TaskStatus, TaskEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<TaskStatus, TaskEvent> config) throws Exception {
        config
                .withConfiguration()
                .autoStartup(false) // Task state machines will also be started manually
                .listener(taskStateMachineListener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<TaskStatus, TaskEvent> states) throws Exception {
        states
                .withStates()
                .initial(TaskStatus.PENDING)
                .states(EnumSet.allOf(TaskStatus.class))
                .end(TaskStatus.COMPLETED)
                .end(TaskStatus.CANCELLED)
                .end(TaskStatus.FAILED); // FAILED can be an end state, or allow retry
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TaskStatus, TaskEvent> transitions) throws Exception {
        transitions
                // From PENDING
                .withExternal()
                .source(TaskStatus.PENDING).target(TaskStatus.IN_PROGRESS).event(TaskEvent.START_TASK)
                .action(startTaskAction())
                .and()
                .withExternal()
                .source(TaskStatus.PENDING).target(TaskStatus.CANCELLED).event(TaskEvent.CANCEL_TASK)
                .action(cancelTaskAction("Task cancelled before starting"))
                .and()

                // From IN_PROGRESS
                .withExternal()
                .source(TaskStatus.IN_PROGRESS).target(TaskStatus.COMPLETED).event(TaskEvent.COMPLETE_TASK)
                .action(completeTaskAction())
                .and()
                .withExternal()
                .source(TaskStatus.IN_PROGRESS).target(TaskStatus.FAILED).event(TaskEvent.FAIL_TASK)
                .action(failTaskAction())
                .and()
                .withExternal()
                .source(TaskStatus.IN_PROGRESS).target(TaskStatus.CANCELLED).event(TaskEvent.CANCEL_TASK)
                .action(cancelTaskAction("Task cancelled while in progress"))
                .and()

                // From FAILED (example: allow retry to IN_PROGRESS)
                .withExternal()
                .source(TaskStatus.FAILED).target(TaskStatus.IN_PROGRESS).event(TaskEvent.RETRY_TASK)
                .action(retryTaskAction())
                .and()
                .withExternal()
                .source(TaskStatus.FAILED).target(TaskStatus.CANCELLED).event(TaskEvent.CANCEL_TASK) // If no retry, can be cancelled
                .action(cancelTaskAction("Task cancelled after failure"));

        // No transitions from COMPLETED or CANCELLED as they are end states.
    }

    @Bean
    public StateMachineListener<TaskStatus, TaskEvent> taskStateMachineListener() {
        return new StateMachineListenerAdapter<TaskStatus, TaskEvent>() {
            @Override
            public void stateChanged(State<TaskStatus, TaskEvent> from, State<TaskStatus, TaskEvent> to) {
                log.info("Task state changed from {} to {}",
                        (from != null ? from.getId() : "null"),
                        (to != null ? to.getId() : "null"));
            }

            public void eventNotAccepted(TaskEvent event) {
                log.warn("Task event {} not accepted by the state machine in its current state.", event);
            }


            @Override
            public void stateMachineError(org.springframework.statemachine.StateMachine<TaskStatus, TaskEvent> stateMachine, Exception exception) {
                log.error("Task StateMachine error for machine ID {}: {}", stateMachine.getId(), exception.getMessage(), exception);
            }
        };
    }

    // --- Action Beans for Task State Machine ---
    // These actions would update the task entity and potentially interact with other services.

    @Bean
    public Action<TaskStatus, TaskEvent> startTaskAction() {
        return context -> {
            Long taskId = context.getExtendedState().get("TASK_ID", Long.class);
            log.info("TASK_ACTION: Starting task: {}", taskId);
            // Update task status in DB, set start time etc.
        };
    }

    @Bean
    public Action<TaskStatus, TaskEvent> completeTaskAction() {
        return context -> {
            Long taskId = context.getExtendedState().get("TASK_ID", Long.class);
            log.info("TASK_ACTION: Completing task: {}", taskId);
            // Update task status in DB, set completion time etc.
            // Potentially trigger an event to the Order State Machine if this was the last task
            // (though this coordination is better handled by a service layer).
        };
    }

    @Bean
    public Action<TaskStatus, TaskEvent> failTaskAction() {
        return context -> {
            Long taskId = context.getExtendedState().get("TASK_ID", Long.class);
            log.warn("TASK_ACTION: Task failed: {}", taskId);
            // Update task status in DB, log error details
        };
    }

    @Bean
    public Action<TaskStatus, TaskEvent> retryTaskAction() {
        return context -> {
            Long taskId = context.getExtendedState().get("TASK_ID", Long.class);
            log.info("TASK_ACTION: Retrying task: {}", taskId);
            // Reset relevant fields in task entity for retry
        };
    }

    public Action<TaskStatus, TaskEvent> cancelTaskAction(String reason) {
        return context -> {
            Long taskId = context.getExtendedState().get("TASK_ID", Long.class);
            log.warn("TASK_ACTION: Cancelling task: {}. Reason: {}", taskId, reason);
            // Update task status in DB to CANCELLED
        };
    }
}