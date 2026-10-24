package com.example.healthcare.service;

import com.example.healthcare.config.RetryProperties;
import com.example.healthcare.entity.DeliveryResultEntity;
import com.example.healthcare.repository.DeliveryResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled background worker that polls for due retries and executes them.
 * Guarantees resilience across application restarts and prevents concurrent execution.
 */
@Component
public class DeliveryRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryRetryScheduler.class);

    private final DeliveryResultRepository deliveryResultRepository;
    private final FhirDeliveryService fhirDeliveryService;
    private final RetryProperties retryProperties;

    public DeliveryRetryScheduler(DeliveryResultRepository deliveryResultRepository,
                                  FhirDeliveryService fhirDeliveryService,
                                  RetryProperties retryProperties) {
        this.deliveryResultRepository = deliveryResultRepository;
        this.fhirDeliveryService = fhirDeliveryService;
        this.retryProperties = retryProperties;
    }

    /**
     * Polls the database for deliveries in RETRYING status whose nextRetryAt has arrived.
     */
    @Scheduled(fixedDelayString = "${app.retry.poll-interval-ms:5000}")
    public void processDueRetries() {
        if (!retryProperties.isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<DeliveryResultEntity> dueDeliveries = deliveryResultRepository.findByStatusAndNextRetryAtLessThanEqual("RETRYING", now);

        if (dueDeliveries.isEmpty()) {
            return;
        }

        logger.debug("Found {} delivery retries due for processing", dueDeliveries.size());

        for (DeliveryResultEntity due : dueDeliveries) {
            try {
                // Atomically claim the delivery record to prevent concurrent workers from processing it
                int claimed = deliveryResultRepository.claimForRetry(due.getId(), now);
                if (claimed == 0) {
                    logger.debug("Delivery ID {} was already claimed by another worker. Skipping.", due.getId());
                    continue;
                }

                logger.info("Claimed due delivery ID {} for retry attempt", due.getId());
                fhirDeliveryService.executeScheduledRetry(due.getId());
            } catch (Exception ex) {
                logger.error("Unexpected error executing due retry for delivery ID {}", due.getId(), ex);
                try {
                    deliveryResultRepository.releaseClaim(due.getId(), "RETRYING", LocalDateTime.now());
                } catch (Exception releaseEx) {
                    logger.error("Failed to release claim for delivery ID {}", due.getId(), releaseEx);
                }
            }
        }
    }
}
