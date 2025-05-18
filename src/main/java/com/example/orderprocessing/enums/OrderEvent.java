package com.example.orderprocessing.enums;

/**
 * Represents the events that can trigger transitions in the Order State Machine.
 */
public enum OrderEvent {
    PROCESS_ORDER,         // Event to start processing a newly created order
    PAYMENT_SUCCESSFUL,    // Event indicating payment was successful
    PAYMENT_FAILED,        // Event indicating payment failed
    RETRY_PAYMENT,         // Event to retry a failed payment
    ALL_TASKS_COMPLETED,   // Event indicating all associated tasks for the order are done
    PLACE_ON_HOLD,         // Event to put an order on hold
    RESUME_ORDER,          // Event to resume a held order
    SHIP_ORDER,            // Event to mark the order as shipped
    DELIVER_ORDER,         // Event to mark the order as delivered
    COMPLETE_ORDER,        // Event to finalize and complete the order
    CANCEL_ORDER           // Event to cancel the order at various stages
}