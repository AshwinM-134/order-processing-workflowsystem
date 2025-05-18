package com.example.orderprocessing.controller;

import com.example.orderprocessing.dto.TaskDto;
import com.example.orderprocessing.dto.TaskEventRequest; // Assuming you create this similar to OrderEventRequest
import com.example.orderprocessing.model.Task;
import com.example.orderprocessing.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks") // Global task operations, could also be nested under orders
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // DTO Mapper (could be shared or in a dedicated mapper)
    private TaskDto convertToDto(Task task) {
        if (task == null) return null;
        return new TaskDto(
                task.getId(),
                task.getOrder() != null ? task.getOrder().getId() : null,
                task.getTaskType(),
                task.getStatus(),
                task.getCreatedDate(),
                task.getUpdatedDate(),
                task.getCompletedDate()
        );
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long taskId) {
        return taskService.getTaskById(taskId)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{taskId}/event")
    public ResponseEntity<?> sendTaskEvent(@PathVariable Long taskId, @RequestBody TaskEventRequest eventRequest) {
        try {
            // TODO: Add authorization/validation
            boolean success = taskService.sendTaskEvent(taskId, eventRequest.getEvent(), eventRequest.getReason());
            if (success) {
                Task updatedTask = taskService.getTaskById(taskId)
                        .orElseThrow(() -> new RuntimeException("Task not found after event processing: " + taskId));
                return ResponseEntity.ok(convertToDto(updatedTask));
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Event " + eventRequest.getEvent() + " not accepted for task " + taskId + " in its current state.");
            }
        } catch (RuntimeException e) { // Catch exceptions like TaskNotFound
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event: " + e.getMessage());
        }
    }
}