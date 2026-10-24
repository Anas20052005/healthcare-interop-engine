package com.example.healthcare.service;

import com.example.healthcare.dto.hospital.HospitalCreateRequest;
import com.example.healthcare.dto.hospital.HospitalResponse;
import com.example.healthcare.dto.hospital.HospitalUpdateRequest;
import java.util.List;

/**
 * Service defining hospital management operations.
 */
public interface HospitalService {

    /**
     * Creates a new hospital tenant.
     *
     * @param request hospital creation payload
     * @return response DTO containing persisted hospital details
     */
    HospitalResponse createHospital(HospitalCreateRequest request);

    /**
     * Retrieves all hospitals registered in the system.
     *
     * @return list of hospital details
     */
    List<HospitalResponse> getAllHospitals();

    /**
     * Retrieves hospital details by identifier.
     *
     * @param id hospital identifier
     * @return hospital details response DTO
     */
    HospitalResponse getHospitalById(Long id);

    /**
     * Updates an existing hospital's details.
     *
     * @param id hospital identifier
     * @param request update payload
     * @return updated hospital details response DTO
     */
    HospitalResponse updateHospital(Long id, HospitalUpdateRequest request);
}
