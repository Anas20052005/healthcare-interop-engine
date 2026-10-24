package com.example.healthcare.mapper;

import com.example.healthcare.dto.fhir.FhirMappingResult;
import com.example.healthcare.dto.hl7.ParsedEncounter;
import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.dto.hl7.ParsedObservation;
import com.example.healthcare.dto.hl7.ParsedPatient;
import com.example.healthcare.exception.HL7MappingException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of {@link HL7ToFhirMapper}.
 * Transforms structured parsed and validated HL7 v2 data into HAPI FHIR R4 resources.
 * <p>
 * Resource ID Strategy:
 * <ul>
 *   <li>Patient: Uses source patient identifier directly: {@code Patient/{patientId}}</li>
 *   <li>Encounter: Deterministic identifier: {@code ENC-{patientId}}</li>
 *   <li>Observation: Deterministic identifier: {@code OBS-{patientId}-{index+1}}</li>
 * </ul>
 * <p>
 * Does NOT access database, parse raw HL7, validate HL7 rules, or handle HTTP.
 */
@Component
public class DefaultHL7ToFhirMapper implements HL7ToFhirMapper {

    private static final String ACT_CODE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v3-ActCode";

    @Override
    public FhirMappingResult map(ParsedHL7Data data) {
        if (data == null) {
            throw new HL7MappingException("Cannot map null ParsedHL7Data");
        }

        Patient patient = null;
        String patientId = null;
        if (data.getPatient() != null) {
            patient = mapPatient(data.getPatient());
            patientId = data.getPatient().getPatientId();
        }

        Encounter encounter = null;
        String encounterId = null;
        if (data.getEncounter() != null) {
            encounter = mapEncounter(data.getEncounter(), patientId);
            if (encounter != null) {
                encounterId = encounter.getId();
            }
        }

        List<Observation> observations = new ArrayList<>();
        if (data.getObservations() != null) {
            for (int i = 0; i < data.getObservations().size(); i++) {
                ParsedObservation obsData = data.getObservations().get(i);
                Observation obs = mapObservation(obsData, patientId, encounterId, i);
                if (obs != null) {
                    observations.add(obs);
                }
            }
        }

        return new FhirMappingResult(patient, encounter, observations);
    }

    @Override
    public Patient mapPatient(ParsedPatient patientData) {
        if (patientData == null) {
            return null;
        }

        Patient patient = new Patient();
        String patientId = patientData.getPatientId();

        // PID-3 -> Patient.identifier and Patient.id
        if (patientId != null && !patientId.trim().isEmpty()) {
            patient.setId(patientId.trim());
            patient.addIdentifier().setValue(patientId.trim());
        }

        // PID-5 -> Patient.name
        HumanName name = patient.addName();
        if (patientData.getFamilyName() != null && !patientData.getFamilyName().trim().isEmpty()) {
            name.setFamily(patientData.getFamilyName().trim());
        }
        if (patientData.getGivenName() != null && !patientData.getGivenName().trim().isEmpty()) {
            name.addGiven(patientData.getGivenName().trim());
        }

        // PID-7 -> Patient.birthDate
        if (patientData.getBirthDate() != null && !patientData.getBirthDate().trim().isEmpty()) {
            String rawDate = patientData.getBirthDate().trim();
            if (rawDate.length() >= 8) {
                String formatted = rawDate.substring(0, 4) + "-" + rawDate.substring(4, 6) + "-" + rawDate.substring(6, 8);
                patient.setBirthDateElement(new DateType(formatted));
            } else {
                patient.setBirthDate(parseDateTime(rawDate));
            }
        }

        // PID-8 -> Patient.gender
        if (patientData.getGender() != null && !patientData.getGender().trim().isEmpty()) {
            switch (patientData.getGender().trim().toUpperCase()) {
                case "M":
                    patient.setGender(Enumerations.AdministrativeGender.MALE);
                    break;
                case "F":
                    patient.setGender(Enumerations.AdministrativeGender.FEMALE);
                    break;
                case "O":
                    patient.setGender(Enumerations.AdministrativeGender.OTHER);
                    break;
                case "U":
                    patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
                    break;
                default:
                    throw new HL7MappingException("Unsupported gender for FHIR mapping: " + patientData.getGender());
            }
        }

        return patient;
    }

    @Override
    public Encounter mapEncounter(ParsedEncounter encounterData, String patientId) {
        if (encounterData == null) {
            return null;
        }

        Encounter encounter = new Encounter();
        String encId = "ENC-" + (patientId != null ? patientId.trim() : "UNKNOWN");
        encounter.setId(encId);

        // Subject reference to Patient
        if (patientId != null && !patientId.trim().isEmpty()) {
            encounter.setSubject(new Reference("Patient/" + patientId.trim()));
        }

        // Encounter status
        if (encounterData.getEnd() != null && !encounterData.getEnd().trim().isEmpty()) {
            encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        } else {
            encounter.setStatus(Encounter.EncounterStatus.INPROGRESS);
        }

        // PV1-2 -> Encounter.class
        if (encounterData.getEncounterClass() != null && !encounterData.getEncounterClass().trim().isEmpty()) {
            Coding classCoding = new Coding();
            classCoding.setSystem(ACT_CODE_SYSTEM);
            switch (encounterData.getEncounterClass().trim().toUpperCase()) {
                case "I":
                    classCoding.setCode("IMP").setDisplay("inpatient encounter");
                    break;
                case "O":
                    classCoding.setCode("AMB").setDisplay("ambulatory");
                    break;
                case "E":
                    classCoding.setCode("EMER").setDisplay("emergency");
                    break;
                default:
                    classCoding.setCode(encounterData.getEncounterClass().trim())
                            .setDisplay(encounterData.getEncounterClass().trim());
                    break;
            }
            encounter.setClass_(classCoding);
        }

        // PV1-3 -> Encounter.location.display
        if (encounterData.getLocation() != null && !encounterData.getLocation().trim().isEmpty()) {
            Encounter.EncounterLocationComponent locationComponent = encounter.addLocation();
            locationComponent.setLocation(new Reference().setDisplay(encounterData.getLocation().trim()));
        }

        // PV1-44 & PV1-45 -> Encounter.period
        Period period = new Period();
        if (encounterData.getStart() != null && !encounterData.getStart().trim().isEmpty()) {
            period.setStart(parseDateTime(encounterData.getStart().trim()));
        }
        if (encounterData.getEnd() != null && !encounterData.getEnd().trim().isEmpty()) {
            period.setEnd(parseDateTime(encounterData.getEnd().trim()));
        }
        if (period.hasStart() || period.hasEnd()) {
            encounter.setPeriod(period);
        }

        return encounter;
    }

