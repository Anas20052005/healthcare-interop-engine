package com.example.healthcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Persistence entity for conversion history and audit tracking.
 */
@Entity
@Table(name = "conversions", indexes = {
        @Index(name = "idx_conv_hosp_created", columnList = "hospital_id, created_at"),
        @Index(name = "idx_conv_hosp_status", columnList = "hospital_id, status"),
        @Index(name = "idx_conv_msg_ctrl", columnList = "message_control_id")
})
public class ConversionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_control_id", length = 100)
    private String messageControlId;

    @Column(name = "message_type", length = 50)
    private String messageType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "patient_identifier", length = 100)
    private String patientIdentifier;

    @Column(name = "encounter_identifier", length = 100)
    private String encounterIdentifier;

    @Column(name = "observation_count")
    private Integer observationCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(name = "fhir_payload", columnDefinition = "LONGTEXT")
    private String fhirPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ConversionEntity() {
    }

    public ConversionEntity(String messageControlId, String messageType, String status, String patientIdentifier, String errorMessage) {
        this(messageControlId, messageType, status, patientIdentifier, errorMessage, null);
    }

    public ConversionEntity(String messageControlId, String messageType, String status, String patientIdentifier, String errorMessage, Hospital hospital) {
        this.messageControlId = messageControlId;
        this.messageType = messageType;
        this.status = status;
        this.patientIdentifier = patientIdentifier;
        this.errorMessage = errorMessage;
        this.hospital = hospital;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageControlId() {
        return messageControlId;
    }

    public void setMessageControlId(String messageControlId) {
        this.messageControlId = messageControlId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPatientIdentifier() {
        return patientIdentifier;
    }

    public void setPatientIdentifier(String patientIdentifier) {
        this.patientIdentifier = patientIdentifier;
    }

    public String getEncounterIdentifier() {
        return encounterIdentifier;
    }

    public void setEncounterIdentifier(String encounterIdentifier) {
        this.encounterIdentifier = encounterIdentifier;
    }

    public Integer getObservationCount() {
        return observationCount;
    }

    public void setObservationCount(Integer observationCount) {
        this.observationCount = observationCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFhirPayload() {
        return fhirPayload;
    }

    public void setFhirPayload(String fhirPayload) {
        this.fhirPayload = fhirPayload;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }
}
