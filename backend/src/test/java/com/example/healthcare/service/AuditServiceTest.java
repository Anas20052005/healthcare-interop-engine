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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    private DefaultAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new DefaultAuditService(auditLogRepository, tenantSecurityService);
    }

    @Test
    @DisplayName("Should successfully record audit log and sanitize CRLF injection")
    void shouldRecordAuditLogAndSanitizeInput() {
        Hospital hospital = new Hospital("Mercy Hospital", "MERCY_01");
        hospital.setId(10L);

        User currentUser = new User("admin_mercy", "hashedPass", "ADMIN", true);
        currentUser.setId(1L);
        currentUser.setHospital(hospital);

        when(tenantSecurityService.getCurrentUser()).thenReturn(Optional.of(currentUser));

        auditService.log(
                "USER_CREATE",
                "USER",
                "99",
                "SUCCESS",
                "Created user\r\nwith malicious CRLF\nnewlines",
                hospital
        );

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertEquals("USER_CREATE", saved.getAction());
        assertEquals("USER", saved.getResourceType());
        assertEquals("99", saved.getResourceId());
        assertEquals("SUCCESS", saved.getOutcome());
        assertEquals("admin_mercy", saved.getUsername());
        assertEquals(1L, saved.getActorId());
        assertEquals(hospital, saved.getHospital());
        assertEquals("Created user with malicious CRLF newlines", saved.getDetails());
    }

    @Test
    @DisplayName("Should record security events with null actor when unauthenticated")
    void shouldRecordSecurityEvent() {
        Hospital hospital = new Hospital("Mercy Hospital", "MERCY_01");
        hospital.setId(10L);

        auditService.logSecurityEvent("failed_user", "LOGIN_FAILURE", "FAILURE", "Bad password", hospital);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertEquals("LOGIN_FAILURE", saved.getAction());
        assertEquals("SECURITY", saved.getResourceType());
        assertEquals("FAILURE", saved.getOutcome());
        assertEquals("failed_user", saved.getUsername());
        assertNull(saved.getActorId());
        assertEquals(hospital, saved.getHospital());
    }

    @Test
    @DisplayName("Should enforce tenant scoping for hospital-level query")
    @SuppressWarnings("unchecked")
    void shouldEnforceTenantScopeOnQuery() {
        Hospital hospital = new Hospital("Hospital A", "HOSP_A");
        hospital.setId(5L);

        when(tenantSecurityService.isPlatformAdmin()).thenReturn(false);
        when(tenantSecurityService.requireCurrentHospital()).thenReturn(hospital);

        AuditLogEntity log1 = new AuditLogEntity(1L, "dr_smith", hospital, "USER_CREATE", "USER", "10",
                "SUCCESS", LocalDateTime.now(), "Created user", null);
        Page<AuditLogEntity> mockPage = new PageImpl<>(List.of(log1), PageRequest.of(0, 10), 1);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        AuditLogFilter filter = new AuditLogFilter();
        AuditLogPageResponse response = auditService.getAuditLogs(filter, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("dr_smith", response.getContent().get(0).getUsername());
        assertEquals(5L, response.getContent().get(0).getHospitalId());
    }

    @Test
    @DisplayName("Should reject access to audit log of another hospital")
    void shouldRejectAccessToOtherHospitalAuditLog() {
        Hospital hospitalA = new Hospital("Hospital A", "HOSP_A");
        hospitalA.setId(5L);

        AuditLogEntity otherHospitalLog = new AuditLogEntity(2L, "other_dr", hospitalA, "USER_CREATE", "USER", "11",
                "SUCCESS", LocalDateTime.now(), "Created user", null);
        otherHospitalLog.setId(100L);

        when(auditLogRepository.findById(100L)).thenReturn(Optional.of(otherHospitalLog));
        org.mockito.Mockito.doThrow(new TenantAccessDeniedException("Cross-tenant access forbidden"))
                .when(tenantSecurityService).validateResourceHospital(hospitalA);

        assertThrows(TenantAccessDeniedException.class, () -> auditService.getAuditLogById(100L));
    }
}
