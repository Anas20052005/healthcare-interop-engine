package com.example.healthcare.mapper;

import ca.uhn.fhir.context.FhirContext;
import com.example.healthcare.dto.fhir.FhirMappingResult;
import com.example.healthcare.dto.hl7.ParsedEncounter;
import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.dto.hl7.ParsedObservation;
import com.example.healthcare.dto.hl7.ParsedPatient;
import com.example.healthcare.service.FhirSerializationService;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HL7ToFhirMapperTest {

    private HL7ToFhirMapper mapper;
    private FhirSerializationService serializationService;

    @BeforeEach
    void setUp() {
        mapper = new DefaultHL7ToFhirMapper();
        serializationService = new FhirSerializationService(FhirContext.forR4());
    }

    // =========================================================================
    // PATIENT MAPPING TESTS (1 - 5)
    // =========================================================================

    @Test
    @DisplayName("1. Patient identifier mapping")
    void testPatientIdentifierMapping() {
        ParsedPatient parsed = new ParsedPatient("P1001", "Sharma", "Rahul", "19950510", "M");
        Patient patient = mapper.mapPatient(parsed);

        assertNotNull(patient);
        assertEquals("P1001", patient.getIdElement().getIdPart());
        assertEquals(1, patient.getIdentifier().size());
        assertEquals("P1001", patient.getIdentifierFirstRep().getValue());
    }

    @Test
    @DisplayName("2. Patient name mapping")
    void testPatientNameMapping() {
        ParsedPatient parsed = new ParsedPatient("P1001", "Sharma", "Rahul", "19950510", "M");
        Patient patient = mapper.mapPatient(parsed);

        assertNotNull(patient);
        assertEquals(1, patient.getName().size());
        assertEquals("Sharma", patient.getNameFirstRep().getFamily());
        assertEquals("Rahul", patient.getNameFirstRep().getGivenAsSingleString());
    }

    @Test
    @DisplayName("3. Birth date mapping")
    void testBirthDateMapping() {
        ParsedPatient parsed = new ParsedPatient("P1001", "Sharma", "Rahul", "19950510", "M");
        Patient patient = mapper.mapPatient(parsed);

        assertNotNull(patient);
        assertNotNull(patient.getBirthDate());
        assertEquals("1995-05-10", patient.getBirthDateElement().getValueAsString());
    }

    @Test
    @DisplayName("4. Male gender mapping")
    void testMaleGenderMapping() {
        ParsedPatient parsed = new ParsedPatient("P1001", "Sharma", "Rahul", "19950510", "M");
        Patient patient = mapper.mapPatient(parsed);

        assertNotNull(patient);
        assertEquals(Enumerations.AdministrativeGender.MALE, patient.getGender());
    }

    @Test
    @DisplayName("5. Female gender mapping")
    void testFemaleGenderMapping() {
        ParsedPatient parsed = new ParsedPatient("P1002", "Patel", "Priya", "19980820", "F");
        Patient patient = mapper.mapPatient(parsed);

        assertNotNull(patient);
        assertEquals(Enumerations.AdministrativeGender.FEMALE, patient.getGender());
    }

    // =========================================================================
    // ENCOUNTER MAPPING TESTS (6 - 10)
    // =========================================================================

    @Test
    @DisplayName("6. Encounter class mapping")
    void testEncounterClassMapping() {
        ParsedEncounter parsedInpatient = new ParsedEncounter("I", "WARD1", "202610080900", null);
        Encounter encInpatient = mapper.mapEncounter(parsedInpatient, "P1001");
        assertNotNull(encInpatient);
        assertNotNull(encInpatient.getClass_());
        assertEquals("IMP", encInpatient.getClass_().getCode());
        assertEquals("inpatient encounter", encInpatient.getClass_().getDisplay());

        ParsedEncounter parsedOutpatient = new ParsedEncounter("O", "CLINIC", "202610080900", null);
        Encounter encOutpatient = mapper.mapEncounter(parsedOutpatient, "P1001");
        assertEquals("AMB", encOutpatient.getClass_().getCode());
        assertEquals("ambulatory", encOutpatient.getClass_().getDisplay());

        ParsedEncounter parsedEmergency = new ParsedEncounter("E", "ER", "202610080900", null);
        Encounter encEmergency = mapper.mapEncounter(parsedEmergency, "P1001");
        assertEquals("EMER", encEmergency.getClass_().getCode());
        assertEquals("emergency", encEmergency.getClass_().getDisplay());
    }

    @Test
    @DisplayName("7. Encounter location mapping")
    void testEncounterLocationMapping() {
        ParsedEncounter parsed = new ParsedEncounter("I", "WARD1", "202610080900", null);
        Encounter encounter = mapper.mapEncounter(parsed, "P1001");

        assertNotNull(encounter);
        assertEquals(1, encounter.getLocation().size());
        assertEquals("WARD1", encounter.getLocationFirstRep().getLocation().getDisplay());
    }

    @Test
    @DisplayName("8. Encounter start mapping")
    void testEncounterStartMapping() {
        ParsedEncounter parsed = new ParsedEncounter("I", "WARD1", "202610080900", null);
        Encounter encounter = mapper.mapEncounter(parsed, "P1001");

        assertNotNull(encounter);
        assertNotNull(encounter.getPeriod());
        assertNotNull(encounter.getPeriod().getStart());
        assertNull(encounter.getPeriod().getEnd());
    }

    @Test
    @DisplayName("9. Encounter end mapping")
    void testEncounterEndMapping() {
        ParsedEncounter parsed = new ParsedEncounter("I", "WARD1", "202610080900", "202610151200");
        Encounter encounter = mapper.mapEncounter(parsed, "P1001");

        assertNotNull(encounter);
        assertNotNull(encounter.getPeriod());
        assertNotNull(encounter.getPeriod().getStart());
        assertNotNull(encounter.getPeriod().getEnd());
        assertEquals(Encounter.EncounterStatus.FINISHED, encounter.getStatus());
    }

    @Test
    @DisplayName("10. Encounter patient reference")
    void testEncounterPatientReference() {
        ParsedEncounter parsed = new ParsedEncounter("I", "WARD1", "202610080900", null);
        Encounter encounter = mapper.mapEncounter(parsed, "P1001");

        assertNotNull(encounter);
        assertNotNull(encounter.getSubject());
        assertEquals("Patient/P1001", encounter.getSubject().getReference());
    }

    // =========================================================================
    // OBSERVATION MAPPING TESTS (11 - 20)
    // =========================================================================

    @Test
    @DisplayName("11. NM observation creates Quantity")
    void testNmObservationCreatesQuantity() {
        ParsedObservation parsed = new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", "70-100", "F");
        Observation obs = mapper.mapObservation(parsed, "P1001", "ENC-P1001", 0);

        assertNotNull(obs);
        assertNotNull(obs.getValue());
        assertTrue(obs.getValue() instanceof Quantity);
    }

    @Test
    @DisplayName("12. NM numeric value is mapped correctly")
    void testNmNumericValueMappedCorrectly() {
        ParsedObservation parsed = new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", "70-100", "F");
        Observation obs = mapper.mapObservation(parsed, "P1001", "ENC-P1001", 0);

        Quantity qty = (Quantity) obs.getValue();
        assertEquals(new BigDecimal("120"), qty.getValue());
    }

    @Test
    @DisplayName("13. Unit mapping")
    void testUnitMapping() {
        ParsedObservation parsed = new ParsedObservation("NM", "HEMOGLOBIN", "13.5", "g/dL", "12-16", "F");
        Observation obs = mapper.mapObservation(parsed, "P1001", "ENC-P1001", 0);

        Quantity qty = (Quantity) obs.getValue();
        assertEquals("g/dL", qty.getUnit());
    }

    @Test
    @DisplayName("14. ST observation creates string value")
    void testStObservationCreatesStringValue() {
        ParsedObservation parsed = new ParsedObservation("ST", "COMMENT", "Patient stable", null, null, "F");
        Observation obs = mapper.mapObservation(parsed, "P1001", "ENC-P1001", 0);

        assertNotNull(obs);
        assertNotNull(obs.getValue());
        assertTrue(obs.getValue() instanceof StringType);
        assertEquals("Patient stable", ((StringType) obs.getValue()).getValue());
    }

    @Test
    @DisplayName("15. Observation code mapping")
    void testObservationCodeMapping() {
        // Simple code
        ParsedObservation parsedSimple = new ParsedObservation("NM", "GLUCOSE", "95", "mg/dL", null, "F");
        Observation obsSimple = mapper.mapObservation(parsedSimple, "P1001", "ENC-P1001", 0);
        assertEquals("GLUCOSE", obsSimple.getCode().getText());

        // Component code with display text: GLUCOSE^Blood Glucose
        ParsedObservation parsedComponent = new ParsedObservation("NM", "GLUCOSE^Blood Glucose", "95", "mg/dL", null, "F");
        Observation obsComp = mapper.mapObservation(parsedComponent, "P1001", "ENC-P1001", 0);
        CodeableConcept cc = obsComp.getCode();
        assertEquals("GLUCOSE", cc.getCodingFirstRep().getCode());
        assertEquals("Blood Glucose", cc.getCodingFirstRep().getDisplay());
        assertEquals("Blood Glucose", cc.getText());
    }

    @Test
    @DisplayName("16. Reference range mapping")
    void testReferenceRangeMapping() {
        ParsedObservation parsed = new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", "70-100", "F");
        Observation obs = mapper.mapObservation(parsed, "P1001", "ENC-P1001", 0);

        assertNotNull(obs);
        assertEquals(1, obs.getReferenceRange().size());
        assertEquals("70-100", obs.getReferenceRangeFirstRep().getText());
    }

    @Test
    @DisplayName("17. Observation status mapping")
    void testObservationStatusMapping() {
        ParsedObservation parsedF = new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", null, "F");
        Observation obsF = mapper.mapObservation(parsedF, "P1001", "ENC-P1001", 0);
        assertEquals(Observation.ObservationStatus.FINAL, obsF.getStatus());

        ParsedObservation parsedP = new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", null, "P");
        Observation obsP = mapper.mapObservation(parsedP, "P1001", "ENC-P1001", 0);
        assertEquals(Observation.ObservationStatus.PRELIMINARY, obsP.getStatus());

        ParsedObservation parsedC = new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", null, "C");
        Observation obsC = mapper.mapObservation(parsedC, "P1001", "ENC-P1001", 0);
        assertEquals(Observation.ObservationStatus.CORRECTED, obsC.getStatus());
    }

    @Test
    @DisplayName("18. Observation patient reference")
    void testObservationPatientReference() {
        ParsedObservation parsed = new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", null, "F");
        Observation obs = mapper.mapObservation(parsed, "P1001", "ENC-P1001", 0);

        assertNotNull(obs);
        assertNotNull(obs.getSubject());
        assertEquals("Patient/P1001", obs.getSubject().getReference());
    }

    @Test
    @DisplayName("19. Observation encounter reference")
    void testObservationEncounterReference() {
        ParsedObservation parsed = new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", null, "F");
        Observation obs = mapper.mapObservation(parsed, "P1001", "ENC-P1001", 0);

        assertNotNull(obs);
        assertNotNull(obs.getEncounter());
        assertEquals("Encounter/ENC-P1001", obs.getEncounter().getReference());
    }

    @Test
    @DisplayName("20. Multiple OBX values create multiple Observation resources")
    void testMultipleObxValuesCreateMultipleObservations() {
        ParsedHL7Data data = new ParsedHL7Data();
        data.setMessageType("ADT^A01");
        data.setPatient(new ParsedPatient("P1001", "Sharma", "Rahul", "19950510", "M"));

        List<ParsedObservation> obsList = new ArrayList<>();
        obsList.add(new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", "70-100", "F"));
        obsList.add(new ParsedObservation("NM", "HEMOGLOBIN", "13.5", "g/dL", "12-16", "F"));
        obsList.add(new ParsedObservation("ST", "NOTE", "Patient stable", null, null, "F"));
        data.setObservations(obsList);

        FhirMappingResult result = mapper.map(data);

        assertNotNull(result);
        assertEquals(3, result.getObservations().size());
        assertEquals("OBS-P1001-1", result.getObservations().get(0).getIdElement().getIdPart());
        assertEquals("OBS-P1001-2", result.getObservations().get(1).getIdElement().getIdPart());
        assertEquals("OBS-P1001-3", result.getObservations().get(2).getIdElement().getIdPart());
    }

    // =========================================================================
    // INTEGRATION-LEVEL MAPPER TEST + SERIALIZATION
    // =========================================================================

    @Test
    @DisplayName("21. Complete synthetic ADT^A01 mapping and FHIR JSON serialization")
    void testCompleteSyntheticAdtA01MappingAndSerialization() {
        ParsedHL7Data data = new ParsedHL7Data();
        data.setMessageType("ADT^A01");
        data.setMessageControlId("MSG00001");
        data.setVersion("2.5");

        data.setPatient(new ParsedPatient("P1001", "Sharma", "Rahul", "19950510", "M"));
        data.setEncounter(new ParsedEncounter("I", "WARD1", "202610080900", "202610151200"));

        List<ParsedObservation> obsList = new ArrayList<>();
        obsList.add(new ParsedObservation("NM", "GLUCOSE", "120", "mg/dL", "70-100", "F"));
        obsList.add(new ParsedObservation("NM", "HEMOGLOBIN", "13.5", "g/dL", "12-16", "F"));
        data.setObservations(obsList);

        FhirMappingResult result = mapper.map(data);

        // Verify Patient
        assertNotNull(result.getPatient());
        assertEquals("P1001", result.getPatient().getIdElement().getIdPart());
        assertEquals("Sharma", result.getPatient().getNameFirstRep().getFamily());
        assertEquals("Rahul", result.getPatient().getNameFirstRep().getGivenAsSingleString());
        assertEquals("1995-05-10", result.getPatient().getBirthDateElement().getValueAsString());
        assertEquals(Enumerations.AdministrativeGender.MALE, result.getPatient().getGender());

        // Verify Encounter
        assertNotNull(result.getEncounter());
        assertEquals("ENC-P1001", result.getEncounter().getIdElement().getIdPart());
        assertEquals("Patient/P1001", result.getEncounter().getSubject().getReference());
        assertEquals("IMP", result.getEncounter().getClass_().getCode());
        assertEquals("WARD1", result.getEncounter().getLocationFirstRep().getLocation().getDisplay());
        assertNotNull(result.getEncounter().getPeriod().getStart());
        assertNotNull(result.getEncounter().getPeriod().getEnd());

        // Verify Observations
        assertNotNull(result.getObservations());
        assertEquals(2, result.getObservations().size());

        Observation obs1 = result.getObservations().get(0);
        assertEquals("OBS-P1001-1", obs1.getIdElement().getIdPart());
        assertEquals("Patient/P1001", obs1.getSubject().getReference());
        assertEquals("Encounter/ENC-P1001", obs1.getEncounter().getReference());
        assertEquals("GLUCOSE", obs1.getCode().getText());
        assertEquals(new BigDecimal("120"), ((Quantity) obs1.getValue()).getValue());
        assertEquals("mg/dL", ((Quantity) obs1.getValue()).getUnit());
        assertEquals(Observation.ObservationStatus.FINAL, obs1.getStatus());

        Observation obs2 = result.getObservations().get(1);
        assertEquals("OBS-P1001-2", obs2.getIdElement().getIdPart());
        assertEquals("HEMOGLOBIN", obs2.getCode().getText());
        assertEquals(new BigDecimal("13.5"), ((Quantity) obs2.getValue()).getValue());
        assertEquals("g/dL", ((Quantity) obs2.getValue()).getUnit());

        // Verify FHIR JSON Serialization
        String patientJson = serializationService.serializeToJson(result.getPatient());
        assertNotNull(patientJson);
        assertTrue(patientJson.contains("\"resourceType\": \"Patient\""));
        assertTrue(patientJson.contains("\"id\": \"P1001\""));
        assertTrue(patientJson.contains("\"family\": \"Sharma\""));
        assertTrue(patientJson.contains("\"given\": [ \"Rahul\" ]") || patientJson.contains("\"given\": [\n    \"Rahul\"\n  ]"));
        assertTrue(patientJson.contains("\"birthDate\": \"1995-05-10\""));
        assertTrue(patientJson.contains("\"gender\": \"male\""));

        String encounterJson = serializationService.serializeToJson(result.getEncounter());
        assertNotNull(encounterJson);
        assertTrue(encounterJson.contains("\"resourceType\": \"Encounter\""));
        assertTrue(encounterJson.contains("\"reference\": \"Patient/P1001\""));
        assertTrue(encounterJson.contains("\"code\": \"IMP\""));

        String obsJson = serializationService.serializeToJson(obs1);
        assertNotNull(obsJson);
        assertTrue(obsJson.contains("\"resourceType\": \"Observation\""));
        assertTrue(obsJson.contains("\"reference\": \"Patient/P1001\""));
        assertTrue(obsJson.contains("\"value\": 120"));
        assertTrue(obsJson.contains("\"unit\": \"mg/dL\""));
    }
}
