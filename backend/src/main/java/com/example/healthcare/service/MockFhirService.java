package com.example.healthcare.service;

import com.example.healthcare.dto.mockfhir.MockReceivedResource;
import java.util.List;

/**
 * Service contract for managing the local mock FHIR R4 server.
 */
public interface MockFhirService {

    MockReceivedResource processReceivedPayload(String rawPayload, String authHeader, String resourceTypeHint);

    List<MockReceivedResource> getReceivedResources();

    MockReceivedResource getReceivedResourceById(String id);

    void clearReceivedResources();
}
