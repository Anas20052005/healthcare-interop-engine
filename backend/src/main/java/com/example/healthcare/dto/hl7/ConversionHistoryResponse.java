package com.example.healthcare.dto.hl7;

import java.time.LocalDateTime;

/**
 * Response DTO representing an audit record of an HL7-to-FHIR conversion.
 * Protects internal entity representations and avoids exposing sensitive PHI or credentials.
 */
public class ConversionHistoryResponse {

    private Long conversionId;
    private String messageControlId;
    private String messageType;
    private String patientId;
    private String encounterId;
    private Integer observationCount;
    private String status;
    private String errorMessage;
    private LocalDateTime timestamp;

    private Long hospitalId;
    private String hospitalName;
    private String fhirOutput;

    public ConversionHistoryResponse() {
    }

    public ConversionHistoryResponse(Long conversionId, String messageControlId, String messageType,
                                     String patientId, String encounterId, Integer observationCount,
                                     String status, String errorMessage, LocalDateTime timestamp) {
        this(conversionId, messageControlId, messageType, patientId, encounterId, observationCount, status, errorMessage, timestamp, null, null);
    }

    public ConversionHistoryResponse(Long conversionId, String messageControlId, String messageType,
                                     String patientId, String encounterId, Integer observationCount,
                                     String status, String errorMessage, LocalDateTime timestamp,
                                     Long hospitalId, String hospitalName) {
        this.conversionId = conversionId;
        this.messageControlId = messageControlId;
        this.messageType = messageType;
        this.patientId = patientId;
        this.encounterId = encounterId;
        this.observationCount = observationCount;
        this.status = status;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
    }

    public Long getConversionId() {
        return conversionId;
    }

    public void setConversionId(Long conversionId) {
        this.conversionId = conversionId;
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

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public Integer getObservationCount() {
        return observationCount;
    }

    public void setObservationCount(Integer observationCount) {
        this.observationCount = observationCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public String getFhirOutput() {
        return fhirOutput;
    }

    public void setFhirOutput(String fhirOutput) {
        this.fhirOutput = fhirOutput;
    }
}