    @Override
    public Observation mapObservation(ParsedObservation obsData, String patientId, String encounterId, int index) {
        if (obsData == null) {
            return null;
        }

        Observation observation = new Observation();
        String obsId = "OBS-" + (patientId != null ? patientId.trim() : "UNKNOWN") + "-" + (index + 1);
        observation.setId(obsId);

        // Subject reference to Patient
        if (patientId != null && !patientId.trim().isEmpty()) {
            observation.setSubject(new Reference("Patient/" + patientId.trim()));
        }

        // Encounter reference
        if (encounterId != null && !encounterId.trim().isEmpty()) {
            observation.setEncounter(new Reference("Encounter/" + encounterId.trim()));
        }

        // OBX-11 -> Observation.status
        if (obsData.getStatus() != null && !obsData.getStatus().trim().isEmpty()) {
            switch (obsData.getStatus().trim().toUpperCase()) {
                case "F":
                case "N":
                    observation.setStatus(Observation.ObservationStatus.FINAL);
                    break;
                case "P":
                    observation.setStatus(Observation.ObservationStatus.PRELIMINARY);
                    break;
                case "C":
                    observation.setStatus(Observation.ObservationStatus.CORRECTED);
                    break;
                default:
                    throw new HL7MappingException("Unsupported observation status: " + obsData.getStatus());
            }
        } else {
            observation.setStatus(Observation.ObservationStatus.FINAL);
        }

        // OBX-3 -> Observation.code
        if (obsData.getObservationIdentifier() != null && !obsData.getObservationIdentifier().trim().isEmpty()) {
            CodeableConcept codeableConcept = new CodeableConcept();
            String raw = obsData.getObservationIdentifier().trim();
            if (raw.contains("^")) {
                String[] parts = raw.split("\\^", -1);
                String code = parts.length > 0 ? parts[0].trim() : "";
                String display = parts.length > 1 ? parts[1].trim() : code;
                Coding coding = codeableConcept.addCoding();
                coding.setCode(code);
                if (!display.isEmpty()) {
                    coding.setDisplay(display);
                }
                codeableConcept.setText(!display.isEmpty() ? display : code);
            } else {
                codeableConcept.addCoding().setCode(raw).setDisplay(raw);
                codeableConcept.setText(raw);
            }
            observation.setCode(codeableConcept);
        }

        // OBX-2 & OBX-5 & OBX-6 -> Observation.value[x]
        String valueType = obsData.getValueType() != null ? obsData.getValueType().trim().toUpperCase() : "";
        String valueStr = obsData.getObservationValue() != null ? obsData.getObservationValue().trim() : "";

        if ("NM".equals(valueType)) {
            Quantity quantity = new Quantity();
            try {
                quantity.setValue(new BigDecimal(valueStr));
            } catch (NumberFormatException e) {
                throw new HL7MappingException("Invalid numeric observation value: " + valueStr, e);
            }
            if (obsData.getUnits() != null && !obsData.getUnits().trim().isEmpty()) {
                quantity.setUnit(obsData.getUnits().trim());
            }
            observation.setValue(quantity);
        } else if ("ST".equals(valueType)) {
            observation.setValue(new StringType(valueStr));
        }

        // OBX-7 -> Observation.referenceRange
        if (obsData.getReferenceRange() != null && !obsData.getReferenceRange().trim().isEmpty()) {
            Observation.ObservationReferenceRangeComponent refRange = observation.addReferenceRange();
            refRange.setText(obsData.getReferenceRange().trim());
        }

        return observation;
    }

    /**
     * Parses an HL7 timestamp string into a {@link Date}.
     * Supports formats: YYYYMMDD, YYYYMMDDHHMM, YYYYMMDDHHMMSS.
     */
    private Date parseDateTime(String hl7Date) {
        if (hl7Date == null || hl7Date.trim().isEmpty()) {
            return null;
        }

        String trimmed = hl7Date.trim();
        try {
            if (trimmed.length() == 8) {
                return new SimpleDateFormat("yyyyMMdd").parse(trimmed);
            } else if (trimmed.length() == 12) {
                return new SimpleDateFormat("yyyyMMddHHmm").parse(trimmed);
            } else if (trimmed.length() >= 14) {
                return new SimpleDateFormat("yyyyMMddHHmmss").parse(trimmed.substring(0, 14));
            }
        } catch (ParseException e) {
            throw new HL7MappingException("Failed to parse date: " + hl7Date, e);
        }

        throw new HL7MappingException("Unsupported date format for FHIR mapping: " + hl7Date);
    }
}
