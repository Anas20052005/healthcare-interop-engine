package com.example.healthcare.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence entity for patient demographic data extracted from HL7 PID segment.
 * Kept strictly separate from HAPI FHIR Patient model.
 */
@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patient_ident_hosp", columnList = "patient_identifier, hospital_id"),
        @Index(name = "idx_patient_hosp_created", columnList = "hospital_id, created_at")
})
public class PatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_identifier", nullable = false, length = 100)
    private String patientIdentifier;

    @Column(name = "family_name", length = 100)
    private String familyName;

    @Column(name = "given_name", length = 100)
    private String givenName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "gender", length = 20)
    private String gender;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EncounterEntity> encounters = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ObservationEntity> observations = new ArrayList<>();

    public PatientEntity() {
    }

    public PatientEntity(String patientIdentifier, String familyName, String givenName, LocalDate birthDate, String gender) {
        this(patientIdentifier, familyName, givenName, birthDate, gender, null);
    }

    public PatientEntity(String patientIdentifier, String familyName, String givenName, LocalDate birthDate, String gender, Hospital hospital) {
        this.patientIdentifier = patientIdentifier;
        this.familyName = familyName;
        this.givenName = givenName;
        this.birthDate = birthDate;
        this.gender = gender;
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

    public String getPatientIdentifier() {
        return patientIdentifier;
    }

    public void setPatientIdentifier(String patientIdentifier) {
        this.patientIdentifier = patientIdentifier;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<EncounterEntity> getEncounters() {
        return encounters;
    }

    public void setEncounters(List<EncounterEntity> encounters) {
        this.encounters = encounters;
    }

    public List<ObservationEntity> getObservations() {
        return observations;
    }

    public void setObservations(List<ObservationEntity> observations) {
        this.observations = observations;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }
}
