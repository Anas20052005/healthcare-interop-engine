package com.example.healthcare.service;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FhirSerializationServiceTest {

    private FhirSerializationService serializationService;

    @BeforeEach
    void setUp() {
        serializationService = new FhirSerializationService(FhirContext.forR4());
    }

    @Test
    @DisplayName("Should serialize Patient resource to formatted JSON")
    void testSerializePatient() {
        Patient patient = new Patient();
        patient.setId("P1001");
        patient.addName().setFamily("Sharma").addGiven("Rahul");

        String json = serializationService.serializeToJson(patient);

        assertNotNull(json);
        assertTrue(json.contains("\"resourceType\": \"Patient\""));
        assertTrue(json.contains("\"id\": \"P1001\""));
        assertTrue(json.contains("\"family\": \"Sharma\""));
    }

    @Test
    @DisplayName("Should handle null resource safely")
    void testSerializeNullResource() {
        assertNull(serializationService.serializeToJson(null));
    }

    @Test
    @DisplayName("Should expose valid FhirContext")
    void testGetFhirContext() {
        assertNotNull(serializationService.getFhirContext());
        assertEquals(FhirContext.forR4().getVersion().getVersion(), serializationService.getFhirContext().getVersion().getVersion());
    }
}
