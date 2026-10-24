package com.example.healthcare.dto.hl7;

import java.time.LocalDateTime;

/**
 * Response DTO returned by the HL7 to FHIR conversion API.
 * Encapsulates conversion status, metadata, and generated FHIR R4 output
 * without exposing internal JPA entities.
 */
public class HL7ConvertResponse {

    private String status;
    private Long conversionId;
    private String messageControlId;
    private String messageType;
    private String patientId;
    private String encounterId;
    private int observationCount;
    private String fhirOutput;
    private LocalDateTime timestamp;

    public HL7ConvertResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public HL7ConvertResponse(String status, Long conversionId, String messageControlId,
                              String messageType, String patientId, String encounterId,
                              int observationCount, String fhirOutput) {
        this.status = status;
        this.conversionId = conversionId;
        this.messageControlId = messageControlId;
        this.messageType = messageType;
        this.patientId = patientId;
        this.encounterId = encounterId;
        this.observationCount = observationCount;
        this.fhirOutput = fhirOutput;
        this.timestamp = LocalDateTime.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public int getObservationCount() {
        return observationCount;
    }

    public void setObservationCount(int observationCount) {
        this.observationCount = observationCount;
    }

    public String getFhirOutput() {
        return fhirOutput;
    }

    public void setFhirOutput(String fhirOutput) {
        this.fhirOutput = fhirOutput;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
