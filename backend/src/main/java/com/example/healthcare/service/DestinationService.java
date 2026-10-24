package com.example.healthcare.service;

import com.example.healthcare.dto.destination.DestinationCreateRequest;
import com.example.healthcare.dto.destination.DestinationResponse;
import com.example.healthcare.dto.destination.DestinationTestResponse;
import com.example.healthcare.dto.destination.DestinationUpdateRequest;
import java.util.List;

/**
 * Service contract for configuring and managing external and mock FHIR server destinations.
 */
public interface DestinationService {

    List<DestinationResponse> getAccessibleDestinations();

    DestinationResponse getDestinationById(Long id);

    DestinationResponse createDestination(DestinationCreateRequest request);

    DestinationResponse updateDestination(Long id, DestinationUpdateRequest request);

    DestinationResponse updateDestinationStatus(Long id, boolean enabled);

    void deleteDestination(Long id);

    /**
     * Probes the destination's FHIR metadata endpoint ({baseUrl}/metadata) to verify
     * connectivity and FHIR compliance. No patient data is sent.
     *
     * @param id destination ID
     * @return test result with latency, HTTP status, and server capability details
     */
    DestinationTestResponse testConnection(Long id);
}
