package com.example.healthcare.controller;

import com.example.healthcare.dto.delivery.DeliveryResultResponse;
import com.example.healthcare.dto.destination.DestinationCreateRequest;
import com.example.healthcare.dto.destination.DestinationResponse;
import com.example.healthcare.dto.destination.DestinationStatusRequest;
import com.example.healthcare.dto.destination.DestinationTestResponse;
import com.example.healthcare.dto.destination.DestinationUpdateRequest;
import com.example.healthcare.service.DestinationService;
import com.example.healthcare.service.FhirDeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for external and mock FHIR destination configuration.
 */
@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationService destinationService;
    private final FhirDeliveryService fhirDeliveryService;

    public DestinationController(DestinationService destinationService, FhirDeliveryService fhirDeliveryService) {
        this.destinationService = destinationService;
        this.fhirDeliveryService = fhirDeliveryService;
    }

    /**
     * Lists all destinations accessible to the authenticated caller
     * (hospital-owned + platform-wide destinations).
     */
    @GetMapping
    public ResponseEntity<List<DestinationResponse>> getDestinations() {
        List<DestinationResponse> destinations = destinationService.getAccessibleDestinations();
        return ResponseEntity.ok(destinations);
    }

    /**
     * Retrieves destination configuration details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DestinationResponse> getDestinationById(@PathVariable Long id) {
        DestinationResponse destination = destinationService.getDestinationById(id);
        return ResponseEntity.ok(destination);
    }

    /**
     * Creates a new destination configuration.
     */
    @PostMapping
    public ResponseEntity<DestinationResponse> createDestination(@Valid @RequestBody DestinationCreateRequest request) {
        DestinationResponse created = destinationService.createDestination(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing destination configuration.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DestinationResponse> updateDestination(
            @PathVariable Long id,
            @Valid @RequestBody DestinationUpdateRequest request) {
        DestinationResponse updated = destinationService.updateDestination(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Toggles or updates the enabled status of a destination.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<DestinationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody DestinationStatusRequest request) {
        DestinationResponse updated = destinationService.updateDestinationStatus(id, request.getEnabled());
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a destination configuration.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDestination(@PathVariable Long id) {
        destinationService.deleteDestination(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves previous delivery attempts for this destination.
     */
    @GetMapping("/{id}/deliveries")
    public ResponseEntity<List<DeliveryResultResponse>> getDeliveries(@PathVariable Long id) {
        List<DeliveryResultResponse> deliveries = fhirDeliveryService.getDeliveriesForDestination(id);
        return ResponseEntity.ok(deliveries);
    }

    /**
     * Tests connectivity to the destination's FHIR metadata endpoint ({baseUrl}/metadata).
     * No patient data is sent. Enforces the same tenant isolation as all other endpoints.
     */
    @GetMapping("/{id}/test")
    public ResponseEntity<DestinationTestResponse> testConnection(@PathVariable Long id) {
        DestinationTestResponse result = destinationService.testConnection(id);
        return ResponseEntity.ok(result);
    }
}
