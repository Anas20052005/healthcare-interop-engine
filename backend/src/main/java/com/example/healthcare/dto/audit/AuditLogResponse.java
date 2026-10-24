package com.example.healthcare.dto.audit;

import java.time.LocalDateTime;

/**
 * Response DTO representing an audit trail entry.
 * Sanitized of any credentials, sensitive tokens, or private patient payloads.
 */
public class AuditLogResponse {

    private Long id;
    private Long actorId;
    private String username;
    private Long hospitalId;
    private String hospitalName;
    private String action;
    private String resourceType;
    private String resourceId;
    private String outcome;
    private LocalDateTime timestamp;
    private String details;
    private String correlationId;

    public AuditLogResponse() {
    }

    public AuditLogResponse(Long id, Long actorId, String username, Long hospitalId, String hospitalName,
                            String action, String resourceType, String resourceId, String outcome,
                            LocalDateTime timestamp, String details, String correlationId) {
        this.id = id;
        this.actorId = actorId;
        this.username = username;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.timestamp = timestamp;
        this.details = details;
        this.correlationId = correlationId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
