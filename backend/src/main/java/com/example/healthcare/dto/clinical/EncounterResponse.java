package com.example.healthcare.dto.clinical;

import java.time.LocalDateTime;

public class EncounterResponse {

    private Long id;
    private String encounterIdentifier;
    private Long patientId;
    private String patientClass;
    private String location;
    private LocalDateTime admitDateTime;
    private LocalDateTime dischargeDateTime;
    private LocalDateTime createdAt;
    private Long hospitalId;
    private String hospitalName;

    public EncounterResponse() {
    }

    public EncounterResponse(Long id, String encounterIdentifier, Long patientId, String patientClass,
                             String location, LocalDateTime admitDateTime, LocalDateTime dischargeDateTime,
                             LocalDateTime createdAt, Long hospitalId, String hospitalName) {
        this.id = id;
        this.encounterIdentifier = encounterIdentifier;
        this.patientId = patientId;
        this.patientClass = patientClass;
        this.location = location;
        this.admitDateTime = admitDateTime;
        this.dischargeDateTime = dischargeDateTime;
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

    public String getEncounterIdentifier() {
        return encounterIdentifier;
    }

    public void setEncounterIdentifier(String encounterIdentifier) {
        this.encounterIdentifier = encounterIdentifier;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
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
