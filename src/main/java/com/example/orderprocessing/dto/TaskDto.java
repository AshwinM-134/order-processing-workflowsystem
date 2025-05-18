package com.example.orderprocessing.dto;

import com.example.orderprocessing.enums.TaskStatus;
import com.example.orderprocessing.enums.TaskType;
import java.time.LocalDateTime;

public class TaskDto {
    private Long id;
    private Long orderId;
    private TaskType taskType;
    private TaskStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime completedDate;

    // Constructors, Getters, and Setters

    public TaskDto() {
    }

    public TaskDto(Long id, Long orderId, TaskType taskType, TaskStatus status, LocalDateTime createdDate, LocalDateTime updatedDate, LocalDateTime completedDate) {
        this.id = id;
        this.orderId = orderId;
        this.taskType = taskType;
        this.status = status;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.completedDate = completedDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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
}