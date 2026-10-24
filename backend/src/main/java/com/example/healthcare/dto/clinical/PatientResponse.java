package com.example.healthcare.dto.clinical;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PatientResponse {

    private Long id;
    private String patientIdentifier;
    private String familyName;
    private String givenName;
    private LocalDate birthDate;
    private String gender;
    private LocalDateTime createdAt;
    private Long hospitalId;
    private String hospitalName;

    public PatientResponse() {
    }

    public PatientResponse(Long id, String patientIdentifier, String familyName, String givenName,
                           LocalDate birthDate, String gender, LocalDateTime createdAt,
                           Long hospitalId, String hospitalName) {
        this.id = id;
        this.patientIdentifier = patientIdentifier;
        this.familyName = familyName;
        this.givenName = givenName;
        this.birthDate = birthDate;
        this.gender = gender;
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
