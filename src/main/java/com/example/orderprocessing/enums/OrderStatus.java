package com.example.orderprocessing.enums;

/**
 * Represents the various statuses an Order can be in.
 * These are used as states in the Order State Machine.
 */
public enum OrderStatus {
    CREATED,             // Order has been created but not yet processed
    PAYMENT_PENDING,     // Order is awaiting payment confirmation
    PAYMENT_FAILED,      // Payment attempt failed
    IN_PROGRESS,         // Order payment successful, processing has started (e.g., tasks are being created/executed)
    ON_HOLD,             // Order processing is temporarily paused
    READY_FOR_SHIPMENT,  // All tasks completed, order is ready to be shipped
    SHIPPED,             // Order has been shipped
    DELIVERED,           // Order has been delivered to the customer
    COMPLETED,           // Order is fully completed (e.g., post-delivery checks done)
    CANCELLED            // Order has been cancelled
}