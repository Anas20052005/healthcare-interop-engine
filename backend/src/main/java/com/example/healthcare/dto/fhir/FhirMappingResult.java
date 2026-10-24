package com.example.healthcare.dto.fhir;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;

import java.util.ArrayList;
import java.util.List;

/**
 * Container holding the discrete FHIR R4 resources generated from an HL7 message conversion.
 */
public class FhirMappingResult {

    private Patient patient;
    private Encounter encounter;
    private List<Observation> observations = new ArrayList<>();

    public FhirMappingResult() {
    }

    public FhirMappingResult(Patient patient, Encounter encounter, List<Observation> observations) {
        this.patient = patient;
        this.encounter = encounter;
        this.observations = observations != null ? observations : new ArrayList<>();
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public void setObservations(List<Observation> observations) {
        this.observations = observations != null ? observations : new ArrayList<>();
    }
}
