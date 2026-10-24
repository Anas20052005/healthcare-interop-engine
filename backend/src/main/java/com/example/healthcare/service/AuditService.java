package com.example.healthcare.service;

import com.example.healthcare.dto.audit.AuditLogFilter;
import com.example.healthcare.dto.audit.AuditLogPageResponse;
import com.example.healthcare.dto.audit.AuditLogResponse;
import com.example.healthcare.entity.Hospital;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for recording immutable audit log records and
 * retrieving paginated, tenant-isolated audit trails.
 */
public interface AuditService {

    /**
     * Records an audit entry, automatically resolving current authenticated actor from security context.
     *
     * @param action       action identifier (e.g. USER_CREATE, HL7_CONVERSION_SUCCESS)
     * @param resourceType category of resource (e.g. USER, CONVERSION, DESTINATION)
     * @param resourceId   identifier of the affected resource (safe, non-PHI)
     * @param outcome      "SUCCESS" or "FAILURE"
     * @param details      sanitized, safe textual description
     * @param hospital     hospital entity associated with the action
     */
    void log(String action, String resourceType, String resourceId, String outcome, String details, Hospital hospital);

    /**
     * Explicitly records an audit entry with full parameter control.
     */
    void log(Long actorId, String username, String action, String resourceType, String resourceId,
             String outcome, String details, Hospital hospital, String correlationId);

    /**
     * Records a security event (such as login success/failure or access denied) where actor may not yet be authenticated.
     */
    void logSecurityEvent(String username, String action, String outcome, String details, Hospital hospital);

    /**
     * Retrieves paginated audit logs based on filter criteria, enforcing strict multi-tenant hospital isolation.
     */
    AuditLogPageResponse getAuditLogs(AuditLogFilter filter, Pageable pageable);

    /**
     * Retrieves a single audit log entry by ID, verifying tenant access.
     */
    AuditLogResponse getAuditLogById(Long id);
}
