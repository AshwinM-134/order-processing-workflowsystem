package com.example.orderprocessing.model;

import com.example.orderprocessing.enums.TaskStatus;
import com.example.orderprocessing.enums.TaskType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tasks")
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(name = "task_type", nullable = false, length = 50)
  private TaskType taskType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TaskStatus status;

  @CreationTimestamp
  @Column(name = "created_date", nullable = false, updatable = false)
  private LocalDateTime createdDate;

  @UpdateTimestamp
  @Column(name = "updated_date", nullable = false)
  private LocalDateTime updatedDate;

  @Column(name = "completed_date")
  private LocalDateTime completedDate;

  // Constructors
  public Task() {
    this.status = TaskStatus.PENDING; // Default status
  }

  public Task(Order order, TaskType taskType) {
    this();
    this.order = order;
    this.taskType = taskType;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public LocalDateTime getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(LocalDateTime updatedDate) {
    this.updatedDate = updatedDate;
  }

  public LocalDateTime getCompletedDate() {
    return completedDate;
  }

  public void setCompletedDate(LocalDateTime completedDate) {
    this.completedDate = completedDate;
  }

  // equals, hashCode, toString
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Task task = (Task) o;
    return Objects.equals(id, task.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return (
      "Task{" +
      "id=" +
      id +
      ", orderId=" +
      (order != null ? order.getId() : "null") +
      ", taskType=" +
      taskType +
      ", status=" +
      status +
      ", createdDate=" +
      createdDate +
      ", updatedDate=" +
      updatedDate +
      ", completedDate=" +
      completedDate +
      '}'
    );
  }
}
