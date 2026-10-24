package com.example.healthcare.controller;

import ca.uhn.fhir.context.FhirContext;
import com.example.healthcare.dto.mockfhir.MockReceivedResource;
import com.example.healthcare.service.MockFhirService;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller providing the local mock FHIR R4 server destination for development and testing.
 * Accepts FHIR R4 resources or Bundles, validates them using HAPI FHIR,
 * records received messages for inspection, and returns compliant FHIR responses.
 */
@RestController
@RequestMapping("/api/mock-fhir")
public class MockFhirController {

    private final MockFhirService mockFhirService;
    private final FhirContext fhirContext;

    public MockFhirController(MockFhirService mockFhirService, FhirContext fhirContext) {
        this.mockFhirService = mockFhirService;
        this.fhirContext = fhirContext;
    }

    /**
     * Universal FHIR R4 ingest endpoint accepting Bundles or individual resources.
     */
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, "application/fhir+json", MediaType.ALL_VALUE})
    public ResponseEntity<String> receiveGeneral(
            @RequestBody String payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return handleIncomingPayload(payload, authHeader, null);
    }

    /**
     * Endpoint specifically accepting FHIR R4 Bundle resources.
     */
    @PostMapping(value = "/Bundle", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/fhir+json", MediaType.ALL_VALUE})
    public ResponseEntity<String> receiveBundle(
            @RequestBody String payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return handleIncomingPayload(payload, authHeader, "Bundle");
    }

    /**
     * Endpoint accepting individual FHIR R4 resources (Patient, Encounter, Observation, etc.).
     */
    @PostMapping(value = "/{resourceType}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/fhir+json", MediaType.ALL_VALUE})
    public ResponseEntity<String> receiveResource(
            @PathVariable String resourceType,
            @RequestBody String payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return handleIncomingPayload(payload, authHeader, resourceType);
    }

    /**
     * Retrieves the list of received test resources for developer inspection.
     */
    @GetMapping("/received")
    public ResponseEntity<List<MockReceivedResource>> getReceivedResources() {
        List<MockReceivedResource> resources = mockFhirService.getReceivedResources();
        return ResponseEntity.ok(resources);
    }

    /**
     * Inspects a specific received message by tracking ID, including raw payload.
     */
    @GetMapping("/received/{id}")
    public ResponseEntity<MockReceivedResource> getReceivedResourceById(@PathVariable String id) {
        MockReceivedResource resource = mockFhirService.getReceivedResourceById(id);
        return ResponseEntity.ok(resource);
    }

    /**
     * Clears the received messages buffer.
     */
    @DeleteMapping("/received")
    public ResponseEntity<Void> clearReceivedResources() {
        mockFhirService.clearReceivedResources();
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<String> handleIncomingPayload(String payload, String authHeader, String resourceTypeHint) {
        MockReceivedResource received = mockFhirService.processReceivedPayload(payload, authHeader, resourceTypeHint);

        // Build FHIR OperationOutcome identifying this as a mock test destination
        OperationOutcome outcome = new OperationOutcome();
        OperationOutcome.OperationOutcomeIssueComponent issue = outcome.addIssue();
        issue.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
        issue.setCode(OperationOutcome.IssueType.INFORMATIONAL);
        issue.setDiagnostics(String.format(
                "Mock FHIR Server (Development/Testing Destination - Not for Production Use): " +
                "Successfully received and validated FHIR R4 %s. Tracking ID: %s. %s",
                received.getResourceType(),
                received.getId(),
                received.getSummary()
        ));

        String serializedOutcome = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(outcome);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Mock-Destination", "Local Development Mock FHIR Server - Not for Production Use");
        headers.set("X-FHIR-Version", "4.0.1");
        headers.setContentType(MediaType.valueOf("application/fhir+json;charset=UTF-8"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(headers)
                .body(serializedOutcome);
    }
}
