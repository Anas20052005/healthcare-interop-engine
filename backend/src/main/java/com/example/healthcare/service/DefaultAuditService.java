package com.example.healthcare.service;

import com.example.healthcare.dto.audit.AuditLogFilter;
import com.example.healthcare.dto.audit.AuditLogPageResponse;
import com.example.healthcare.dto.audit.AuditLogResponse;
import com.example.healthcare.entity.AuditLogEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.exception.ResourceNotFoundException;
import com.example.healthcare.exception.TenantAccessDeniedException;
import com.example.healthcare.repository.AuditLogRepository;
import com.example.healthcare.security.TenantSecurityService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link AuditService}.
 * Persists immutable audit records in separate transactions (REQUIRES_NEW)
 * and enforces strict multi-tenant isolation on audit queries.
 */
@Service
public class DefaultAuditService implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final TenantSecurityService tenantSecurityService;
    private final com.example.healthcare.repository.HospitalRepository hospitalRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultAuditService(AuditLogRepository auditLogRepository,
                               TenantSecurityService tenantSecurityService,
                               com.example.healthcare.repository.HospitalRepository hospitalRepository) {
        this.auditLogRepository = auditLogRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.hospitalRepository = hospitalRepository;
    }

    public DefaultAuditService(AuditLogRepository auditLogRepository,
                               TenantSecurityService tenantSecurityService) {
        this(auditLogRepository, tenantSecurityService, null);
    }

    @Override
    @Transactional
    public void log(String action, String resourceType, String resourceId, String outcome, String details, Hospital hospital) {
        Long actorId = null;
        String username = null;
        Hospital resolvedHospital = hospital;

        try {
            Optional<User> currentUserOpt = tenantSecurityService.getCurrentUser();
            if (currentUserOpt.isPresent()) {
                User user = currentUserOpt.get();
                actorId = user.getId();
                username = user.getUsername();
                if (resolvedHospital == null && user.getHospital() != null) {
                    resolvedHospital = user.getHospital();
                }
            } else {
                username = "SYSTEM";
            }
        } catch (Exception e) {
            logger.debug("Could not resolve current user for audit log: {}", e.getMessage());
            username = "SYSTEM";
        }

        log(actorId, username, action, resourceType, resourceId, outcome, details, resolvedHospital, null);
    }

    @Override
    @Transactional
    public void log(Long actorId, String username, String action, String resourceType, String resourceId,
                    String outcome, String details, Hospital hospital, String correlationId) {
        try {
            String sanitizedDetails = sanitizeText(details);
            String safeAction = sanitizeText(action);
            String safeOutcome = (outcome != null && outcome.equalsIgnoreCase("FAILURE")) ? "FAILURE" : "SUCCESS";

            Hospital managedHospital = null;
            if (hospital != null && hospital.getId() != null) {
                if (hospitalRepository != null) {
                    managedHospital = hospitalRepository.findById(hospital.getId()).orElse(null);
                } else {
                    managedHospital = hospital;
                }
            }

            AuditLogEntity entity = new AuditLogEntity(
                    actorId,
                    username != null ? username.trim() : "ANONYMOUS",
                    managedHospital,
                    safeAction,
                    resourceType != null ? resourceType.trim() : null,
                    resourceId != null ? resourceId.trim() : null,
                    safeOutcome,
                    LocalDateTime.now(),
                    sanitizedDetails,
                    correlationId
            );

            auditLogRepository.save(entity);
            logger.info("Audit: action='{}', outcome='{}', actor='{}', resource='{}:{}', hospital='{}'",
                    safeAction, safeOutcome, username, resourceType, resourceId,
                    managedHospital != null ? managedHospital.getName() : "PLATFORM");
        } catch (Exception ex) {
            // Audit logging failure must never corrupt the main business transaction
            logger.error("Failed to persist audit log record: {}", ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public void logSecurityEvent(String username, String action, String outcome, String details, Hospital hospital) {
        log(null, username, action, "SECURITY", null, outcome, details, hospital, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogPageResponse getAuditLogs(AuditLogFilter filter, Pageable pageable) {
        // Enforce tenant isolation on audit log querying
        Long targetHospitalId = null;

        if (tenantSecurityService.isPlatformAdmin()) {
            // Platform admin can query across all or filter by requested hospital ID
            if (filter != null && filter.getHospitalId() != null) {
                targetHospitalId = filter.getHospitalId();
            }
        } else {
            // Hospital user/admin is strictly locked to their own hospital records
            Hospital currentHospital = tenantSecurityService.requireCurrentHospital();
            targetHospitalId = currentHospital.getId();
        }

        final Long scopedHospitalId = targetHospitalId;

        // Default to sorting by timestamp descending if unsorted
        Pageable resolvedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            resolvedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "timestamp"));
        }

        Specification<AuditLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant boundary
            if (scopedHospitalId != null) {
                predicates.add(cb.equal(root.get("hospital").get("id"), scopedHospitalId));
            }

            if (filter != null) {
                if (filter.getAction() != null && !filter.getAction().isBlank()) {
                    predicates.add(cb.equal(root.get("action"), filter.getAction().trim()));
                }

                if (filter.getOutcome() != null && !filter.getOutcome().isBlank()) {
                    predicates.add(cb.equal(root.get("outcome"), filter.getOutcome().trim().toUpperCase()));
                }

                if (filter.getResourceType() != null && !filter.getResourceType().isBlank()) {
                    predicates.add(cb.equal(root.get("resourceType"), filter.getResourceType().trim()));
                }

                if (filter.getResourceId() != null && !filter.getResourceId().isBlank()) {
                    predicates.add(cb.equal(root.get("resourceId"), filter.getResourceId().trim()));
                }

                if (filter.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), filter.getStartDate()));
                }

                if (filter.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), filter.getEndDate()));
                }

                if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                    String pattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("username")), pattern),
                            cb.like(cb.lower(root.get("action")), pattern),
                            cb.like(cb.lower(root.get("details")), pattern)
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLogEntity> pageResult = auditLogRepository.findAll(spec, resolvedPageable);

        List<AuditLogResponse> responseList = new ArrayList<>(pageResult.getContent().size());
        for (AuditLogEntity entity : pageResult.getContent()) {
            responseList.add(mapToResponse(entity));
        }

        return new AuditLogPageResponse(
                responseList,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long id) {
        AuditLogEntity entity = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log record not found with id: " + id));

        if (entity.getHospital() != null) {
            tenantSecurityService.validateResourceHospital(entity.getHospital());
        } else if (!tenantSecurityService.isPlatformAdmin()) {
            throw new TenantAccessDeniedException("Access denied: cannot access platform-level audit records");
        }

        return mapToResponse(entity);
    }

    private AuditLogResponse mapToResponse(AuditLogEntity entity) {
        Long hospitalId = entity.getHospital() != null ? entity.getHospital().getId() : null;
        String hospitalName = entity.getHospital() != null ? entity.getHospital().getName() : null;

        return new AuditLogResponse(
                entity.getId(),
                entity.getActorId(),
                entity.getUsername(),
                hospitalId,
                hospitalName,
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getOutcome(),
                entity.getTimestamp(),
                entity.getDetails(),
                entity.getCorrelationId()
        );
    }

    private String sanitizeText(String text) {
        if (text == null) return null;
        // Strip CR/LF to prevent log injection, and truncate safely
        String clean = text.replaceAll("[\\r\\n]+", " ").trim();
        if (clean.length() > 500) {
            return clean.substring(0, 497) + "...";
        }
        return clean;
    }
}
