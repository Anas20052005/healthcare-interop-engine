package com.example.healthcare.controller;

import com.example.healthcare.dto.audit.AuditLogFilter;
import com.example.healthcare.dto.audit.AuditLogPageResponse;
import com.example.healthcare.dto.audit.AuditLogResponse;
import com.example.healthcare.service.AuditService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST controller exposing endpoints for viewing system and tenant audit trails.
 * Enforces role-based access and tenant isolation (hospital users can only see
 * their hospital's logs; platform admins can view all or filter by hospital).
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Retrieves paginated audit logs based on filter criteria.
     * Enforces hospital tenant isolation.
     */
    @GetMapping
    public ResponseEntity<AuditLogPageResponse> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        // Enforce safe page bounds
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        AuditLogFilter filter = new AuditLogFilter(
                action, outcome, resourceType, resourceId, search, hospitalId, startDate, endDate
        );

        AuditLogPageResponse response = auditService.getAuditLogs(filter, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single audit log entry by ID, verifying hospital tenant access.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponse> getAuditLogById(@PathVariable Long id) {
        AuditLogResponse response = auditService.getAuditLogById(id);
        return ResponseEntity.ok(response);
    }
}
