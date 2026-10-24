package com.example.healthcare.validator;

import com.example.healthcare.dto.hl7.ParsedEncounter;
import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.dto.hl7.ParsedObservation;
import com.example.healthcare.dto.hl7.ParsedPatient;
import com.example.healthcare.exception.InvalidHL7Exception;
import com.example.healthcare.exception.UnsupportedHL7MessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HL7ValidatorTest {

    private HL7Validator validator;

    @BeforeEach
    void setUp() {
        validator = new DefaultHL7Validator();
    }

    /**
     * Helper to construct a clean, valid ADT^A01 synthetic test model.
     */
    private ParsedHL7Data createValidData() {
        ParsedHL7Data data = new ParsedHL7Data();
        data.setMessageType("ADT^A01");
        data.setMessageControlId("MSG00001");
        data.setVersion("2.5");

        ParsedPatient patient = new ParsedPatient(
                "P1001",
                "Sharma",
                "Rahul",
                "19950510",
                "M"
        );
        data.setPatient(patient);

        ParsedEncounter encounter = new ParsedEncounter(
                "I",
                "WARD1",
                "202610080900",
                "202610151200"
        );
        data.setEncounter(encounter);

        List<ParsedObservation> observations = new ArrayList<>();
        observations.add(new ParsedObservation(
                "NM",
                "GLUCOSE",
                "120",
                "mg/dL",
                "70-100",
                "F"
        ));
        observations.add(new ParsedObservation(
                "NM",
                "HEMOGLOBIN",
                "13.5",
                "g/dL",
                "12-16",
                "F"
        ));
        data.setObservations(observations);

        return data;
    }

    @Test
    @DisplayName("1. Valid ADT^A01 data passes validation without exceptions")
    void testValidAdtA01DataPasses() {
        ParsedHL7Data data = createValidData();
        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    @DisplayName("2. Missing message type fails validation")
    void testMissingMessageTypeFails() {
        ParsedHL7Data data = createValidData();

        data.setMessageType(null);
        InvalidHL7Exception exNull = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(exNull.getMessage().contains("HL7 message type is required"));

        data.setMessageType("   ");
        InvalidHL7Exception exBlank = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(exBlank.getMessage().contains("HL7 message type is required"));
    }

    @Test
    @DisplayName("3. Unsupported message type fails with UnsupportedHL7MessageException")
    void testUnsupportedMessageTypeFails() {
        ParsedHL7Data data = createValidData();

        data.setMessageType("ADT^A08");
        UnsupportedHL7MessageException ex1 =
                assertThrows(UnsupportedHL7MessageException.class, () -> validator.validate(data));
        assertTrue(ex1.getMessage().contains("Unsupported HL7 message type: ADT^A08"));

        data.setMessageType("ORM^O01");
        UnsupportedHL7MessageException ex2 =
                assertThrows(UnsupportedHL7MessageException.class, () -> validator.validate(data));
        assertTrue(ex2.getMessage().contains("Unsupported HL7 message type: ORM^O01"));
    }

    @Test
    @DisplayName("4. Missing patient identifier fails validation")
    void testMissingPatientIdentifierFails() {
        ParsedHL7Data data = createValidData();

        data.getPatient().setPatientId(null);
        InvalidHL7Exception exNull = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(exNull.getMessage().contains("Patient identifier is required"));

        data.getPatient().setPatientId("  ");
        InvalidHL7Exception exBlank = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(exBlank.getMessage().contains("Patient identifier is required"));
    }

    @Test
    @DisplayName("5. Missing family name fails validation")
    void testMissingFamilyNameFails() {
        ParsedHL7Data data = createValidData();

        data.getPatient().setFamilyName(null);
        InvalidHL7Exception exNull = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(exNull.getMessage().contains("Patient family name is required"));

        data.getPatient().setFamilyName("  ");
        InvalidHL7Exception exBlank = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(exBlank.getMessage().contains("Patient family name is required"));
    }

    @Test
    @DisplayName("6. Missing given name fails validation")
    void testMissingGivenNameFails() {
        ParsedHL7Data data = createValidData();

        data.getPatient().setGivenName(null);
        InvalidHL7Exception exNull = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(exNull.getMessage().contains("Patient given name is required"));

        data.getPatient().setGivenName("  ");
        InvalidHL7Exception exBlank = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(exBlank.getMessage().contains("Patient given name is required"));
    }

    @Test
    @DisplayName("7. Invalid birth date fails validation")
    void testInvalidBirthDateFails() {
        ParsedHL7Data data = createValidData();

        // Month 13, day 40
        data.getPatient().setBirthDate("19951340");
        InvalidHL7Exception ex1 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex1.getMessage().contains("Invalid birth date"));

        // Non leap year Feb 29
        data.getPatient().setBirthDate("19950229");
        InvalidHL7Exception ex2 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex2.getMessage().contains("Invalid birth date"));

        // Alphanumeric string
        data.getPatient().setBirthDate("abcd1234");
        InvalidHL7Exception ex3 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex3.getMessage().contains("Invalid birth date"));

        // Hyphenated date (not standard HL7 numeric format)
        data.getPatient().setBirthDate("1995-05-10");
        InvalidHL7Exception ex4 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex4.getMessage().contains("Invalid birth date"));
    }

    @Test
    @DisplayName("8. Valid birth date passes validation")
    void testValidBirthDatePasses() {
        ParsedHL7Data data = createValidData();

        // 8-digit date
        data.getPatient().setBirthDate("19950510");
        assertDoesNotThrow(() -> validator.validate(data));

        // Leap year valid date
        data.getPatient().setBirthDate("19960229");
        assertDoesNotThrow(() -> validator.validate(data));

        // 12-digit timestamp
        data.getPatient().setBirthDate("199505101430");
        assertDoesNotThrow(() -> validator.validate(data));

        // 14-digit timestamp
        data.getPatient().setBirthDate("19950510143000");
        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    @DisplayName("9. Unsupported gender fails validation")
    void testUnsupportedGenderFails() {
        ParsedHL7Data data = createValidData();

        data.getPatient().setGender("X");
        InvalidHL7Exception ex1 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex1.getMessage().contains("Unsupported gender"));

        data.getPatient().setGender("male");
        InvalidHL7Exception ex2 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex2.getMessage().contains("Unsupported gender"));
    }

    @Test
    @DisplayName("10. Valid gender values pass validation")
    void testValidGenderPasses() {
        ParsedHL7Data data = createValidData();

        data.getPatient().setGender("M");
        assertDoesNotThrow(() -> validator.validate(data));

        data.getPatient().setGender("F");
        assertDoesNotThrow(() -> validator.validate(data));

        data.getPatient().setGender("O");
        assertDoesNotThrow(() -> validator.validate(data));

        data.getPatient().setGender("U");
        assertDoesNotThrow(() -> validator.validate(data));

        // Case-insensitive
        data.getPatient().setGender("m");
        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    @DisplayName("11. Invalid encounter data fails validation where required")
    void testInvalidEncounterDataFails() {
        ParsedHL7Data data = createValidData();

        // Unsupported encounter class
        data.getEncounter().setEncounterClass("INVALID_CLASS");
        InvalidHL7Exception ex1 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex1.getMessage().contains("Unsupported encounter class"));

        // Malformed start date
        data.getEncounter().setEncounterClass("I");
        data.getEncounter().setStart("bad-date");
        InvalidHL7Exception ex2 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex2.getMessage().contains("Invalid encounter start date"));

        // End date before start date
        data.getEncounter().setStart("202610151200");
        data.getEncounter().setEnd("202610080900");
        InvalidHL7Exception ex3 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex3.getMessage().contains("end date cannot be before start date"));
    }

    @Test
    @DisplayName("12. Valid encounter data passes validation")
    void testValidEncounterDataPasses() {
        ParsedHL7Data data = createValidData();

        data.getEncounter().setEncounterClass("I");
        assertDoesNotThrow(() -> validator.validate(data));

        data.getEncounter().setEncounterClass("O");
        assertDoesNotThrow(() -> validator.validate(data));

        data.getEncounter().setEncounterClass("E");
        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    @DisplayName("13. Missing optional encounter information behaves gracefully")
    void testMissingOptionalEncounterInformation() {
        ParsedHL7Data data = createValidData();

        // Location and end date absent
        data.getEncounter().setLocation(null);
        data.getEncounter().setEnd(null);
        assertDoesNotThrow(() -> validator.validate(data));

        // Entire encounter object absent
        data.setEncounter(null);
        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    @DisplayName("14. Valid numeric observation passes validation")
    void testValidNumericObservationPasses() {
        ParsedHL7Data data = createValidData();
        data.getObservations().clear();

        data.getObservations().add(new ParsedObservation(
                "NM", "GLUCOSE", "120", "mg/dL", "70-100", "F"
        ));
        data.getObservations().add(new ParsedObservation(
                "NM", "HEMOGLOBIN", "13.5", "g/dL", "12-16", "F"
        ));
        data.getObservations().add(new ParsedObservation(
                "NM", "TEMP_DIFF", "-0.8", "C", null, "F"
        ));

        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    @DisplayName("15. Invalid numeric observation value fails validation")
    void testInvalidNumericObservationFails() {
        ParsedHL7Data data = createValidData();
        data.getObservations().clear();

        data.getObservations().add(new ParsedObservation(
                "NM", "GLUCOSE", "abc", "mg/dL", "70-100", "F"
        ));

        InvalidHL7Exception ex = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex.getMessage().contains("Invalid numeric observation value"));
    }

    @Test
    @DisplayName("16. Valid string observation passes validation")
    void testValidStringObservationPasses() {
        ParsedHL7Data data = createValidData();
        data.getObservations().clear();

        data.getObservations().add(new ParsedObservation(
                "ST", "COMMENT", "Patient is in stable condition", null, null, "F"
        ));

        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    @DisplayName("17. Unsupported observation value type fails validation")
    void testUnsupportedObservationValueTypeFails() {
        ParsedHL7Data data = createValidData();
        data.getObservations().clear();

        data.getObservations().add(new ParsedObservation(
                "TX", "NOTES", "Detailed clinical note", null, null, "F"
        ));

        InvalidHL7Exception ex = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex.getMessage().contains("Unsupported observation value type: TX"));
    }

    @Test
    @DisplayName("18. Invalid observation status fails validation")
    void testInvalidObservationStatusFails() {
        ParsedHL7Data data = createValidData();
        data.getObservations().clear();

        // Unsupported status
        data.getObservations().add(new ParsedObservation(
                "NM", "GLUCOSE", "120", "mg/dL", "70-100", "INVALID"
        ));
        InvalidHL7Exception ex1 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex1.getMessage().contains("Invalid observation status"));

        // Null status
        data.getObservations().clear();
        data.getObservations().add(new ParsedObservation(
                "NM", "GLUCOSE", "120", "mg/dL", "70-100", null
        ));
        InvalidHL7Exception ex2 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex2.getMessage().contains("Observation status is required"));
    }

    @Test
    @DisplayName("19. Multiple valid observations pass validation")
    void testMultipleValidObservationsPass() {
        ParsedHL7Data data = createValidData();
        data.getObservations().clear();

        data.getObservations().add(new ParsedObservation("NM", "GLUCOSE", "95", "mg/dL", "70-100", "F"));
        data.getObservations().add(new ParsedObservation("NM", "HEMOGLOBIN", "14.2", "g/dL", "12-16", "P"));
        data.getObservations().add(new ParsedObservation("ST", "NOTE", "Fasting blood test", null, null, "C"));

        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    @DisplayName("20. One invalid observation among valid ones causes validation failure")
    void testOneInvalidObservationCausesValidationFailure() {
        ParsedHL7Data data = createValidData();
        data.getObservations().clear();

        data.getObservations().add(new ParsedObservation("NM", "GLUCOSE", "95", "mg/dL", "70-100", "F"));
        data.getObservations().add(new ParsedObservation("NM", "POTASSIUM", "not_a_number", "mmol/L", "3.5-5.0", "F"));

        InvalidHL7Exception ex = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex.getMessage().contains("Invalid numeric observation value"));
    }

    @Test
    @DisplayName("21. Null root parsed HL7 data fails validation")
    void testNullParsedDataFails() {
        InvalidHL7Exception ex = assertThrows(InvalidHL7Exception.class, () -> validator.validate(null));
        assertTrue(ex.getMessage().contains("Parsed HL7 data cannot be null"));
    }

    @Test
    @DisplayName("22. Missing patient object fails validation")
    void testMissingPatientObjectFails() {
        ParsedHL7Data data = createValidData();
        data.setPatient(null);

        InvalidHL7Exception ex = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex.getMessage().contains("Patient information (PID) is required"));
    }

    @Test
    @DisplayName("23. Missing observation identifier or value fails validation")
    void testMissingObservationFieldsFail() {
        ParsedHL7Data data = createValidData();
        data.getObservations().clear();

        // Missing identifier
        data.getObservations().add(new ParsedObservation("NM", null, "120", "mg/dL", null, "F"));
        InvalidHL7Exception ex1 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex1.getMessage().contains("Observation identifier is required"));

        // Missing value
        data.getObservations().clear();
        data.getObservations().add(new ParsedObservation("NM", "GLUCOSE", null, "mg/dL", null, "F"));
        InvalidHL7Exception ex2 = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
        assertTrue(ex2.getMessage().contains("Observation value is required"));
    }

    @Test
    @DisplayName("24. Valid message without optional encounter and observations passes validation")
    void testOmittedOptionalEncounterAndObservationsPasses() {
        ParsedHL7Data data = createValidData();
        data.setEncounter(null);
        data.setObservations(null);

        assertDoesNotThrow(() -> validator.validate(data));
    }

    @Test
    @DisplayName("25. Explicitly rejects unsupported OBX data types (CWE, CE, TX)")
    void testUnsupportedObxDataTypesRejectedWithClearMessage() {
        ParsedHL7Data data = createValidData();

        for (String unsupportedType : List.of("CWE", "CE", "TX", "ED", "RP")) {
            data.getObservations().clear();
            data.getObservations().add(new ParsedObservation(unsupportedType, "CODE_TEST", "Observation value text", null, null, "F"));

            InvalidHL7Exception ex = assertThrows(InvalidHL7Exception.class, () -> validator.validate(data));
            assertTrue(ex.getMessage().contains("Unsupported observation value type: " + unsupportedType));
            assertTrue(ex.getMessage().contains("Supported types are NM and ST"));
        }
    }

    @Test
    @DisplayName("26. Explicitly rejects unsupported HL7 trigger events (e.g. ADT^A08, ORU^R01)")
    void testUnsupportedTriggerEventsRejectedWithClearMessage() {
        ParsedHL7Data data = createValidData();

        for (String unsupportedMsg : List.of("ADT^A08", "ADT^A03", "ORU^R01", "MDM^T02")) {
            data.setMessageType(unsupportedMsg);

            UnsupportedHL7MessageException ex = assertThrows(UnsupportedHL7MessageException.class, () -> validator.validate(data));
            assertTrue(ex.getMessage().contains("Unsupported HL7 message type: " + unsupportedMsg));
            assertTrue(ex.getMessage().contains("Only ADT^A01 is supported by the current version"));
        }
    }
}
