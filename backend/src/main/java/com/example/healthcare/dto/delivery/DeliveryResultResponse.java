package com.example.healthcare.dto.delivery;

import java.time.LocalDateTime;

/**
 * Response DTO representing an immediate delivery result for a send or retry attempt.
 * Contains safe status, HTTP code, attempt counts, next retry schedule, and sanitized diagnostic summary.
 */
public class DeliveryResultResponse {

    private Long id;
    private Long conversionId;
    private Long destinationId;
    private String destinationName;
    private LocalDateTime attemptTimestamp;
    private String status; // "SUCCESS", "FAILED", "RETRYING", "IN_PROGRESS", "PENDING"
    private Integer httpStatusCode;
    private String errorMessage;
    private String responseSummary;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean retryable;
    private Long hospitalId;
    private String hospitalName;

    public DeliveryResultResponse() {
    }

    public DeliveryResultResponse(Long id, Long conversionId, Long destinationId, String destinationName,
                                  LocalDateTime attemptTimestamp, String status, Integer httpStatusCode,
                                  String errorMessage, Long hospitalId, String hospitalName) {
        this(id, conversionId, destinationId, destinationName, attemptTimestamp, status, httpStatusCode,
                errorMessage, null, 1, 3, null, attemptTimestamp, attemptTimestamp,
                "FAILED".equalsIgnoreCase(status), hospitalId, hospitalName);
    }

    public DeliveryResultResponse(Long id, Long conversionId, Long destinationId, String destinationName,
                                  LocalDateTime attemptTimestamp, String status, Integer httpStatusCode,
                                  String errorMessage, String responseSummary, Long hospitalId, String hospitalName) {
        this(id, conversionId, destinationId, destinationName, attemptTimestamp, status, httpStatusCode,
                errorMessage, responseSummary, 1, 3, null, attemptTimestamp, attemptTimestamp,
                "FAILED".equalsIgnoreCase(status), hospitalId, hospitalName);
    }

    public DeliveryResultResponse(Long id, Long conversionId, Long destinationId, String destinationName,
                                  LocalDateTime attemptTimestamp, String status, Integer httpStatusCode,
                                  String errorMessage, String responseSummary, Integer attemptCount,
                                  Integer maxAttempts, LocalDateTime nextRetryAt, LocalDateTime createdAt,
                                  LocalDateTime updatedAt, Boolean retryable, Long hospitalId, String hospitalName) {
        this.id = id;
        this.conversionId = conversionId;
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.attemptTimestamp = attemptTimestamp;
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.errorMessage = errorMessage;
        this.responseSummary = responseSummary;
        this.attemptCount = attemptCount != null ? attemptCount : 1;
        this.maxAttempts = maxAttempts != null ? maxAttempts : 3;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.retryable = retryable != null ? retryable : "FAILED".equalsIgnoreCase(status);
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversionId() {
        return conversionId;
    }

    public void setConversionId(Long conversionId) {
        this.conversionId = conversionId;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public LocalDateTime getAttemptTimestamp() {
        return attemptTimestamp;
    }

    public void setAttemptTimestamp(LocalDateTime attemptTimestamp) {
        this.attemptTimestamp = attemptTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public void setResponseSummary(String responseSummary) {
        this.responseSummary = responseSummary;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
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

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
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
}
