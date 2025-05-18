package com.example.orderprocessing.service;

import com.example.orderprocessing.enums.TaskEvent;
import com.example.orderprocessing.enums.TaskStatus;
import com.example.orderprocessing.enums.TaskType;
import com.example.orderprocessing.model.Order;
import com.example.orderprocessing.model.Task;
import com.example.orderprocessing.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

  private final TaskRepository taskRepository;
  private final TaskStateMachineService taskStateMachineService;
  private final ApplicationEventPublisher eventPublisher; // For decoupling, to notify OrderService

  @Transactional
  public Task createTask(Order order, TaskType taskType) {
    Task task = new Task(order, taskType);
    // Initial status is PENDING by default
    Task savedTask = taskRepository.save(task);
    log.info(
      "Task {} created with ID: {} for order ID: {}",
      taskType,
      savedTask.getId(),
      order.getId()
    );

    // Initialize and start state machine for this new task
    taskStateMachineService.initializeStateMachine(savedTask);
    // Consider sending START_TASK event immediately if appropriate or let it be explicit
    // For now, let's make it explicit via an endpoint or another service call.
    return savedTask;
  }

  @Transactional(readOnly = true)
  public Optional<Task> getTaskById(Long taskId) {
    return taskRepository.findById(taskId);
  }

  @Transactional(readOnly = true)
  public List<Task> getTasksForOrder(Long orderId) {
    return taskRepository.findByOrderId(orderId);
  }

  @Transactional
  public boolean sendTaskEvent(Long taskId, TaskEvent event, String reason) {
    Task task = taskRepository
      .findById(taskId)
      .orElseThrow(() -> new RuntimeException("Task not found: " + taskId)); // Specific exception
    log.info("Attempting to send event {} to task {}", event, taskId);
    boolean eventAccepted = taskStateMachineService.sendEvent(
      task,
      event,
      reason
    );

    // If the task was completed by this event, notify potentially interested parties (like OrderService)
    // We re-fetch the task to ensure its status is the latest after the state machine has run.
    if (eventAccepted && TaskEvent.COMPLETE_TASK.equals(event)) {
      Task updatedTask = taskRepository.findById(taskId).orElse(task); // Re-fetch
      if (TaskStatus.COMPLETED.equals(updatedTask.getStatus())) {
        log.info(
          "Task {} for order {} has been completed. Publishing completion event.",
          taskId,
          updatedTask.getOrder().getId()
        );
        updatedTask.setCompletedDate(LocalDateTime.now());
        taskRepository.save(updatedTask); // Save completion date
        // Notify OrderService (or any other listener) that a task was completed.
        // This is a simple way to decouple. OrderService will listen for this.
        eventPublisher.publishEvent(
          new TaskCompletedApplicationEvent(
            this,
            updatedTask.getOrder().getId(),
            updatedTask.getId()
          )
        );
      }
    }
    return eventAccepted;
  }

  @Transactional(readOnly = true)
  public boolean areAllTasksCompletedForOrder(Long orderId) {
    // A task is considered "not completed" if its status is not COMPLETED.
    // This means PENDING, IN_PROGRESS, FAILED, CANCELLED tasks are considered incomplete.
    long incompleteTasks = taskRepository.countByOrderIdAndStatusIsNot(
      orderId,
      TaskStatus.COMPLETED
    );
    log.debug(
      "Order {} has {} tasks that are not in COMPLETED state.",
      orderId,
      incompleteTasks
    );
    return incompleteTasks == 0;
  }

  // Define an application event for when a task is completed
  public static class TaskCompletedApplicationEvent
    extends org.springframework.context.ApplicationEvent {

    private final Long orderId;
    private final Long taskId;

    public TaskCompletedApplicationEvent(
      Object source,
      Long orderId,
      Long taskId
    ) {
      super(source);
      this.orderId = orderId;
      this.taskId = taskId;
    }

    public Long getOrderId() {
      return orderId;
    }

    public Long getTaskId() {
      return taskId;
    }
  }
}
