package com.example.healthcare.dto.delivery;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for explicitly dispatching a conversion's FHIR output to a destination.
 */
public class SendConversionRequest {

    @NotNull(message = "Destination ID is required")
    private Long destinationId;

    public SendConversionRequest() {
    }

    public SendConversionRequest(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }
}
