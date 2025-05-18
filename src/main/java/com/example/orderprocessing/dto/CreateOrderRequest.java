package com.example.orderprocessing.dto;

import com.fasterxml.jackson.databind.JsonNode;
// import jakarta.validation.constraints.NotNull; // Example for validation

public class CreateOrderRequest {

    // @NotNull // Add validation if needed
    private JsonNode metadata; // Customer info, items, delivery preferences, etc.

    // Getters and Setters
    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }
}