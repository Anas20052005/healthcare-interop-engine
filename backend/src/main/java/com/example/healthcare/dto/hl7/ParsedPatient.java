package com.example.healthcare.dto.hl7;

/**
 * Structured model representing patient demographics extracted from HL7 PID segment.
 * Preserves raw extracted values without premature clinical validation or FHIR mapping.
 */
public class ParsedPatient {

    private String patientId;
    private String familyName;
    private String givenName;
    private String birthDate;
    private String gender;

    public ParsedPatient() {
    }

    public ParsedPatient(String patientId, String familyName, String givenName, String birthDate, String gender) {
        this.patientId = patientId;
        this.familyName = familyName;
        this.givenName = givenName;
        this.birthDate = birthDate;
        this.gender = gender;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
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

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "ParsedPatient{" +
                "patientId='" + patientId + '\'' +
                ", familyName='" + familyName + '\'' +
                ", givenName='" + givenName + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", gender='" + gender + '\'' +
                '}';
    }
}
