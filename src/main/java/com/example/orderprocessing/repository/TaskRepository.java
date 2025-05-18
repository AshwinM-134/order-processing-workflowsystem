package com.example.orderprocessing.repository;

import com.example.orderprocessing.model.Task;
import com.example.orderprocessing.enums.TaskStatus;
import com.example.orderprocessing.enums.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Custom query methods for tasks
    List<Task> findByOrderId(Long orderId);
    List<Task> findByOrderIdAndStatus(Long orderId, TaskStatus status);
    List<Task> findByOrderIdAndTaskType(Long orderId, TaskType taskType);
    long countByOrderIdAndStatusIsNot(Long orderId, TaskStatus status);
}