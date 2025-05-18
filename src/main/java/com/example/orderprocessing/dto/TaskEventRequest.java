package com.example.orderprocessing.dto;

import com.example.orderprocessing.enums.TaskEvent;

public class TaskEventRequest {
    private TaskEvent event;
    private String reason; // Optional

    public TaskEvent getEvent() {
        return event;
    }

    public void setEvent(TaskEvent event) {
        this.event = event;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}