package com.example.orderprocessing.model;

import com.example.orderprocessing.enums.OrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "orders") // "order" is a reserved keyword in SQL, so "orders" is safer
@DynamicUpdate
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private OrderStatus status;

  @CreationTimestamp
  @Column(name = "created_date", nullable = false, updatable = false)
  private LocalDateTime createdDate;

  @UpdateTimestamp
  @Column(name = "updated_date", nullable = false)
  private LocalDateTime updatedDate;

  // For PostgreSQL JSONB
  @Column(name = "metadata", columnDefinition = "jsonb")
  @Type(JsonType.class)
  private JsonNode metadata;

  @OneToMany(
    mappedBy = "order",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY
  )
  private List<Task> tasks = new ArrayList<>();

  // Constructors
  public Order() {
    this.status = OrderStatus.CREATED; // Default status
  }

  public Order(JsonNode metadata) {
    this();
    this.metadata = metadata;
  }

  // Getters and Setters
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

  public List<Task> getTasks() {
    return tasks;
  }

  public void setTasks(List<Task> tasks) {
    this.tasks = tasks;
  }

  public void addTask(Task task) {
    tasks.add(task);
    task.setOrder(this);
  }

  public void removeTask(Task task) {
    tasks.remove(task);
    task.setOrder(null);
  }

  // equals, hashCode, toString
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Order order = (Order) o;
    return Objects.equals(id, order.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return (
      "Order{" +
      "id=" +
      id +
      ", status=" +
      status +
      ", createdDate=" +
      createdDate +
      ", updatedDate=" +
      updatedDate +
      ", metadata=" +
      (metadata != null ? metadata.toString() : "null") +
      '}'
    );
  }
}
