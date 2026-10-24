package com.example.healthcare.dto.destination;

import com.example.healthcare.entity.DestinationAuthType;
import java.time.LocalDateTime;

/**
 * Response DTO representing destination configuration details.
 * Strictly avoids leaking bearer tokens or credentials in responses or presentation layers.
 */
public class DestinationResponse {

    private Long id;
    private String name;
    private String baseUrl;
    private String fhirVersion;
    private DestinationAuthType authType;
    private boolean hasAuthToken; // Boolean indicator only; token itself is never exposed
    private boolean enabled;
    private boolean mock;
    private Long hospitalId;
    private String hospitalName;
    private boolean platformWide;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DestinationResponse() {
    }

    public DestinationResponse(Long id, String name, String baseUrl, String fhirVersion,
                               DestinationAuthType authType, boolean hasAuthToken, boolean enabled,
                               boolean mock, Long hospitalId, String hospitalName, boolean platformWide,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.fhirVersion = fhirVersion;
        this.authType = authType;
        this.hasAuthToken = hasAuthToken;
        this.enabled = enabled;
        this.mock = mock;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.platformWide = platformWide;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isHasAuthToken() {
        return hasAuthToken;
    }

    public void setHasAuthToken(boolean hasAuthToken) {
        this.hasAuthToken = hasAuthToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public boolean isPlatformWide() {
        return platformWide;
    }

    public void setPlatformWide(boolean platformWide) {
        this.platformWide = platformWide;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
