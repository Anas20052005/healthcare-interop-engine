package com.example.healthcare.service;

import ca.uhn.fhir.context.FhirContext;
import com.example.healthcare.config.RetryProperties;
import com.example.healthcare.dto.delivery.DeliveryResultResponse;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.DeliveryResultEntity;
import com.example.healthcare.entity.DestinationAuthType;
import com.example.healthcare.entity.DestinationEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.exception.DeliveryRetryNotAllowedException;
import com.example.healthcare.exception.DestinationNotFoundException;
import com.example.healthcare.exception.InvalidDestinationException;
import com.example.healthcare.exception.ResourceNotFoundException;
import com.example.healthcare.exception.TenantAccessDeniedException;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.DeliveryResultRepository;
import com.example.healthcare.repository.DestinationRepository;
import com.example.healthcare.security.TenantSecurityService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link FhirDeliveryService}.
 * Dispatches converted FHIR outputs to external or mock destinations.
 * Provides bounded automatic retry on transient failures and secure manual retry capabilities.
 * Keeps external HTTP calls strictly outside database transactions.
 */
@Service
public class DefaultFhirDeliveryService implements FhirDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFhirDeliveryService.class);

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    private final ConversionRepository conversionRepository;
    private final DestinationRepository destinationRepository;
    private final DeliveryResultRepository deliveryResultRepository;
    private final TenantSecurityService tenantSecurityService;
    private final SsrfProtectionService ssrfProtectionService;
    private final TokenCryptoService tokenCryptoService;
    private final MockFhirService mockFhirService;
    private final FhirContext fhirContext;
    private final RestTemplate restTemplate;
    private final AuditService auditService;
    private final RetryPolicyService retryPolicyService;

    @Autowired
    public DefaultFhirDeliveryService(ConversionRepository conversionRepository,
                                      DestinationRepository destinationRepository,
                                      DeliveryResultRepository deliveryResultRepository,
                                      TenantSecurityService tenantSecurityService,
                                      SsrfProtectionService ssrfProtectionService,
                                      TokenCryptoService tokenCryptoService,
                                      MockFhirService mockFhirService,
                                      FhirContext fhirContext,
                                      AuditService auditService,
                                      RetryPolicyService retryPolicyService) {
        this.conversionRepository = conversionRepository;
        this.destinationRepository = destinationRepository;
        this.deliveryResultRepository = deliveryResultRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.ssrfProtectionService = ssrfProtectionService;
        this.tokenCryptoService = tokenCryptoService;
        this.mockFhirService = mockFhirService;
        this.fhirContext = fhirContext;
        this.auditService = auditService;
        this.retryPolicyService = retryPolicyService != null ? retryPolicyService : new RetryPolicyService(new RetryProperties());

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    public DefaultFhirDeliveryService(ConversionRepository conversionRepository,
                                      DestinationRepository destinationRepository,
                                      DeliveryResultRepository deliveryResultRepository,
                                      TenantSecurityService tenantSecurityService,
                                      SsrfProtectionService ssrfProtectionService,
                                      TokenCryptoService tokenCryptoService,
                                      MockFhirService mockFhirService,
                                      FhirContext fhirContext,
                                      AuditService auditService) {
        this(conversionRepository, destinationRepository, deliveryResultRepository,
                tenantSecurityService, ssrfProtectionService, tokenCryptoService,
                mockFhirService, fhirContext, auditService, new RetryPolicyService(new RetryProperties()));
    }

    public DefaultFhirDeliveryService(ConversionRepository conversionRepository,
                                      DestinationRepository destinationRepository,
                                      DeliveryResultRepository deliveryResultRepository,
                                      TenantSecurityService tenantSecurityService,
                                      SsrfProtectionService ssrfProtectionService,
                                      TokenCryptoService tokenCryptoService,
                                      MockFhirService mockFhirService,
                                      FhirContext fhirContext) {
        this(conversionRepository, destinationRepository, deliveryResultRepository,
                tenantSecurityService, ssrfProtectionService, tokenCryptoService,
                mockFhirService, fhirContext, null, new RetryPolicyService(new RetryProperties()));
    }

    // Constructor overload for testing with custom RestTemplate
    public DefaultFhirDeliveryService(ConversionRepository conversionRepository,
                                      DestinationRepository destinationRepository,
                                      DeliveryResultRepository deliveryResultRepository,
                                      TenantSecurityService tenantSecurityService,
                                      SsrfProtectionService ssrfProtectionService,
                                      TokenCryptoService tokenCryptoService,
                                      MockFhirService mockFhirService,
                                      FhirContext fhirContext,
                                      RestTemplate restTemplate) {
        this.conversionRepository = conversionRepository;
        this.destinationRepository = destinationRepository;
        this.deliveryResultRepository = deliveryResultRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.ssrfProtectionService = ssrfProtectionService;
        this.tokenCryptoService = tokenCryptoService;
        this.mockFhirService = mockFhirService;
        this.fhirContext = fhirContext;
        this.restTemplate = restTemplate;
        this.auditService = null;
        this.retryPolicyService = new RetryPolicyService(new RetryProperties());
    }

    public DefaultFhirDeliveryService(ConversionRepository conversionRepository,
                                      DestinationRepository destinationRepository,
                                      DeliveryResultRepository deliveryResultRepository,
                                      TenantSecurityService tenantSecurityService,
                                      SsrfProtectionService ssrfProtectionService,
                                      TokenCryptoService tokenCryptoService,
                                      MockFhirService mockFhirService,
                                      FhirContext fhirContext,
                                      RestTemplate restTemplate,
                                      AuditService auditService,
                                      RetryPolicyService retryPolicyService) {
        this.conversionRepository = conversionRepository;
        this.destinationRepository = destinationRepository;
        this.deliveryResultRepository = deliveryResultRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.ssrfProtectionService = ssrfProtectionService;
        this.tokenCryptoService = tokenCryptoService;
        this.mockFhirService = mockFhirService;
        this.fhirContext = fhirContext;
        this.restTemplate = restTemplate;
        this.auditService = auditService;
        this.retryPolicyService = retryPolicyService != null ? retryPolicyService : new RetryPolicyService(new RetryProperties());
    }

    @Override
    @Transactional
    public DeliveryResultResponse sendConversion(Long conversionId, Long destinationId) {
        logger.info("Initiating explicit FHIR send: Conversion ID {}, Destination ID {}", conversionId, destinationId);

        // Step 1: Resolve context and validate permissions (read-only)
        DeliveryContext context = prepareDeliveryContext(conversionId, destinationId);

        // Step 2: Validate FHIR payload before sending
        String validationError = validatePayload(context.fhirPayload());
        if (validationError != null) {
            logger.warn("FHIR payload validation failed before sending for conversion {}: {}", conversionId, validationError);
            DeliveryResultEntity failedResult = persistDeliveryResult(
                    conversionId,
                    destinationId,
                    context.destinationName(),
                    "FAILED",
                    null,
                    "Payload validation failed: " + validationError,
                    null,
                    1,
                    retryPolicyService.getMaxAttempts(),
                    null,
                    context.hospital()
            );
            return mapToResponse(failedResult);
        }

        // Step 3: Execute external HTTP request OUTSIDE database transaction
        DeliveryOutcome outcome = executeSend(context);

        // Step 4: Evaluate retry policy for transient failures
        int maxAttempts = retryPolicyService.getMaxAttempts();
        String initialStatus = outcome.status();
        LocalDateTime nextRetryAt = null;

        if ("FAILED".equalsIgnoreCase(outcome.status())) {
            boolean isTransient = retryPolicyService.isTransientError(outcome.httpStatusCode(), outcome.throwable());
            boolean canRetry = retryPolicyService.isRetryEnabled() && isTransient && 1 < maxAttempts;

            if (canRetry) {
                initialStatus = "RETRYING";
                nextRetryAt = retryPolicyService.calculateNextRetryTime(1, outcome.responseHeaders());
                logger.info("Transient failure detected for destination '{}' (HTTP {}). Scheduling automatic retry attempt 2 at {}",
                        context.destinationName(), outcome.httpStatusCode(), nextRetryAt);
            }
        }

        // Step 5: Persist delivery result in transaction
        DeliveryResultEntity savedResult = persistDeliveryResult(
                conversionId,
                destinationId,
                context.destinationName(),
                initialStatus,
                outcome.httpStatusCode(),
                outcome.errorMessage(),
                outcome.responseSummary(),
                1,
                maxAttempts,
                nextRetryAt,
                context.hospital()
        );

        logger.info("Delivery result recorded: ID {}, Status {}, HTTP Code {}, Next Retry: {}",
                savedResult.getId(), savedResult.getStatus(), savedResult.getHttpStatusCode(), savedResult.getNextRetryAt());

        // Step 6: Audit logging
        if (auditService != null) {
            if ("SUCCESS".equalsIgnoreCase(savedResult.getStatus())) {
                auditService.log(
                        "FHIR_DELIVERY_SUCCESS",
                        "DELIVERY",
                        String.valueOf(savedResult.getId()),
                        "SUCCESS",
                        String.format("FHIR delivery to destination '%s' succeeded (HTTP %s): %s",
                                context.destinationName(),
                                savedResult.getHttpStatusCode() != null ? savedResult.getHttpStatusCode() : "200",
                                savedResult.getResponseSummary() != null ? savedResult.getResponseSummary() : "Accepted"),
                        context.hospital()
                );
            } else if ("RETRYING".equalsIgnoreCase(savedResult.getStatus())) {
                auditService.log(
                        "RETRY_SCHEDULED",
                        "DELIVERY",
                        String.valueOf(savedResult.getId()),
                        "FAILURE",
                        String.format("Automatic retry scheduled for destination '%s' (Attempt 1 of %d failed: %s). Next attempt at %s",
                                context.destinationName(),
                                maxAttempts,
                                savedResult.getErrorMessage(),
                                nextRetryAt),
                        context.hospital()
                );
            } else {
                auditService.log(
                        "FHIR_DELIVERY_FAILURE",
                        "DELIVERY",
                        String.valueOf(savedResult.getId()),
                        "FAILURE",
                        String.format("FHIR delivery to destination '%s' failed (HTTP %s): %s",
                                context.destinationName(),
                                savedResult.getHttpStatusCode() != null ? savedResult.getHttpStatusCode() : "N/A",
                                savedResult.getErrorMessage() != null ? savedResult.getErrorMessage() : "Unknown error"),
                        context.hospital()
                );
            }
        }

        return mapToResponse(savedResult);
    }

    @Override
    @Transactional
    public DeliveryResultResponse retryDelivery(Long deliveryId) {
        logger.info("Manual delivery retry requested for Delivery ID: {}", deliveryId);

        // Step 1: Look up delivery record
        DeliveryResultEntity delivery = deliveryResultRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery record not found with id: " + deliveryId));

        // Step 2: Enforce tenant isolation on hospital
        if (delivery.getHospital() != null) {
            tenantSecurityService.validateResourceHospital(delivery.getHospital());
        }

        // Step 3: Validate retry eligibility
        if ("SUCCESS".equalsIgnoreCase(delivery.getStatus())) {
            throw new DeliveryRetryNotAllowedException(
                    "Cannot retry delivery #" + deliveryId + ": delivery already succeeded with HTTP " + delivery.getHttpStatusCode() + ".");
        }

        if ("RETRYING".equalsIgnoreCase(delivery.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(delivery.getStatus())) {
            throw new DeliveryRetryNotAllowedException(
                    "Cannot retry delivery #" + deliveryId + ": a retry is already scheduled or in progress.");
        }

        DestinationEntity destination = destinationRepository.findById(delivery.getDestinationId())
                .orElseThrow(() -> new DestinationNotFoundException("Destination not found with id: " + delivery.getDestinationId()));

        if (!destination.isEnabled()) {
            throw new InvalidDestinationException("Destination '" + destination.getName() + "' is disabled and cannot receive messages.");
        }

        // Step 4: Atomically claim the delivery record for manual retry
        int claimed = deliveryResultRepository.claimForManualRetry(delivery.getId(), LocalDateTime.now());
        if (claimed == 0) {
            throw new DeliveryRetryNotAllowedException("Delivery #" + deliveryId + " is currently being processed by another worker.");
        }

        // Step 5: Audit manual retry initiation
        if (auditService != null) {
            auditService.log(
                    "MANUAL_RETRY_REQUESTED",
                    "DELIVERY",
                    String.valueOf(delivery.getId()),
                    "SUCCESS",
                    String.format("Manual retry requested by user for delivery #%d to destination '%s'",
                            delivery.getId(), delivery.getDestinationName()),
                    delivery.getHospital()
            );
        }

        // Step 6: Prepare context and execute send OUTSIDE database transaction
        DeliveryContext context = prepareDeliveryContext(delivery.getConversionId(), delivery.getDestinationId());
        int attemptCount = (delivery.getAttemptCount() != null ? delivery.getAttemptCount() : 1) + 1;
        int maxAttempts = Math.max(delivery.getMaxAttempts() != null ? delivery.getMaxAttempts() : 3, attemptCount);

        DeliveryOutcome outcome = executeSend(context);

        // Step 7: Update and persist results
        delivery.setAttemptCount(attemptCount);
        delivery.setMaxAttempts(maxAttempts);
        delivery.setAttemptTimestamp(LocalDateTime.now());
        delivery.setHttpStatusCode(outcome.httpStatusCode());
        delivery.setResponseSummary(outcome.responseSummary());

        if ("SUCCESS".equalsIgnoreCase(outcome.status())) {
            delivery.setStatus("SUCCESS");
            delivery.setErrorMessage(null);
            delivery.setNextRetryAt(null);
        } else {
            boolean isTransient = retryPolicyService.isTransientError(outcome.httpStatusCode(), outcome.throwable());
            boolean canRetryAgain = retryPolicyService.isRetryEnabled() && isTransient && attemptCount < maxAttempts;

            delivery.setErrorMessage(outcome.errorMessage());
            if (canRetryAgain) {
                delivery.setStatus("RETRYING");
                delivery.setNextRetryAt(retryPolicyService.calculateNextRetryTime(attemptCount, outcome.responseHeaders()));
            } else {
                delivery.setStatus("FAILED");
                delivery.setNextRetryAt(null);
            }
        }

        DeliveryResultEntity saved = deliveryResultRepository.save(delivery);

        // Step 8: Audit retry completion
        if (auditService != null) {
            String auditAction = "SUCCESS".equalsIgnoreCase(saved.getStatus()) ? "RETRY_SUCCESS" :
                    ("RETRYING".equalsIgnoreCase(saved.getStatus()) ? "RETRY_SCHEDULED" : "RETRY_FAILURE");
            String auditOutcome = "SUCCESS".equalsIgnoreCase(saved.getStatus()) ? "SUCCESS" : "FAILURE";
            String details = String.format("Manual retry attempt %d of %d for destination '%s': %s (HTTP %s)",
                    attemptCount, maxAttempts, context.destinationName(),
                    saved.getStatus(), saved.getHttpStatusCode() != null ? saved.getHttpStatusCode() : "N/A");

            auditService.log(auditAction, "DELIVERY", String.valueOf(saved.getId()), auditOutcome, details, saved.getHospital());
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void executeScheduledRetry(Long deliveryId) {
        DeliveryResultEntity delivery = deliveryResultRepository.findById(deliveryId).orElse(null);
        if (delivery == null || !"IN_PROGRESS".equalsIgnoreCase(delivery.getStatus())) {
            return;
        }

        try {
            DeliveryContext context = prepareDeliveryContextForScheduledRetry(delivery.getConversionId(), delivery.getDestinationId());
            int attemptCount = (delivery.getAttemptCount() != null ? delivery.getAttemptCount() : 1) + 1;
            int maxAttempts = delivery.getMaxAttempts() != null ? delivery.getMaxAttempts() : 3;

            logger.info("Executing scheduled retry: Delivery ID {}, Attempt {}/{}", deliveryId, attemptCount, maxAttempts);

            if (auditService != null) {
                auditService.log(
                        "RETRY_ATTEMPT",
                        "DELIVERY",
                        String.valueOf(delivery.getId()),
                        "SUCCESS",
                        String.format("Executing scheduled retry attempt %d of %d for destination '%s'",
                                attemptCount, maxAttempts, context.destinationName()),
                        delivery.getHospital()
                );
            }

            DeliveryOutcome outcome = executeSend(context);

            delivery.setAttemptCount(attemptCount);
            delivery.setAttemptTimestamp(LocalDateTime.now());
            delivery.setHttpStatusCode(outcome.httpStatusCode());
            delivery.setResponseSummary(outcome.responseSummary());

            if ("SUCCESS".equalsIgnoreCase(outcome.status())) {
                delivery.setStatus("SUCCESS");
                delivery.setErrorMessage(null);
                delivery.setNextRetryAt(null);
                deliveryResultRepository.save(delivery);

                if (auditService != null) {
                    auditService.log(
                            "RETRY_SUCCESS",
                            "DELIVERY",
                            String.valueOf(delivery.getId()),
                            "SUCCESS",
                            String.format("Scheduled retry attempt %d succeeded for destination '%s' (HTTP %s)",
                                    attemptCount, context.destinationName(), outcome.httpStatusCode()),
                            delivery.getHospital()
                    );
                }
            } else {
                boolean isTransient = retryPolicyService.isTransientError(outcome.httpStatusCode(), outcome.throwable());
                boolean canRetryAgain = retryPolicyService.isRetryEnabled() && isTransient && attemptCount < maxAttempts;

                delivery.setErrorMessage(outcome.errorMessage());

                if (canRetryAgain) {
                    LocalDateTime nextRetry = retryPolicyService.calculateNextRetryTime(attemptCount, outcome.responseHeaders());
                    delivery.setStatus("RETRYING");
                    delivery.setNextRetryAt(nextRetry);
                    deliveryResultRepository.save(delivery);

                    if (auditService != null) {
                        auditService.log(
                                "RETRY_SCHEDULED",
                                "DELIVERY",
                                String.valueOf(delivery.getId()),
                                "FAILURE",
                                String.format("Scheduled retry attempt %d failed (HTTP %s). Rescheduled next attempt at %s",
                                        attemptCount, outcome.httpStatusCode(), nextRetry),
                                delivery.getHospital()
                        );
                    }
                } else {
                    delivery.setStatus("FAILED");
                    delivery.setNextRetryAt(null);
                    deliveryResultRepository.save(delivery);

                    if (auditService != null) {
                        auditService.log(
                                "RETRY_FAILURE",
                                "DELIVERY",
                                String.valueOf(delivery.getId()),
                                "FAILURE",
                                String.format("Scheduled retry attempts exhausted (%d/%d) for destination '%s': %s",
                                        attemptCount, maxAttempts, context.destinationName(), outcome.errorMessage()),
                                delivery.getHospital()
                        );
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to complete scheduled retry for Delivery ID {}", deliveryId, ex);
            delivery.setStatus("FAILED");
            delivery.setErrorMessage("Internal error during scheduled retry execution: " + sanitizeErrorMessage(ex.getMessage()));
            delivery.setNextRetryAt(null);
            deliveryResultRepository.save(delivery);
        }
    }

    @Transactional(readOnly = true)
    protected DeliveryContext prepareDeliveryContext(Long conversionId, Long destinationId) {
        ConversionEntity conversion = conversionRepository.findById(conversionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversion record not found with id: " + conversionId));

        // Enforce tenant isolation on conversion record
        tenantSecurityService.validateResourceHospital(conversion.getHospital());

        DestinationEntity destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new DestinationNotFoundException("Destination not found with id: " + destinationId));

        // Enforce tenant isolation on destination: if hospital-specific, user must belong to that hospital
        if (destination.getHospital() != null) {
            tenantSecurityService.validateResourceHospital(destination.getHospital());
        }

        if (!destination.isEnabled()) {
            throw new InvalidDestinationException("Destination '" + destination.getName() + "' is disabled and cannot receive messages");
        }

        // SSRF URL validation
        ssrfProtectionService.validateDestinationUrl(destination.getBaseUrl(), destination.isMock());

        String fhirPayload = conversion.getFhirPayload();
        if (fhirPayload == null || fhirPayload.isBlank()) {
            throw new InvalidDestinationException("Conversion record " + conversionId + " does not contain a stored FHIR payload to send");
        }

        String decryptedToken = null;
        if (destination.getAuthType() == DestinationAuthType.BEARER_TOKEN && destination.getAuthToken() != null) {
            decryptedToken = tokenCryptoService.decrypt(destination.getAuthToken());
        }

        return new DeliveryContext(
                conversionId,
                destinationId,
                destination.getName(),
                destination.getBaseUrl().trim(),
                destination.getAuthType(),
                decryptedToken,
                fhirPayload,
                conversion.getHospital(),
                destination.isMock()
        );
    }

    @Transactional(readOnly = true)
    protected DeliveryContext prepareDeliveryContextForScheduledRetry(Long conversionId, Long destinationId) {
        ConversionEntity conversion = conversionRepository.findById(conversionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversion record not found with id: " + conversionId));

        DestinationEntity destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new DestinationNotFoundException("Destination not found with id: " + destinationId));

        if (!destination.isEnabled()) {
            throw new InvalidDestinationException("Destination '" + destination.getName() + "' is disabled and cannot receive messages");
        }

        // SSRF URL validation
        ssrfProtectionService.validateDestinationUrl(destination.getBaseUrl(), destination.isMock());

        String fhirPayload = conversion.getFhirPayload();
        if (fhirPayload == null || fhirPayload.isBlank()) {
            throw new InvalidDestinationException("Conversion record " + conversionId + " does not contain a stored FHIR payload to send");
        }

        String decryptedToken = null;
        if (destination.getAuthType() == DestinationAuthType.BEARER_TOKEN && destination.getAuthToken() != null) {
            decryptedToken = tokenCryptoService.decrypt(destination.getAuthToken());
        }

        return new DeliveryContext(
                conversionId,
                destinationId,
                destination.getName(),
                destination.getBaseUrl().trim(),
                destination.getAuthType(),
                decryptedToken,
                fhirPayload,
                conversion.getHospital(),
                destination.isMock()
        );
    }

    private String validatePayload(String fhirPayload) {
        try {
            IBaseResource resource = fhirContext.newJsonParser().parseResource(fhirPayload);
            if (resource == null) {
                return "Parsed FHIR resource is null";
            }
            return null; // Valid
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private DeliveryOutcome executeSend(DeliveryContext context) {
        if (context.isMock()) {
            try {
                String authHeader = context.authToken() != null ? "Bearer " + context.authToken() : null;
                IBaseResource resource = fhirContext.newJsonParser().parseResource(context.fhirPayload());
                String resourceType = resource != null && resource.fhirType() != null ? resource.fhirType() : "Bundle";
                var mockRecord = mockFhirService.processReceivedPayload(context.fhirPayload(), authHeader, resourceType);
                String responseSummary = "Mock FHIR server: accepted " + resourceType +
                        (mockRecord.getEntryCount() != null ? " (" + mockRecord.getEntryCount() + " entries)" : "");
                return new DeliveryOutcome("SUCCESS", 201, null, responseSummary, null, null);
            } catch (Exception ex) {
                logger.error("Mock FHIR server reception failed", ex);
                return new DeliveryOutcome("FAILED", 400, "Mock server processing error: " + ex.getMessage(), null, null, ex);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/fhir+json;charset=UTF-8"));
        headers.setAccept(Collections.singletonList(MediaType.valueOf("application/fhir+json")));

        if (context.authType() == DestinationAuthType.BEARER_TOKEN && context.authToken() != null && !context.authToken().isBlank()) {
            headers.setBearerAuth(context.authToken());
        }

        String payloadToSend = preparePayloadForDestination(context.fhirPayload(), context.targetUrl());
        HttpEntity<String> httpEntity = new HttpEntity<>(payloadToSend, headers);
        String targetUrl = context.targetUrl();

        // Protect against TOCTOU / runtime re-binding: re-verify target URL right before dispatch
        ssrfProtectionService.validateDestinationUrl(targetUrl, context.isMock());

        try {
            logger.info("Dispatching FHIR POST to destination: {}", targetUrl);
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, httpEntity, String.class);
            int statusCode = response.getStatusCode().value();
            String responseSummary = summarizeResponse(response.getBody(), statusCode);

            if (response.getStatusCode().is2xxSuccessful()) {
                return new DeliveryOutcome("SUCCESS", statusCode, null, responseSummary, response.getHeaders(), null);
            } else {
                return new DeliveryOutcome("FAILED", statusCode, "Destination server returned non-success HTTP status: " + statusCode, responseSummary, response.getHeaders(), null);
            }
        } catch (HttpStatusCodeException ex) {
            int statusCode = ex.getStatusCode().value();
            String safeError = "Destination server returned HTTP " + statusCode;
            String responseSummary = summarizeResponse(ex.getResponseBodyAsString(), statusCode);
            logger.warn("Destination responded with HTTP error {}: {}", statusCode, ex.getStatusText());
            return new DeliveryOutcome("FAILED", statusCode, safeError, responseSummary, ex.getResponseHeaders(), ex);
        } catch (ResourceAccessException ex) {
            logger.warn("Destination connection failed: {}", ex.getMessage());
            if (ex.getCause() instanceof SocketTimeoutException) {
                return new DeliveryOutcome("FAILED", 504, "Destination server request timed out after " + (READ_TIMEOUT_MS / 1000) + " seconds", null, null, ex);
            } else {
                return new DeliveryOutcome("FAILED", null, "Failed to connect to destination server: Connection refused or host unreachable", null, null, ex);
            }
        } catch (Exception ex) {
            logger.error("Unexpected error during FHIR delivery", ex);
            return new DeliveryOutcome("FAILED", null, "Delivery failed: " + sanitizeErrorMessage(ex.getMessage()), null, null, ex);
        }
    }

    private String preparePayloadForDestination(String fhirPayload, String targetUrl) {
        try {
            IBaseResource resource = fhirContext.newJsonParser().parseResource(fhirPayload);
            if (resource instanceof org.hl7.fhir.r4.model.Bundle bundle) {
                boolean isSpecificBundleEndpoint = targetUrl.matches(".*/Bundle/?$");
                if (!isSpecificBundleEndpoint && bundle.getType() == org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION) {
                    org.hl7.fhir.r4.model.Bundle transactionBundle = bundle.copy();
                    transactionBundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION);
                    for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry : transactionBundle.getEntry()) {
                        if (entry.getRequest() == null || entry.getRequest().getMethod() == null) {
                            org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent req = new org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent();
                            if (entry.getResource() != null) {
                                String type = entry.getResource().fhirType();
                                String id = entry.getResource().getIdElement() != null ? entry.getResource().getIdElement().getIdPart() : null;
                                if (id != null && !id.isBlank()) {
                                    req.setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.PUT);
                                    req.setUrl(type + "/" + id);
                                } else {
                                    req.setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST);
                                    req.setUrl(type);
                                }
                            } else {
                                req.setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST);
                                req.setUrl("Bundle");
                            }
                            entry.setRequest(req);
                        }
                    }
                    return fhirContext.newJsonParser().encodeResourceToString(transactionBundle);
                }
            }
        } catch (Exception ex) {
            logger.debug("Bundle transaction adaptation omitted: {}", ex.getMessage());
        }
        return fhirPayload;
    }

    private String summarizeResponse(String responseBody, int statusCode) {
        if (responseBody == null || responseBody.isBlank()) {
            return "HTTP " + statusCode + " response (empty body)";
        }
        try {
            IBaseResource resource = fhirContext.newJsonParser().parseResource(responseBody);
            if (resource instanceof org.hl7.fhir.r4.model.OperationOutcome outcome) {
                if (outcome.hasIssue() && !outcome.getIssue().isEmpty()) {
                    org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent issue = outcome.getIssueFirstRep();
                    String diag = issue.getDiagnostics();
                    String sev = issue.getSeverity() != null ? issue.getSeverity().toCode() : "info";
                    if (diag != null && !diag.isBlank()) {
                        return "OperationOutcome [" + sev + "]: " + sanitizeSummary(diag);
                    }
                }
                return "OperationOutcome: Acknowledged";
            } else if (resource instanceof org.hl7.fhir.r4.model.Bundle bundle) {
                int count = bundle.getEntry() != null ? bundle.getEntry().size() : 0;
                String type = bundle.getType() != null ? bundle.getType().toCode() : "bundle";
                return "FHIR " + type + " response with " + count + " entries acknowledged";
            } else if (resource != null) {
                String idPart = (resource.getIdElement() != null && resource.getIdElement().getIdPart() != null)
                        ? resource.getIdElement().getIdPart()
                        : "acknowledged";
                return "FHIR " + resource.fhirType() + " accepted (ID: " + idPart + ")";
            }
        } catch (Exception ignored) {
            // Non-FHIR response body
        }
        return sanitizeSummary(responseBody);
    }

    private String sanitizeSummary(String text) {
        if (text == null) return null;
        String clean = text.replaceAll("[\\r\\n]+", " ").trim();
        if (clean.length() > 200) {
            return clean.substring(0, 197) + "...";
        }
        return clean;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeliveryResultEntity persistDeliveryResult(Long conversionId, Long destinationId, String destinationName,
                                                      String status, Integer httpStatusCode, String errorMessage,
                                                      String responseSummary, Integer attemptCount, Integer maxAttempts,
                                                      LocalDateTime nextRetryAt, Hospital hospital) {
        DeliveryResultEntity entity = new DeliveryResultEntity(
                conversionId,
                destinationId,
                destinationName,
                LocalDateTime.now(),
                status,
                httpStatusCode,
                errorMessage,
                responseSummary,
                attemptCount,
                maxAttempts,
                nextRetryAt,
                hospital
        );
        return deliveryResultRepository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeliveryResultEntity persistDeliveryResult(Long conversionId, Long destinationId, String destinationName,
                                                      String status, Integer httpStatusCode, String errorMessage,
                                                      String responseSummary, Hospital hospital) {
        return persistDeliveryResult(conversionId, destinationId, destinationName, status, httpStatusCode, errorMessage,
                responseSummary, 1, retryPolicyService.getMaxAttempts(), null, hospital);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeliveryResultEntity persistDeliveryResult(Long conversionId, Long destinationId, String destinationName,
                                                      String status, Integer httpStatusCode, String errorMessage,
                                                      Hospital hospital) {
        return persistDeliveryResult(conversionId, destinationId, destinationName, status, httpStatusCode, errorMessage,
                null, 1, retryPolicyService.getMaxAttempts(), null, hospital);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResultResponse> getDeliveriesForConversion(Long conversionId) {
        ConversionEntity conversion = conversionRepository.findById(conversionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversion not found with id: " + conversionId));

        tenantSecurityService.validateResourceHospital(conversion.getHospital());

        List<DeliveryResultEntity> results = deliveryResultRepository.findByConversionIdOrderByAttemptTimestampDesc(conversionId);
        List<DeliveryResultResponse> responses = new ArrayList<>(results.size());
        for (DeliveryResultEntity entity : results) {
            responses.add(mapToResponse(entity));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResultResponse> getDeliveriesForDestination(Long destinationId) {
        DestinationEntity destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new DestinationNotFoundException("Destination not found with id: " + destinationId));

        if (destination.getHospital() != null) {
            tenantSecurityService.validateResourceHospital(destination.getHospital());
        }

        List<DeliveryResultEntity> results = deliveryResultRepository.findByDestinationIdOrderByAttemptTimestampDesc(destinationId);
        List<DeliveryResultResponse> responses = new ArrayList<>(results.size());
        for (DeliveryResultEntity entity : results) {
            responses.add(mapToResponse(entity));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResultResponse getDeliveryById(Long deliveryId) {
        DeliveryResultEntity entity = deliveryResultRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery record not found with id: " + deliveryId));

        if (entity.getHospital() != null) {
            tenantSecurityService.validateResourceHospital(entity.getHospital());
        }

        return mapToResponse(entity);
    }

    private DeliveryResultResponse mapToResponse(DeliveryResultEntity entity) {
        Long hospitalId = entity.getHospital() != null ? entity.getHospital().getId() : null;
        String hospitalName = entity.getHospital() != null ? entity.getHospital().getName() : null;

        return new DeliveryResultResponse(
                entity.getId(),
                entity.getConversionId(),
                entity.getDestinationId(),
                entity.getDestinationName(),
                entity.getAttemptTimestamp(),
                entity.getStatus(),
                entity.getHttpStatusCode(),
                entity.getErrorMessage(),
                entity.getResponseSummary(),
                entity.getAttemptCount(),
                entity.getMaxAttempts(),
                entity.getNextRetryAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isManualRetryEligible(),
                hospitalId,
                hospitalName
        );
    }

    private String sanitizeErrorMessage(String msg) {
        if (msg == null) return "Unknown error";
        if (msg.length() > 250) {
            return msg.substring(0, 250) + "...";
        }
        return msg;
    }

    protected record DeliveryContext(
            Long conversionId,
            Long destinationId,
            String destinationName,
            String targetUrl,
            DestinationAuthType authType,
            String authToken,
            String fhirPayload,
            Hospital hospital,
            boolean isMock
    ) {}

    protected record DeliveryOutcome(
            String status,
            Integer httpStatusCode,
            String errorMessage,
            String responseSummary,
            HttpHeaders responseHeaders,
            Throwable throwable
    ) {
        public DeliveryOutcome(String status, Integer httpStatusCode, String errorMessage, String responseSummary) {
            this(status, httpStatusCode, errorMessage, responseSummary, null, null);
        }
    }
}
