package com.example.healthcare.service;

import com.example.healthcare.dto.delivery.DeliveryResultResponse;
import java.util.List;

/**
 * Service contract for sending converted FHIR R4 outputs to destinations,
 * retrieving delivery history, and executing bounded automated/manual retries.
 */
public interface FhirDeliveryService {

    DeliveryResultResponse sendConversion(Long conversionId, Long destinationId);

    List<DeliveryResultResponse> getDeliveriesForConversion(Long conversionId);

    List<DeliveryResultResponse> getDeliveriesForDestination(Long destinationId);

    DeliveryResultResponse getDeliveryById(Long deliveryId);

    /**
     * Triggers a manual retry of an eligible failed delivery attempt, enforcing tenant isolation.
     *
     * @param deliveryId the ID of the failed delivery record to retry
     * @return updated DeliveryResultResponse
     */
    DeliveryResultResponse retryDelivery(Long deliveryId);

    /**
     * Executes a scheduled retry attempt triggered by the background retry worker.
     *
     * @param deliveryId the ID of the delivery record to retry
     */
    void executeScheduledRetry(Long deliveryId);
}
