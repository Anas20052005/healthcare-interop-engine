package com.example.healthcare.validator;

import com.example.healthcare.dto.hl7.ParsedEncounter;
import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.dto.hl7.ParsedObservation;
import com.example.healthcare.dto.hl7.ParsedPatient;
import com.example.healthcare.exception.InvalidHL7Exception;
import com.example.healthcare.exception.UnsupportedHL7MessageException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of {@link HL7Validator}.
 * Validates parsed HL7 v2 messages according to the MVP requirements and healthcare mapping specification.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Message structure and type validation (ADT^A01 only)</li>
 *   <li>Patient demographic validation (PID-3 identifier, PID-5 family/given name, PID-7 date, PID-8 gender)</li>
 *   <li>Encounter validation (PV1-2 class, PV1-44 admit date, PV1-45 discharge date)</li>
 *   <li>Observation validation (OBX-2 value type, OBX-3 code, OBX-5 value, OBX-11 status)</li>
 * </ul>
 * <p>
 * Does NOT perform raw HL7 parsing, FHIR mapping, persistence, or HTTP handling.
 */
@Component
public class DefaultHL7Validator implements HL7Validator {

    private static final String SUPPORTED_MESSAGE_TYPE = "ADT^A01";
    private static final Set<String> SUPPORTED_GENDERS = Set.of("M", "F", "O", "U");
    private static final Set<String> SUPPORTED_ENCOUNTER_CLASSES = Set.of("I", "O", "E");
    private static final Set<String> SUPPORTED_OBX_VALUE_TYPES = Set.of("NM", "ST");
    private static final Set<String> SUPPORTED_OBX_STATUSES = Set.of("F", "P", "C", "N");

    private static final DateTimeFormatter STRICT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter STRICT_DATE_TIME_MIN_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMddHHmm").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter STRICT_DATE_TIME_SEC_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);

    @Override
    public void validate(ParsedHL7Data data) {
        if (data == null) {
            throw new InvalidHL7Exception("Parsed HL7 data cannot be null");
        }

        validateMessageType(data.getMessageType());
        validatePatient(data.getPatient());
        validateEncounter(data.getEncounter());
        validateObservations(data.getObservations());
    }

    /**
     * Validates that the message type is present and is supported (ADT^A01).
     */
    private void validateMessageType(String messageType) {
        if (messageType == null || messageType.trim().isEmpty()) {
            throw new InvalidHL7Exception("HL7 message type is required");
        }

        if (!SUPPORTED_MESSAGE_TYPE.equalsIgnoreCase(messageType.trim())) {
            throw new UnsupportedHL7MessageException(
                    "Unsupported HL7 message type: " + messageType + ". Only ADT^A01 is supported by the current version."
            );
        }
    }

    /**
     * Validates required patient information and demographics.
     */
    private void validatePatient(ParsedPatient patient) {
        if (patient == null) {
            throw new InvalidHL7Exception("Patient information (PID) is required");
        }

        if (patient.getPatientId() == null || patient.getPatientId().trim().isEmpty()) {
            throw new InvalidHL7Exception("Patient identifier is required");
        }

        if (patient.getFamilyName() == null || patient.getFamilyName().trim().isEmpty()) {
            throw new InvalidHL7Exception("Patient family name is required");
        }

        if (patient.getGivenName() == null || patient.getGivenName().trim().isEmpty()) {
            throw new InvalidHL7Exception("Patient given name is required");
        }

        // Validate birth date format if present
        if (patient.getBirthDate() != null && !patient.getBirthDate().trim().isEmpty()) {
            validateAndParseDateTime(patient.getBirthDate().trim(), "birth date");
        }

        // Validate gender if present
        if (patient.getGender() != null && !patient.getGender().trim().isEmpty()) {
            String genderUpper = patient.getGender().trim().toUpperCase();
            if (!SUPPORTED_GENDERS.contains(genderUpper)) {
                throw new InvalidHL7Exception(
                        "Unsupported gender: " + patient.getGender() + ". Supported values are M, F, O, U."
                );
            }
        }
    }

