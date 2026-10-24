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
 * Persistence entity for encounter/visit data extracted from HL7 PV1 segment.
 * Kept strictly separate from HAPI FHIR Encounter model.
 */
@Entity
@Table(name = "encounters", indexes = {
        @Index(name = "idx_enc_patient_hosp", columnList = "patient_id, hospital_id"),
        @Index(name = "idx_enc_ident_hosp", columnList = "encounter_identifier, hospital_id"),
        @Index(name = "idx_enc_hosp_created", columnList = "hospital_id, created_at")
})
public class EncounterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encounter_identifier", length = 100)
    private String encounterIdentifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(name = "patient_class", length = 50)
    private String patientClass;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "admit_date_time")
    private LocalDateTime admitDateTime;

    @Column(name = "discharge_date_time")
    private LocalDateTime dischargeDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public EncounterEntity() {
    }

    public EncounterEntity(String encounterIdentifier, PatientEntity patient, String patientClass, String location, LocalDateTime admitDateTime, LocalDateTime dischargeDateTime) {
        this(encounterIdentifier, patient, patientClass, location, admitDateTime, dischargeDateTime, null);
    }

    public EncounterEntity(String encounterIdentifier, PatientEntity patient, String patientClass, String location, LocalDateTime admitDateTime, LocalDateTime dischargeDateTime, Hospital hospital) {
        this.encounterIdentifier = encounterIdentifier;
        this.patient = patient;
        this.patientClass = patientClass;
        this.location = location;
        this.admitDateTime = admitDateTime;
        this.dischargeDateTime = dischargeDateTime;
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

    public String getEncounterIdentifier() {
        return encounterIdentifier;
    }

    public void setEncounterIdentifier(String encounterIdentifier) {
        this.encounterIdentifier = encounterIdentifier;
    }

    public PatientEntity getPatient() {
        return patient;
    }

    public void setPatient(PatientEntity patient) {
        this.patient = patient;
    }

    public String getPatientClass() {
        return patientClass;
    }

    public void setPatientClass(String patientClass) {
        this.patientClass = patientClass;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getAdmitDateTime() {
        return admitDateTime;
    }

    public void setAdmitDateTime(LocalDateTime admitDateTime) {
        this.admitDateTime = admitDateTime;
    }

    public LocalDateTime getDischargeDateTime() {
        return dischargeDateTime;
    }

    public void setDischargeDateTime(LocalDateTime dischargeDateTime) {
        this.dischargeDateTime = dischargeDateTime;
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
