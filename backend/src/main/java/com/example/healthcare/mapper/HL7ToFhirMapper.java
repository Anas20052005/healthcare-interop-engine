package com.example.healthcare.mapper;

import com.example.healthcare.dto.fhir.FhirMappingResult;
import com.example.healthcare.dto.hl7.ParsedEncounter;
import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.dto.hl7.ParsedObservation;
import com.example.healthcare.dto.hl7.ParsedPatient;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;

/**
 * Maps structured parsed HL7 v2 healthcare data into HAPI FHIR R4 resources.
 */
public interface HL7ToFhirMapper {

    /**
     * Maps an entire validated ParsedHL7Data model into a collection of FHIR R4 resources.
     *
     * @param data validated parsed HL7 data
     * @return container with mapped Patient, Encounter, and Observations
     */
    FhirMappingResult map(ParsedHL7Data data);

    /**
     * Maps patient demographics into a FHIR R4 Patient resource.
     *
     * @param patient patient demographic model
     * @return FHIR Patient resource
     */
    Patient mapPatient(ParsedPatient patient);

    /**
     * Maps encounter/visit information into a FHIR R4 Encounter resource.
     *
     * @param encounter encounter model
     * @param patientId patient identifier for subject reference
     * @return FHIR Encounter resource
     */
    Encounter mapEncounter(ParsedEncounter encounter, String patientId);

    /**
     * Maps an observation into a FHIR R4 Observation resource.
     *
     * @param observation observation model
     * @param patientId patient identifier for subject reference
     * @param encounterId encounter identifier for encounter reference
     * @param index 0-based index for deterministic ID generation
     * @return FHIR Observation resource
     */
    Observation mapObservation(ParsedObservation observation, String patientId, String encounterId, int index);
}