    /**
     * Validates encounter information when present.
     * Preserves conditional/optional nature of encounter information.
     */
    private void validateEncounter(ParsedEncounter encounter) {
        if (encounter == null) {
            return;
        }

        // Validate patient class if provided
        if (encounter.getEncounterClass() != null && !encounter.getEncounterClass().trim().isEmpty()) {
            String classUpper = encounter.getEncounterClass().trim().toUpperCase();
            if (!SUPPORTED_ENCOUNTER_CLASSES.contains(classUpper)) {
                throw new InvalidHL7Exception(
                        "Unsupported encounter class: " + encounter.getEncounterClass() + ". Supported classes are I, O, E."
                );
            }
        }

        LocalDateTime start = null;
        if (encounter.getStart() != null && !encounter.getStart().trim().isEmpty()) {
            start = validateAndParseDateTime(encounter.getStart().trim(), "encounter start date");
        }

        LocalDateTime end = null;
        if (encounter.getEnd() != null && !encounter.getEnd().trim().isEmpty()) {
            end = validateAndParseDateTime(encounter.getEnd().trim(), "encounter end date");
        }

        if (start != null && end != null && end.isBefore(start)) {
            throw new InvalidHL7Exception("Invalid encounter date: end date cannot be before start date");
        }
    }

    /**
     * Validates observations list when present.
     */
    private void validateObservations(List<ParsedObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            return;
        }

        for (int i = 0; i < observations.size(); i++) {
            ParsedObservation obs = observations.get(i);
            validateSingleObservation(obs, i + 1);
        }
    }

    /**
     * Validates a single observation entry.
     */
    private void validateSingleObservation(ParsedObservation obs, int index) {
        if (obs == null) {
            throw new InvalidHL7Exception("Observation at index " + index + " cannot be null");
        }

        if (obs.getObservationIdentifier() == null || obs.getObservationIdentifier().trim().isEmpty()) {
            throw new InvalidHL7Exception("Observation identifier is required for observation " + index);
        }

        String valueType = obs.getValueType();
        if (valueType == null || valueType.trim().isEmpty()) {
            throw new InvalidHL7Exception("Observation value type is required for observation " + index);
        }

        String vtUpper = valueType.trim().toUpperCase();
        if (!SUPPORTED_OBX_VALUE_TYPES.contains(vtUpper)) {
            throw new InvalidHL7Exception(
                    "Unsupported observation value type: " + valueType + " for observation " + index + ". Supported types are NM and ST."
            );
        }

        String value = obs.getObservationValue();
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidHL7Exception("Observation value is required for observation " + index);
        }

        if ("NM".equals(vtUpper)) {
            try {
                Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                throw new InvalidHL7Exception("Invalid numeric observation value: " + value);
            }
        }

        String status = obs.getStatus();
        if (status == null || status.trim().isEmpty()) {
            throw new InvalidHL7Exception("Observation status is required for observation " + index);
        }

        String stUpper = status.trim().toUpperCase();
        if (!SUPPORTED_OBX_STATUSES.contains(stUpper)) {
            throw new InvalidHL7Exception(
                    "Invalid observation status: " + status + " for observation " + index + ". Supported statuses are F, P, C."
            );
        }
    }

    /**
     * Validates and parses an HL7 date or date/time string.
     * Supported formats:
     * <ul>
     *   <li>YYYYMMDD (8 digits)</li>
     *   <li>YYYYMMDDHHMM (12 digits)</li>
     *   <li>YYYYMMDDHHMMSS (14 digits)</li>
     * </ul>
     *
     * @param dateStr the raw date string
     * @param fieldDescription descriptive name of the field for error reporting
     * @return parsed LocalDateTime representation
     * @throws InvalidHL7Exception if the date string is malformed or represents an invalid calendar date
     */
    private LocalDateTime validateAndParseDateTime(String dateStr, String fieldDescription) {
        if (!dateStr.matches("\\d+")) {
            throw new InvalidHL7Exception("Invalid " + fieldDescription + ": " + dateStr);
        }

        try {
            if (dateStr.length() == 8) {
                LocalDate date = LocalDate.parse(dateStr, STRICT_DATE_FORMATTER);
                return date.atStartOfDay();
            } else if (dateStr.length() == 12) {
                return LocalDateTime.parse(dateStr, STRICT_DATE_TIME_MIN_FORMATTER);
            } else if (dateStr.length() == 14) {
                return LocalDateTime.parse(dateStr, STRICT_DATE_TIME_SEC_FORMATTER);
            } else {
                throw new InvalidHL7Exception(
                        "Invalid " + fieldDescription + ": " + dateStr + ". Expected format YYYYMMDD, YYYYMMDDHHMM, or YYYYMMDDHHMMSS."
                );
            }
        } catch (DateTimeParseException e) {
            throw new InvalidHL7Exception("Invalid " + fieldDescription + ": " + dateStr);
        }
    }
}
