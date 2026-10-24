package com.example.healthcare.controller;

import com.example.healthcare.dto.hospital.HospitalCreateRequest;
import com.example.healthcare.dto.hospital.HospitalResponse;
import com.example.healthcare.dto.hospital.HospitalUpdateRequest;
import com.example.healthcare.service.HospitalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for hospital and clinical tenant management operations.
 */
@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    /**
     * Registers a new hospital tenant.
     * Authorized only for users with ROLE_ADMIN.
     *
     * @param request hospital creation payload
     * @return HTTP 201 with created hospital details
     */
    @PostMapping
    public ResponseEntity<HospitalResponse> createHospital(@Valid @RequestBody HospitalCreateRequest request) {
        HospitalResponse response = hospitalService.createHospital(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all registered hospitals in alphabetical order.
     * Requires authenticated user credentials.
     *
     * @return list of hospital details
     */
    @GetMapping
    public ResponseEntity<List<HospitalResponse>> getAllHospitals() {
        List<HospitalResponse> hospitals = hospitalService.getAllHospitals();
        return ResponseEntity.ok(hospitals);
    }

    /**
     * Retrieves details for a specific hospital by ID.
     * Requires authenticated user credentials.
     *
     * @param id hospital identifier
     * @return hospital details response DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<HospitalResponse> getHospitalById(@PathVariable Long id) {
        HospitalResponse response = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing hospital tenant.
     * Authorized only for users with ROLE_ADMIN.
     *
     * @param id hospital identifier
     * @param request update payload
     * @return updated hospital details response DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<HospitalResponse> updateHospital(
            @PathVariable Long id,
            @Valid @RequestBody HospitalUpdateRequest request) {
        HospitalResponse response = hospitalService.updateHospital(id, request);
        return ResponseEntity.ok(response);
    }
}
