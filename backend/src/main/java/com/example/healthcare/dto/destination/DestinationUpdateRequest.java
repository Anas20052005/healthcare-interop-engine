package com.example.healthcare.dto.destination;

import com.example.healthcare.entity.DestinationAuthType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing FHIR destination configuration.
 * If bearerToken is null or empty, the existing stored token is preserved.
 */
public class DestinationUpdateRequest {

    @NotBlank(message = "Destination name is required")
    @Size(max = 150, message = "Destination name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Base URL is required")
    @Size(max = 500, message = "Base URL cannot exceed 500 characters")
    private String baseUrl;

    private String fhirVersion = "R4";

    @NotNull(message = "Authentication type is required")
    private DestinationAuthType authType = DestinationAuthType.NONE;

    private String bearerToken; // If null/blank, existing token is retained without modification

    private Boolean enabled = true;

    public DestinationUpdateRequest() {
    }

    public DestinationUpdateRequest(String name, String baseUrl, String fhirVersion,
                                  DestinationAuthType authType, String bearerToken, Boolean enabled) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.fhirVersion = fhirVersion != null ? fhirVersion : "R4";
        this.authType = authType != null ? authType : DestinationAuthType.NONE;
        this.bearerToken = bearerToken;
        this.enabled = enabled != null ? enabled : true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getFhirVersion() {
        return fhirVersion;
    }

    public void setFhirVersion(String fhirVersion) {
        this.fhirVersion = fhirVersion;
    }

    public DestinationAuthType getAuthType() {
        return authType;
    }

    public void setAuthType(DestinationAuthType authType) {
        this.authType = authType;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
