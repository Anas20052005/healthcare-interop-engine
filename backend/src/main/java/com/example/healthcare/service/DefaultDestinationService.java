package com.example.healthcare.service;

import com.example.healthcare.dto.destination.DestinationCreateRequest;
import com.example.healthcare.dto.destination.DestinationResponse;
import com.example.healthcare.dto.destination.DestinationTestResponse;
import com.example.healthcare.dto.destination.DestinationUpdateRequest;
import com.example.healthcare.entity.DestinationAuthType;
import com.example.healthcare.entity.DestinationEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.exception.DestinationNotFoundException;
import com.example.healthcare.exception.InvalidDestinationException;
import com.example.healthcare.exception.TenantAccessDeniedException;
import com.example.healthcare.repository.DestinationRepository;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.security.TenantSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link DestinationService}.
 * Enforces hospital tenant isolation and secure storage for credentials.
 */
@Service
public class DefaultDestinationService implements DestinationService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDestinationService.class);

    private final DestinationRepository destinationRepository;
    private final HospitalRepository hospitalRepository;
    private final TenantSecurityService tenantSecurityService;
    private final SsrfProtectionService ssrfProtectionService;
    private final TokenCryptoService tokenCryptoService;
    private final RestTemplate connectionTestRestTemplate;
    private final AuditService auditService;

    private static final int TEST_CONNECT_TIMEOUT_MS = 5000;
    private static final int TEST_READ_TIMEOUT_MS   = 8000;

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultDestinationService(DestinationRepository destinationRepository,
                                     HospitalRepository hospitalRepository,
                                     TenantSecurityService tenantSecurityService,
                                     SsrfProtectionService ssrfProtectionService,
                                     TokenCryptoService tokenCryptoService,
                                     AuditService auditService) {
        this.destinationRepository = destinationRepository;
        this.hospitalRepository = hospitalRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.ssrfProtectionService = ssrfProtectionService;
        this.tokenCryptoService = tokenCryptoService;
        this.auditService = auditService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TEST_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(TEST_READ_TIMEOUT_MS);
        this.connectionTestRestTemplate = new RestTemplate(factory);
    }

    public DefaultDestinationService(DestinationRepository destinationRepository,
                                     HospitalRepository hospitalRepository,
                                     TenantSecurityService tenantSecurityService,
                                     SsrfProtectionService ssrfProtectionService,
                                     TokenCryptoService tokenCryptoService) {
        this(destinationRepository, hospitalRepository, tenantSecurityService, ssrfProtectionService, tokenCryptoService, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationResponse> getAccessibleDestinations() {
        List<DestinationEntity> entities;
        if (tenantSecurityService.isPlatformAdmin()) {
            logger.info("Platform admin retrieving all destinations");
            entities = destinationRepository.findAllByOrderByCreatedAtDesc();
        } else {
            Hospital hospital = tenantSecurityService.requireCurrentHospital();
            logger.info("Retrieving accessible destinations for hospital '{}' (ID: {})", hospital.getName(), hospital.getId());
            entities = destinationRepository.findByHospitalIdOrHospitalIsNullOrderByCreatedAtDesc(hospital.getId());
        }

        List<DestinationResponse> responses = new ArrayList<>(entities.size());
        for (DestinationEntity entity : entities) {
            responses.add(mapToResponse(entity));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public DestinationResponse getDestinationById(Long id) {
        DestinationEntity entity = destinationRepository.findById(id)
                .orElseThrow(() -> new DestinationNotFoundException("Destination not found with id: " + id));

        // Enforce authorization
        validateDestinationAccess(entity);

        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public DestinationResponse createDestination(DestinationCreateRequest request) {
        Hospital assignedHospital = null;
        if (tenantSecurityService.isPlatformAdmin()) {
            if (request.getHospitalId() != null) {
                assignedHospital = hospitalRepository.findById(request.getHospitalId())
                        .orElseThrow(() -> new InvalidDestinationException("Assigned hospital not found with id: " + request.getHospitalId()));
            }
        } else {
            assignedHospital = tenantSecurityService.requireCurrentHospital();
        }

        boolean isMock = Boolean.TRUE.equals(request.getMock());
        // Validate URL against SSRF policy
        ssrfProtectionService.validateDestinationUrl(request.getBaseUrl(), isMock);

        // Check duplicate name in scope
        if (assignedHospital != null) {
            if (destinationRepository.existsByNameAndHospitalId(request.getName(), assignedHospital.getId())) {
                throw new InvalidDestinationException("Destination with name '" + request.getName() + "' already exists for this hospital");
            }
        } else {
            if (destinationRepository.existsByNameAndHospitalIsNull(request.getName())) {
                throw new InvalidDestinationException("Platform-wide destination with name '" + request.getName() + "' already exists");
            }
        }

        DestinationEntity entity = new DestinationEntity(
                request.getName(),
                request.getBaseUrl().trim(),
                request.getFhirVersion(),
                request.getAuthType(),
                request.getEnabled() != null ? request.getEnabled() : true,
                isMock,
                assignedHospital
        );

        if (request.getAuthType() == DestinationAuthType.BEARER_TOKEN && request.getBearerToken() != null && !request.getBearerToken().isBlank()) {
            entity.setAuthToken(tokenCryptoService.encrypt(request.getBearerToken().trim()));
        }

        DestinationEntity saved = destinationRepository.save(entity);
        logger.info("Destination '{}' (ID: {}) created successfully for hospital: {}",
                saved.getName(), saved.getId(), assignedHospital != null ? assignedHospital.getName() : "Platform-Wide");

        if (auditService != null) {
            auditService.log(
                    "DESTINATION_CREATE",
                    "DESTINATION",
                    String.valueOf(saved.getId()),
                    "SUCCESS",
                    "Created destination '" + saved.getName() + "' (URL: " + saved.getBaseUrl() + ", auth: " + saved.getAuthType() + ")",
                    saved.getHospital()
            );
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public DestinationResponse updateDestination(Long id, DestinationUpdateRequest request) {
        DestinationEntity entity = destinationRepository.findById(id)
                .orElseThrow(() -> new DestinationNotFoundException("Destination not found with id: " + id));

        // Enforce update authority
        validateDestinationModification(entity);

        // Validate URL against SSRF policy
        ssrfProtectionService.validateDestinationUrl(request.getBaseUrl(), entity.isMock());

        // Check duplicate name if changed
        if (!entity.getName().equalsIgnoreCase(request.getName())) {
            if (entity.getHospital() != null) {
                if (destinationRepository.existsByNameAndHospitalId(request.getName(), entity.getHospital().getId())) {
                    throw new InvalidDestinationException("Destination with name '" + request.getName() + "' already exists for this hospital");
                }
            } else {
                if (destinationRepository.existsByNameAndHospitalIsNull(request.getName())) {
                    throw new InvalidDestinationException("Platform-wide destination with name '" + request.getName() + "' already exists");
                }
            }
        }

        entity.setName(request.getName());
        entity.setBaseUrl(request.getBaseUrl().trim());
        if (request.getFhirVersion() != null && !request.getFhirVersion().isBlank()) {
            entity.setFhirVersion(request.getFhirVersion());
        }
        entity.setAuthType(request.getAuthType());
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }

        // Handle token: if BEARER_TOKEN and new token provided, encrypt & set.
        // If BEARER_TOKEN and token is null/blank, retain existing token!
        if (request.getAuthType() == DestinationAuthType.BEARER_TOKEN) {
            if (request.getBearerToken() != null && !request.getBearerToken().isBlank()) {
                entity.setAuthToken(tokenCryptoService.encrypt(request.getBearerToken().trim()));
            }
        } else {
            entity.setAuthToken(null);
        }

        DestinationEntity updated = destinationRepository.save(entity);
        logger.info("Destination '{}' (ID: {}) updated successfully", updated.getName(), updated.getId());

        if (auditService != null) {
            auditService.log(
                    "DESTINATION_UPDATE",
                    "DESTINATION",
                    String.valueOf(updated.getId()),
                    "SUCCESS",
                    "Updated destination '" + updated.getName() + "' (URL: " + updated.getBaseUrl() + ", auth: " + updated.getAuthType() + ")",
                    updated.getHospital()
            );
        }

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public DestinationResponse updateDestinationStatus(Long id, boolean enabled) {
        DestinationEntity entity = destinationRepository.findById(id)
                .orElseThrow(() -> new DestinationNotFoundException("Destination not found with id: " + id));

        validateDestinationModification(entity);

        entity.setEnabled(enabled);
        DestinationEntity updated = destinationRepository.save(entity);
        logger.info("Destination '{}' (ID: {}) status updated to enabled={}", updated.getName(), updated.getId(), enabled);

        if (auditService != null) {
            auditService.log(
                    "DESTINATION_STATUS_TOGGLE",
                    "DESTINATION",
                    String.valueOf(updated.getId()),
                    "SUCCESS",
                    "Toggled destination '" + updated.getName() + "' status to enabled=" + enabled,
                    updated.getHospital()
            );
        }

        return mapToResponse(updated);
    }

    @Override
    public void deleteDestination(Long id) {
        DestinationEntity entity = destinationRepository.findById(id)
                .orElseThrow(() -> new DestinationNotFoundException("Destination not found with id: " + id));

        validateDestinationModification(entity);

        destinationRepository.delete(entity);
        logger.info("Destination '{}' (ID: {}) deleted successfully", entity.getName(), entity.getId());

        if (auditService != null) {
            auditService.log(
                    "DESTINATION_DELETE",
                    "DESTINATION",
                    String.valueOf(entity.getId()),
                    "SUCCESS",
                    "Deleted destination '" + entity.getName() + "'",
                    entity.getHospital()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DestinationTestResponse testConnection(Long id) {
        DestinationEntity entity = destinationRepository.findById(id)
                .orElseThrow(() -> new DestinationNotFoundException("Destination not found with id: " + id));

        // Enforce read-level access: user must be able to see this destination
        validateDestinationAccess(entity);

        // Build the FHIR metadata probe URL: {baseUrl}/metadata
        String metadataUrl = entity.getBaseUrl().stripTrailing();
        if (!metadataUrl.endsWith("/metadata")) {
            metadataUrl = metadataUrl.replaceAll("/+$", "") + "/metadata";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.valueOf("application/fhir+json")));

        // Attach bearer token if configured (needed for protected endpoints)
        if (entity.getAuthType() == DestinationAuthType.BEARER_TOKEN && entity.getAuthToken() != null) {
            try {
                String plainToken = tokenCryptoService.decrypt(entity.getAuthToken());
                if (plainToken != null && !plainToken.isBlank()) {
                    headers.setBearerAuth(plainToken);
                }
            } catch (Exception e) {
                logger.warn("Could not decrypt bearer token for connection test on destination {}: {}", id, e.getMessage());
            }
        }

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        long startMs = System.currentTimeMillis();

        try {
            logger.info("Connection test: probing FHIR metadata at '{}'", metadataUrl);
            ResponseEntity<String> response = connectionTestRestTemplate.exchange(
                    metadataUrl, HttpMethod.GET, requestEntity, String.class);
            long latencyMs = System.currentTimeMillis() - startMs;
            int statusCode = response.getStatusCode().value();

            // Try to extract basic info from CapabilityStatement if returned
            String softwareName = null;
            String fhirVersion = null;
            String body = response.getBody();
            if (body != null) {
                // Simple JSON field extraction — avoids a heavy FHIR parse just for display
                softwareName = extractJsonStringField(body, "software", "name");
                fhirVersion  = extractJsonStringField(body, "fhirVersion");
            }

            boolean success = response.getStatusCode().is2xxSuccessful();
            String message = success
                    ? String.format("Reachable — HTTP %d in %d ms", statusCode, latencyMs)
                    : String.format("Responded HTTP %d in %d ms (non-2xx)", statusCode, latencyMs);

            logger.info("Connection test result for destination '{}' (ID: {}): {}", entity.getName(), id, message);
            return new DestinationTestResponse(success, message, statusCode, softwareName, fhirVersion, latencyMs);

        } catch (HttpStatusCodeException ex) {
            long latencyMs = System.currentTimeMillis() - startMs;
            int statusCode = ex.getStatusCode().value();
            String message = String.format("Server responded HTTP %d in %d ms", statusCode, latencyMs);
            // 4xx responses still prove the server is reachable
            boolean success = statusCode == 401 || statusCode == 403;
            logger.warn("Connection test HTTP error for destination '{}': {}", entity.getName(), statusCode);
            return new DestinationTestResponse(success, message, statusCode, null, null, latencyMs);

        } catch (ResourceAccessException ex) {
            long latencyMs = System.currentTimeMillis() - startMs;
            String cause = ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "Connection error";
            String message = "Connection failed: " + cause + " (" + latencyMs + " ms)";
            logger.warn("Connection test failed for destination '{}': {}", entity.getName(), ex.getMessage());
            return new DestinationTestResponse(false, message, null, null, null, latencyMs);

        } catch (Exception ex) {
            long latencyMs = System.currentTimeMillis() - startMs;
            logger.error("Unexpected error during connection test for destination {}", id, ex);
            return new DestinationTestResponse(false, "Unexpected error: " + ex.getClass().getSimpleName(), null, null, null, latencyMs);
        }
    }

    /**
     * Minimal JSON string field extractor. Searches for {@code "field": "value"} patterns
     * without a full JSON parser to keep this lightweight.
     */
    private String extractJsonStringField(String json, String... fieldPath) {
        try {
            String current = json;
            for (int i = 0; i < fieldPath.length - 1; i++) {
                int idx = current.indexOf('"' + fieldPath[i] + '"');
                if (idx < 0) return null;
                current = current.substring(idx);
            }
            String lastField = fieldPath[fieldPath.length - 1];
            int startIdx = current.indexOf('"' + lastField + '"');
            if (startIdx < 0) return null;
            int colonIdx = current.indexOf(':', startIdx + lastField.length() + 2);
            if (colonIdx < 0) return null;
            int quoteStart = current.indexOf('"', colonIdx + 1);
            if (quoteStart < 0) return null;
            int quoteEnd = current.indexOf('"', quoteStart + 1);
            if (quoteEnd < 0) return null;
            String value = current.substring(quoteStart + 1, quoteEnd);
            return value.isBlank() ? null : value;
        } catch (Exception e) {
            return null;
        }
    }

    private void validateDestinationAccess(DestinationEntity entity) {
        if (tenantSecurityService.isPlatformAdmin()) {
            return;
        }
        if (entity.getHospital() == null) {
            // Platform-wide destinations can be read by any authenticated user
            return;
        }
        Hospital hospital = tenantSecurityService.requireCurrentHospital();
        if (!entity.getHospital().getId().equals(hospital.getId())) {
            logger.warn("Tenant access violation: user hospital {} attempted to view destination {} belonging to hospital {}",
                    hospital.getId(), entity.getId(), entity.getHospital().getId());
            throw new TenantAccessDeniedException("Access denied: cannot view destination belonging to another hospital");
        }
    }

    private void validateDestinationModification(DestinationEntity entity) {
        if (tenantSecurityService.isPlatformAdmin()) {
            return;
        }
        if (entity.getHospital() == null) {
            throw new TenantAccessDeniedException("Access denied: only platform administrators can modify platform-wide destinations");
        }
        Hospital hospital = tenantSecurityService.requireCurrentHospital();
        if (!entity.getHospital().getId().equals(hospital.getId())) {
            logger.warn("Tenant access violation: user hospital {} attempted to modify destination {} belonging to hospital {}",
                    hospital.getId(), entity.getId(), entity.getHospital().getId());
            throw new TenantAccessDeniedException("Access denied: cannot modify destination belonging to another hospital");
        }
    }

    private DestinationResponse mapToResponse(DestinationEntity entity) {
        boolean hasToken = entity.getAuthToken() != null && !entity.getAuthToken().isBlank();
        Long hospitalId = entity.getHospital() != null ? entity.getHospital().getId() : null;
        String hospitalName = entity.getHospital() != null ? entity.getHospital().getName() : null;

        return new DestinationResponse(
                entity.getId(),
                entity.getName(),
                entity.getBaseUrl(),
                entity.getFhirVersion(),
                entity.getAuthType(),
                hasToken,
                entity.isEnabled(),
                entity.isMock(),
                hospitalId,
                hospitalName,
                entity.getHospital() == null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
