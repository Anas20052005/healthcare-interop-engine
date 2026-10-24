package com.example.healthcare.controller;

import com.example.healthcare.dto.delivery.DeliveryResultResponse;
import com.example.healthcare.dto.delivery.SendConversionRequest;
import com.example.healthcare.dto.hl7.ConversionHistoryResponse;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.dto.hl7.HL7ConvertResponse;
import com.example.healthcare.service.FhirDeliveryService;
import com.example.healthcare.service.HL7ConversionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for HL7 v2 to FHIR R4 conversion operations and destination dispatch.
 * Thin presentation layer delegating all business and orchestration logic
 * to HL7ConversionService and FhirDeliveryService.
 */
@RestController
@RequestMapping("/api/hl7")
public class HL7ConversionController {

    private final HL7ConversionService hl7ConversionService;
    private final FhirDeliveryService fhirDeliveryService;

    public HL7ConversionController(HL7ConversionService hl7ConversionService, FhirDeliveryService fhirDeliveryService) {
        this.hl7ConversionService = hl7ConversionService;
        this.fhirDeliveryService = fhirDeliveryService;
    }

    /**
     * Converts a raw HL7 v2 message into FHIR R4 resources and persists
     * the conversion audit and clinical models to MySQL.
     *
     * @param request conversion request DTO containing the HL7 message
     * @return conversion response DTO containing FHIR output and metadata
     */
    @PostMapping("/convert")
    public ResponseEntity<HL7ConvertResponse> convert(@Valid @RequestBody HL7ConvertRequest request) {
        HL7ConvertResponse response = hl7ConversionService.convert(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the audit history of previous HL7-to-FHIR conversions.
     * Authenticated endpoint requiring a valid Bearer JWT.
     *
     * @return list of conversion history DTO records
     */
    @GetMapping("/history")
    public ResponseEntity<List<ConversionHistoryResponse>> getHistory() {
        List<ConversionHistoryResponse> history = hl7ConversionService.getConversionHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * Retrieves details of a specific conversion by ID, enforcing tenant isolation.
     *
     * @param id conversion record ID
     * @return conversion history DTO
     */
    @GetMapping({"/history/{id}", "/conversions/{id}"})
    public ResponseEntity<ConversionHistoryResponse> getConversionById(@PathVariable Long id) {
        ConversionHistoryResponse response = hl7ConversionService.getConversionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Explicitly dispatches an existing conversion's FHIR output to a selected destination server.
     *
     * @param id conversion record ID
     * @param request delivery request specifying destination ID
     * @return delivery result with immediate status, HTTP code, and safe error summary
     */
    @PostMapping({"/conversions/{id}/send", "/history/{id}/send"})
    public ResponseEntity<DeliveryResultResponse> sendConversion(
            @PathVariable Long id,
            @Valid @RequestBody SendConversionRequest request) {
        DeliveryResultResponse result = fhirDeliveryService.sendConversion(id, request.getDestinationId());
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves previous delivery results for a specific conversion.
     *
     * @param id conversion record ID
     * @return list of delivery result attempts
     */
    @GetMapping({"/conversions/{id}/deliveries", "/history/{id}/deliveries"})
    public ResponseEntity<List<DeliveryResultResponse>> getDeliveries(@PathVariable Long id) {
        List<DeliveryResultResponse> deliveries = fhirDeliveryService.getDeliveriesForConversion(id);
        return ResponseEntity.ok(deliveries);
    }

    /**
     * Retrieves details of a specific delivery attempt by ID, enforcing tenant isolation.
     *
     * @param id delivery attempt record ID
     * @return delivery result DTO
     */
    @GetMapping("/deliveries/{id}")
    public ResponseEntity<DeliveryResultResponse> getDeliveryById(@PathVariable Long id) {
        DeliveryResultResponse delivery = fhirDeliveryService.getDeliveryById(id);
        return ResponseEntity.ok(delivery);
    }

    /**
     * Retries an eligible failed delivery attempt.
     * Enforces hospital tenant isolation and validates delivery state.
     *
     * @param id delivery attempt record ID
     * @return updated DeliveryResultResponse
     */
    @PostMapping("/deliveries/{id}/retry")
    public ResponseEntity<DeliveryResultResponse> retryDelivery(@PathVariable Long id) {
        DeliveryResultResponse result = fhirDeliveryService.retryDelivery(id);
        return ResponseEntity.ok(result);
    }
}
