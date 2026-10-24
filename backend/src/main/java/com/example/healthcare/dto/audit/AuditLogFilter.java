package com.example.healthcare.dto.audit;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Filter criteria DTO for querying audit logs.
 */
public class AuditLogFilter {

    private String action;
    private String outcome;
    private String resourceType;
    private String resourceId;
    private String search;
    private Long hospitalId; // Only applicable to platform admins

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    public AuditLogFilter() {
    }

    public AuditLogFilter(String action, String outcome, String resourceType, String resourceId,
                          String search, Long hospitalId, LocalDateTime startDate, LocalDateTime endDate) {
        this.action = action;
        this.outcome = outcome;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.search = search;
        this.hospitalId = hospitalId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
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

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
