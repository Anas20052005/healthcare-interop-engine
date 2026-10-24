package com.example.healthcare.controller;

import com.example.healthcare.service.MockFhirService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MockFhirControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockFhirService mockFhirService;

    private static final String VALID_PATIENT_JSON = """
            {
              "resourceType": "Patient",
              "id": "pat-123",
              "name": [
                {
                  "family": "Smith",
                  "given": ["John"]
                }
              ],
              "gender": "male"
            }
            """;

    private static final String VALID_BUNDLE_JSON = """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "entry": [
                {
                  "resource": {
                    "resourceType": "Patient",
                    "id": "p1",
                    "name": [{"family": "Doe", "given": ["Jane"]}]
                  }
                },
                {
                  "resource": {
                    "resourceType": "Encounter",
                    "id": "e1",
                    "status": "in-progress"
                  }
                },
                {
                  "resource": {
                    "resourceType": "Observation",
                    "id": "o1",
                    "status": "final",
                    "code": {"text": "GLUCOSE"}
                  }
                }
              ]
            }
            """;

    private static final String INVALID_FHIR_JSON = """
            {
              "resourceType": "NotAValidResourceType",
              "broken": true
            }
            """;

    @Test
    @DisplayName("Mock destination accepts valid FHIR Bundle and returns 201 Created with OperationOutcome and headers")
    void testReceiveBundleSuccess() throws Exception {
        mockFhirService.clearReceivedResources();

        mockMvc.perform(post("/api/mock-fhir/Bundle")
                        .contentType("application/fhir+json")
                        .content(VALID_BUNDLE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Mock-Destination", containsString("Local Development Mock FHIR Server")))
                .andExpect(header().string("X-FHIR-Version", "4.0.1"))
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].severity").value("information"))
                .andExpect(jsonPath("$.issue[0].diagnostics").value(containsString("Bundle")));
    }

    @Test
    @DisplayName("Mock destination accepts valid FHIR Patient resource")
    void testReceivePatientSuccess() throws Exception {
        mockMvc.perform(post("/api/mock-fhir/Patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PATIENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].diagnostics").value(containsString("Patient")));
    }

    @Test
    @DisplayName("Mock destination general endpoint accepts valid Bundle")
    void testReceiveGeneralSuccess() throws Exception {
        mockMvc.perform(post("/api/mock-fhir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BUNDLE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"));
    }

    @Test
    @DisplayName("Mock destination rejects invalid FHIR payload with 400 Bad Request")
    void testReceiveInvalidPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/mock-fhir/Bundle")
                        .contentType("application/fhir+json")
                        .content(INVALID_FHIR_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("invalid FHIR R4 format")));
    }

    @Test
    @DisplayName("Received messages can be listed and inspected via GET /api/mock-fhir/received")
    void testInspectReceivedMessages() throws Exception {
        mockFhirService.clearReceivedResources();

        mockMvc.perform(post("/api/mock-fhir/Patient")
                        .contentType("application/fhir+json")
                        .header("Authorization", "Bearer test-mock-token")
                        .content(VALID_PATIENT_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/mock-fhir/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resourceType").value("Patient"))
                .andExpect(jsonPath("$[0].resourceId").value("pat-123"))
                .andExpect(jsonPath("$[0].hasAuthHeader").value(true));
    }

    @Test
    @DisplayName("DELETE /api/mock-fhir/received clears received messages")
    void testClearReceivedMessages() throws Exception {
        mockMvc.perform(post("/api/mock-fhir/Patient")
                        .contentType("application/fhir+json")
                        .content(VALID_PATIENT_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/mock-fhir/received"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/mock-fhir/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
