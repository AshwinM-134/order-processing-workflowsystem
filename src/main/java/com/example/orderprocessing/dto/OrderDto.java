package com.example.orderprocessing.dto;

import com.example.orderprocessing.enums.OrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {
    private Long id;
    private OrderStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private JsonNode metadata; // To represent customer info, delivery preferences, etc.
    private List<TaskDto> tasks;

    // Constructors, Getters, and Setters

    public OrderDto() {
    }

    public OrderDto(Long id, OrderStatus status, LocalDateTime createdDate, LocalDateTime updatedDate, JsonNode metadata, List<TaskDto> tasks) {
        this.id = id;
        this.status = status;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.metadata = metadata;
        this.tasks = tasks;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
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

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public List<TaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
    }
}