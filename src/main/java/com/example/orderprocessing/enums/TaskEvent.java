package com.example.orderprocessing.enums;

/**
 * Represents the events that can trigger transitions in the Task State Machine.
 */
public enum TaskEvent {
    START_TASK,          // Event to begin processing a pending task
    COMPLETE_TASK,       // Event to mark a task as successfully completed
    FAIL_TASK,           // Event to mark a task as failed
    RETRY_TASK,          // Event to retry a failed task
    CANCEL_TASK          // Event to cancel a task
}