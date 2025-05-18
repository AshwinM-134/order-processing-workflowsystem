package com.example.orderprocessing.service;

import com.example.orderprocessing.enums.TaskEvent;
import com.example.orderprocessing.enums.TaskStatus;
import com.example.orderprocessing.model.Task;
import com.example.orderprocessing.repository.TaskRepository;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStateMachineService {

    @Qualifier("taskStateMachineFactory")
    private final StateMachineFactory<TaskStatus, TaskEvent> taskStateMachineFactory;
    private final TaskRepository taskRepository; // To persist state changes

    public StateMachine<TaskStatus, TaskEvent> initializeStateMachine(Task task) {
        String machineId = "taskSM-" + task.getId();
        StateMachine<TaskStatus, TaskEvent> stateMachine = taskStateMachineFactory.getStateMachine(machineId);

        if (stateMachine.getState() != null && !stateMachine.isComplete()) {
            stateMachine.stop();
        }

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(new StateMachineInterceptorAdapter<>() {
                        @Override
                        public void postStateChange(State<TaskStatus, TaskEvent> state, Message<TaskEvent> message,
                                                    Transition<TaskStatus, TaskEvent> transition,
                                                    StateMachine<TaskStatus, TaskEvent> stateMachine,
                                                    StateMachine<TaskStatus, TaskEvent> rootStateMachine) {
                            log.debug("Interceptor: Task {} transitioned to state {}", task.getId(), state.getId());
                            if (task.getStatus() != state.getId()) {
                                task.setStatus(state.getId());
                                if (state.getId() == TaskStatus.COMPLETED && task.getCompletedDate() == null) {
                                    task.setCompletedDate(LocalDateTime.now());
                                }
                                taskRepository.save(task); // Persist the new status and completion date
                                log.info("Task {} status updated to {} in database.", task.getId(), state.getId());
                            }
                        }

                        @Override
                        public Exception stateMachineError(StateMachine<TaskStatus, TaskEvent> machine, Exception exception) {
                            log.error("Error in Task StateMachine (ID: {}) for Task {}: {}", machine.getId(), task.getId(), exception.getMessage(), exception);
                            return exception; // Propagate
                        }
                    });
                    sma.resetStateMachine(new DefaultStateMachineContext<>(task.getStatus(), null, null, null, null, machineId));
                });

        stateMachine.start();
        log.info("Task StateMachine (ID: {}) initialized and started for Task ID: {} in state: {}", machineId, task.getId(), stateMachine.getState().getId());
        return stateMachine;
    }


    @Transactional
    public boolean sendEvent(Task task, TaskEvent event, String reason) {
        StateMachine<TaskStatus, TaskEvent> stateMachine = initializeStateMachine(task);

        Map<String, Object> headers = new HashMap<>();
        headers.put("TASK_ID", task.getId());
        if (reason != null) {
            headers.put("REASON", reason);
        }

        Message<TaskEvent> message = MessageBuilder
                .withPayload(event)
                .copyHeaders(headers)
                .build();

        log.info("Sending event {} to Task StateMachine (ID: {}) for Task ID: {} with current state: {}",
                event, stateMachine.getId(), task.getId(), stateMachine.getState().getId());

        boolean eventAccepted = stateMachine.sendEvent(message);

        if (eventAccepted) {
            log.info("Event {} accepted by Task StateMachine (ID: {}) for Task ID: {}. New state: {}",
                    event, stateMachine.getId(), task.getId(), stateMachine.getState().getId());
        } else {
            log.warn("Event {} NOT accepted by Task StateMachine (ID: {}) for Task ID: {} in state: {}",
                    event, stateMachine.getId(), task.getId(), stateMachine.getState().getId());
        }

        stateMachine.stop();
        log.debug("Task StateMachine (ID: {}) stopped for Task ID: {}", stateMachine.getId(), task.getId());
        return eventAccepted;
    }
}