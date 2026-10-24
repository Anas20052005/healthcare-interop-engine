package com.example.healthcare.dto.clinical;

import java.time.LocalDateTime;

public class ObservationResponse {

    private Long id;
    private String observationCode;
    private String valueType;
    private String valueString;
    private Double valueQuantity;
    private String unit;
    private String referenceRange;
    private String status;
    private Long patientId;
    private Long encounterId;
    private LocalDateTime createdAt;
    private Long hospitalId;
    private String hospitalName;

    public ObservationResponse() {
    }

    public ObservationResponse(Long id, String observationCode, String valueType, String valueString,
                               Double valueQuantity, String unit, String referenceRange, String status,
                               Long patientId, Long encounterId, LocalDateTime createdAt,
                               Long hospitalId, String hospitalName) {
        this.id = id;
        this.observationCode = observationCode;
        this.valueType = valueType;
        this.valueString = valueString;
        this.valueQuantity = valueQuantity;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.status = status;
        this.patientId = patientId;
        this.encounterId = encounterId;
        this.createdAt = createdAt;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getObservationCode() {
        return observationCode;
    }

    public void setObservationCode(String observationCode) {
        this.observationCode = observationCode;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
    }

    public Double getValueQuantity() {
        return valueQuantity;
    }

    public void setValueQuantity(Double valueQuantity) {
        this.valueQuantity = valueQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(Long encounterId) {
        this.encounterId = encounterId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
