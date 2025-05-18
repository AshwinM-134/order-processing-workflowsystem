package com.example.orderprocessing.enums;

/**
 * Represents the different types of tasks associated with an order.
 */
public enum TaskType {
    VALIDATE_ORDER_DETAILS, // Task to validate the details of the order
    PROCESS_PAYMENT,        // Task for handling payment processing
    CHECK_INVENTORY,        // Task to check if items are in stock
    PACKAGING,              // Task for packaging the order items
    ARRANGE_SHIPPING,       // Task for arranging shipment logistics
    NOTIFY_CUSTOMER,        // Task for sending notifications to the customer
    QUALITY_CHECK,          // Task for performing quality checks before shipment
    HANDLE_RETURN,          // Task for processing a returned order (if applicable)
    GENERATE_INVOICE        // Task for generating the order invoice
    // Add other task types as needed
}