package com.example.healthcare.dto.mockfhir;

import java.time.LocalDateTime;

/**
 * DTO representing an inspected FHIR resource or Bundle received by the local mock FHIR destination.
 */
public class MockReceivedResource {

    private String id;
    private String resourceType;
    private String resourceId;
    private Integer entryCount;
    private String summary;
    private LocalDateTime receivedAt;
    private String rawPayload;
    private boolean hasAuthHeader;

    public MockReceivedResource() {
    }

    public MockReceivedResource(String id, String resourceType, String resourceId, Integer entryCount,
                                String summary, LocalDateTime receivedAt, String rawPayload, boolean hasAuthHeader) {
        this.id = id;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.entryCount = entryCount;
        this.summary = summary;
        this.receivedAt = receivedAt;
        this.rawPayload = rawPayload;
        this.hasAuthHeader = hasAuthHeader;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(Integer entryCount) {
        this.entryCount = entryCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public boolean isHasAuthHeader() {
        return hasAuthHeader;
    }

    public void setHasAuthHeader(boolean hasAuthHeader) {
        this.hasAuthHeader = hasAuthHeader;
    }
}
