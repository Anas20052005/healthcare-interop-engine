package com.example.healthcare.service;

import ca.uhn.fhir.context.FhirContext;
import com.example.healthcare.dto.mockfhir.MockReceivedResource;
import com.example.healthcare.exception.InvalidFhirPayloadException;
import com.example.healthcare.exception.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Default implementation of {@link MockFhirService}.
 * Validates incoming payloads using HAPI FHIR R4 and retains a thread-safe
 * buffer of received messages for development and testing inspection.
 */
@Service
public class DefaultMockFhirService implements MockFhirService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMockFhirService.class);
    private static final int MAX_BUFFER_SIZE = 100;

    private final FhirContext fhirContext;
    private final ConcurrentLinkedDeque<MockReceivedResource> receivedBuffer = new ConcurrentLinkedDeque<>();

    public DefaultMockFhirService(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @Override
    public MockReceivedResource processReceivedPayload(String rawPayload, String authHeader, String resourceTypeHint) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new InvalidFhirPayloadException("FHIR payload cannot be empty or blank");
        }

        IBaseResource parsedResource;
        try {
            parsedResource = fhirContext.newJsonParser().parseResource(rawPayload.trim());
        } catch (Exception e) {
            logger.warn("Mock FHIR destination rejected invalid payload: {}", e.getMessage());
            throw new InvalidFhirPayloadException("Payload validation failed: invalid FHIR R4 format - " + e.getMessage());
        }

        if (parsedResource == null) {
            throw new InvalidFhirPayloadException("Failed to parse FHIR resource from payload");
        }

        String actualType = parsedResource.fhirType();
        String resourceId = parsedResource.getIdElement() != null ? parsedResource.getIdElement().getIdPart() : null;
        Integer entryCount = null;
        String summary;

        if (parsedResource instanceof Bundle bundle) {
            entryCount = bundle.getEntry() != null ? bundle.getEntry().size() : 0;
            List<String> types = new ArrayList<>();
            if (bundle.getEntry() != null) {
                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    if (entry.getResource() != null) {
                        types.add(entry.getResource().fhirType());
                    }
                }
            }
            summary = String.format("Bundle type [%s] with %d entries (%s)",
                    bundle.getType() != null ? bundle.getType().toCode() : "unknown",
                    entryCount,
                    String.join(", ", types));
        } else if (parsedResource instanceof Patient patient) {
            String name = patient.getNameFirstRep() != null ? patient.getNameFirstRep().getNameAsSingleString() : "Unknown";
            String gender = patient.getGender() != null ? patient.getGender().toCode() : "unknown";
            summary = String.format("Patient [ID: %s, Name: %s, Gender: %s]", resourceId, name, gender);
        } else if (parsedResource instanceof Encounter encounter) {
            String status = encounter.getStatus() != null ? encounter.getStatus().toCode() : "unknown";
            summary = String.format("Encounter [ID: %s, Status: %s]", resourceId, status);
        } else if (parsedResource instanceof Observation obs) {
            String code = obs.getCode() != null && obs.getCode().getText() != null
                    ? obs.getCode().getText()
                    : (obs.getCode() != null && !obs.getCode().getCoding().isEmpty() ? obs.getCode().getCodingFirstRep().getCode() : "unknown");
            summary = String.format("Observation [ID: %s, Code: %s]", resourceId, code);
        } else {
            summary = String.format("%s resource [ID: %s]", actualType, resourceId != null ? resourceId : "N/A");
        }

        boolean hasAuth = (authHeader != null && !authHeader.isBlank());
        String trackingId = UUID.randomUUID().toString();

        MockReceivedResource record = new MockReceivedResource(
                trackingId,
                actualType,
                resourceId,
                entryCount,
                summary,
                LocalDateTime.now(),
                rawPayload,
                hasAuth
        );

        // Maintain buffer size
        receivedBuffer.addFirst(record);
        while (receivedBuffer.size() > MAX_BUFFER_SIZE) {
            receivedBuffer.removeLast();
        }

        logger.info("Mock FHIR destination received valid {} resource (Tracking ID: {}). Summary: {}",
                actualType, trackingId, summary);

        return record;
    }

    @Override
    public List<MockReceivedResource> getReceivedResources() {
        return new ArrayList<>(receivedBuffer);
    }

    @Override
    public MockReceivedResource getReceivedResourceById(String id) {
        return receivedBuffer.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No mock received resource found with id: " + id));
    }

    @Override
    public void clearReceivedResources() {
        receivedBuffer.clear();
        logger.info("Mock FHIR received resources buffer cleared");
    }
}
