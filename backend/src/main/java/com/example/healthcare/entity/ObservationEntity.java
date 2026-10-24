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
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Persistence entity for observation/result data extracted from HL7 OBX segment.
 * Kept strictly separate from HAPI FHIR Observation model.
 */
@Entity
@Table(name = "observations", indexes = {
        @Index(name = "idx_obs_patient_hosp", columnList = "patient_id, hospital_id"),
        @Index(name = "idx_obs_encounter_hosp", columnList = "encounter_id, hospital_id"),
        @Index(name = "idx_obs_hosp_created", columnList = "hospital_id, created_at")
})
public class ObservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private EncounterEntity encounter;

    @Column(name = "observation_code", length = 100)
    private String observationCode;

    @Column(name = "value_type", length = 20)
    private String valueType;

    @Column(name = "value_string", length = 500)
    private String valueString;

    @Column(name = "value_quantity")
    private Double valueQuantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(name = "status", length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ObservationEntity() {
    }

    public ObservationEntity(PatientEntity patient, EncounterEntity encounter, String observationCode, String valueType, String valueString, Double valueQuantity, String unit, String referenceRange, String status) {
        this(patient, encounter, observationCode, valueType, valueString, valueQuantity, unit, referenceRange, status, null);
    }

    public ObservationEntity(PatientEntity patient, EncounterEntity encounter, String observationCode, String valueType, String valueString, Double valueQuantity, String unit, String referenceRange, String status, Hospital hospital) {
        this.patient = patient;
        this.encounter = encounter;
        this.observationCode = observationCode;
        this.valueType = valueType;
        this.valueString = valueString;
        this.valueQuantity = valueQuantity;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.status = status;
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

    public PatientEntity getPatient() {
        return patient;
    }

    public void setPatient(PatientEntity patient) {
        this.patient = patient;
    }

    public EncounterEntity getEncounter() {
        return encounter;
    }

    public void setEncounter(EncounterEntity encounter) {
        this.encounter = encounter;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }
}
