package com.example.orderprocessing.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class UpdateOrderRequest {

    private JsonNode metadata; // Allow updating metadata

    // Getters and Setters
    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }
}