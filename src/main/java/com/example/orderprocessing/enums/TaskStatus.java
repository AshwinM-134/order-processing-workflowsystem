package com.example.orderprocessing.enums;

/**
 * Represents the various statuses a Task can be in.
 * These are used as states in the Task State Machine.
 */
public enum TaskStatus {
    PENDING,             // Task is created but not yet started
    IN_PROGRESS,         // Task is actively being worked on
    COMPLETED,           // Task has been successfully completed
    FAILED,              // Task execution failed
    CANCELLED            // Task has been cancelled
}