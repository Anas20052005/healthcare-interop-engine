package com.example.healthcare.dto.destination;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating the enabled/disabled status of a destination.
 */
public class DestinationStatusRequest {

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;

    public DestinationStatusRequest() {
    }

    public DestinationStatusRequest(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
