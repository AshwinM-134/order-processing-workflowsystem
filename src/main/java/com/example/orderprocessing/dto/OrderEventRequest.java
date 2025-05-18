package com.example.orderprocessing.dto;

import com.example.orderprocessing.enums.OrderEvent;
// import jakarta.validation.constraints.NotNull; // Example for validation

public class OrderEventRequest {
    // @NotNull
    private OrderEvent event;
    // You can add more fields here if an event needs to carry specific data
    // For example, a cancellation reason for CANCEL_ORDER event
    private String reason; // Optional: for CANCEL_ORDER

    public OrderEvent getEvent() {
        return event;
    }

    public void setEvent(OrderEvent event) {
        this.event = event;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}