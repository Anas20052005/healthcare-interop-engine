package com.example.healthcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entity storing delivery results and attempt lifecycle for FHIR send operations.
 * Complies with Priority 5 and Priority 9 bounded retry requirements without leaking sensitive patient data or secrets.
 */
@Entity
@Table(name = "delivery_results", indexes = {
        @Index(name = "idx_deliv_status_retry", columnList = "status, next_retry_at"),
        @Index(name = "idx_deliv_hospital_status", columnList = "hospital_id, status"),
        @Index(name = "idx_deliv_hospital_attempt", columnList = "hospital_id, attempt_timestamp"),
        @Index(name = "idx_deliv_conversion_id", columnList = "conversion_id")
})
public class DeliveryResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversion_id", nullable = false)
    private Long conversionId;

    @Column(name = "destination_id", nullable = false)
    private Long destinationId;

    @Column(name = "destination_name", length = 150)
    private String destinationName;

    @Column(name = "attempt_timestamp", nullable = false)
    private LocalDateTime attemptTimestamp;

    @Column(nullable = false, length = 30)
    private String status; // "SUCCESS", "FAILED", "RETRYING", "IN_PROGRESS", "PENDING"

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "response_summary", length = 500)
    private String responseSummary;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 1;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DeliveryResultEntity() {
    }

    public DeliveryResultEntity(Long conversionId, Long destinationId, String destinationName,
                                LocalDateTime attemptTimestamp, String status, Integer httpStatusCode,
                                String errorMessage, Hospital hospital) {
        this(conversionId, destinationId, destinationName, attemptTimestamp, status, httpStatusCode, errorMessage, null, 1, 3, null, hospital);
    }

    public DeliveryResultEntity(Long conversionId, Long destinationId, String destinationName,
                                LocalDateTime attemptTimestamp, String status, Integer httpStatusCode,
                                String errorMessage, String responseSummary, Hospital hospital) {
        this(conversionId, destinationId, destinationName, attemptTimestamp, status, httpStatusCode, errorMessage, responseSummary, 1, 3, null, hospital);
    }

    public DeliveryResultEntity(Long conversionId, Long destinationId, String destinationName,
                                LocalDateTime attemptTimestamp, String status, Integer httpStatusCode,
                                String errorMessage, String responseSummary, Integer attemptCount,
                                Integer maxAttempts, LocalDateTime nextRetryAt, Hospital hospital) {
        this.conversionId = conversionId;
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.attemptTimestamp = attemptTimestamp != null ? attemptTimestamp : LocalDateTime.now();
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.errorMessage = errorMessage;
        this.responseSummary = responseSummary;
        this.attemptCount = attemptCount != null ? attemptCount : 1;
        this.maxAttempts = maxAttempts != null ? maxAttempts : 3;
        this.nextRetryAt = nextRetryAt;
        this.hospital = hospital;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.attemptTimestamp == null) {
            this.attemptTimestamp = LocalDateTime.now();
        }
        if (this.attemptCount == null) {
            this.attemptCount = 1;
        }
        if (this.maxAttempts == null) {
            this.maxAttempts = 3;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isManualRetryEligible() {
        return "FAILED".equalsIgnoreCase(this.status);
    }

    public boolean isRetrying() {
        return "RETRYING".equalsIgnoreCase(this.status);
    }

    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(this.status);
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

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
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
