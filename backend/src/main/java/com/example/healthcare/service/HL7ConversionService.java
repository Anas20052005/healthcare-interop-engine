package com.example.healthcare.service;

import com.example.healthcare.dto.hl7.ConversionHistoryResponse;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.dto.hl7.HL7ConvertResponse;
import java.util.List;

/**
 * Service interface for end-to-end HL7 to FHIR conversion orchestration.
 */
public interface HL7ConversionService {

    /**
     * Orchestrates parsing, validation, FHIR R4 mapping, and persistence
     * of an incoming HL7 v2 message.
     *
     * @param request conversion request DTO containing raw HL7 message
     * @return conversion response DTO containing FHIR output and persistence metadata
     */
    HL7ConvertResponse convert(HL7ConvertRequest request);

    /**
     * Retrieves the audit history of previous HL7-to-FHIR conversions.
     *
     * @return list of conversion history records ordered with most recent first
     */
    List<ConversionHistoryResponse> getConversionHistory();

    /**
     * Retrieves a single conversion history record by ID, verifying tenant hospital ownership.
     *
     * @param id conversion record ID
     * @return conversion history DTO
     */
    ConversionHistoryResponse getConversionById(Long id);
}
