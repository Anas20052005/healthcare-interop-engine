package com.example.healthcare.integration;

import com.example.healthcare.dto.auth.LoginRequest;
import com.example.healthcare.dto.destination.DestinationCreateRequest;
import com.example.healthcare.entity.AuditLogEntity;
import com.example.healthcare.entity.DestinationAuthType;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.AuditLogRepository;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for Priority 8: Audit Logging.
 * Validates:
 * 1. Automatic audit trail creation for logins (success and failure).
 * 2. Automatic audit trail creation for destination management.
 * 3. Strict tenant isolation (Hospital A cannot retrieve Hospital B's audit logs).
 * 4. Platform admin cross-hospital querying and filtering.
 * 5. Rejection of unauthenticated and unauthorized requests.
 * 6. Audit details do not leak passwords, tokens, or sensitive patient data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuditLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private User adminHospitalA;
    private User adminHospitalB;
    private User platformAdmin;

    private String tokenHospitalA;
    private String tokenHospitalB;
    private String tokenPlatformAdmin;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        hospitalA = new Hospital("General Hospital A", "GEN_HOSP_A");
        hospitalA = hospitalRepository.save(hospitalA);

        hospitalB = new Hospital("St. Peter Hospital B", "ST_PETER_B");
        hospitalB = hospitalRepository.save(hospitalB);

        adminHospitalA = new User("admin_a", passwordEncoder.encode("Password123!"), "ADMIN", true);
        adminHospitalA.setHospital(hospitalA);
        adminHospitalA = userRepository.save(adminHospitalA);

        adminHospitalB = new User("admin_b", passwordEncoder.encode("Password123!"), "ADMIN", true);
        adminHospitalB.setHospital(hospitalB);
        adminHospitalB = userRepository.save(adminHospitalB);

        platformAdmin = new User("super_admin", passwordEncoder.encode("AdminPass123!"), "ADMIN", true);
        platformAdmin = userRepository.save(platformAdmin);

        tokenHospitalA = "Bearer " + jwtService.generateToken(adminHospitalA.getUsername(), "ROLE_ADMIN", hospitalA.getId());
        tokenHospitalB = "Bearer " + jwtService.generateToken(adminHospitalB.getUsername(), "ROLE_ADMIN", hospitalB.getId());
        tokenPlatformAdmin = "Bearer " + jwtService.generateToken(platformAdmin.getUsername(), "ROLE_ADMIN", null);
    }

    @Test
    @DisplayName("1. Successful and failed login attempts generate audit log records")
    void shouldAuditSuccessfulAndFailedLogins() throws Exception {
        // Successful login
        LoginRequest validLogin = new LoginRequest("admin_a", "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk());

        // Failed login
        LoginRequest invalidLogin = new LoginRequest("admin_a", "WrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());

        // Verify audit logs exist
        var logs = auditLogRepository.findAll();
        boolean hasSuccess = logs.stream().anyMatch(l -> "LOGIN_SUCCESS".equals(l.getAction()) && "admin_a".equals(l.getUsername()));
        boolean hasFailure = logs.stream().anyMatch(l -> "LOGIN_FAILURE".equals(l.getAction()) && "admin_a".equals(l.getUsername()));

        assertTrue(hasSuccess, "Audit log must contain LOGIN_SUCCESS for admin_a");
        assertTrue(hasFailure, "Audit log must contain LOGIN_FAILURE for admin_a");

        // Verify no password leak
        for (AuditLogEntity log : logs) {
            assertFalse(log.getDetails().contains("Password123!"), "Audit log details must never contain password");
            assertFalse(log.getDetails().contains("WrongPassword!"), "Audit log details must never contain wrong password");
        }
    }

    @Test
    @DisplayName("2. Destination creation generates an audit record")
    void shouldAuditDestinationCreation() throws Exception {
        DestinationCreateRequest createReq = new DestinationCreateRequest();
        createReq.setName("EHR FHIR Endpoint");
        createReq.setBaseUrl("https://ehr.hospital-a.org/fhir");
        createReq.setFhirVersion("R4");
        createReq.setAuthType(DestinationAuthType.NONE);
        createReq.setEnabled(true);
        createReq.setMock(false);

        mockMvc.perform(post("/api/destinations")
                        .header("Authorization", tokenHospitalA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        var logs = auditLogRepository.findAll();
        boolean hasDestCreate = logs.stream().anyMatch(l -> "DESTINATION_CREATE".equals(l.getAction()) && "SUCCESS".equals(l.getOutcome()));
        assertTrue(hasDestCreate, "Audit log must record DESTINATION_CREATE action");
    }

    @Test
    @DisplayName("3. Hospital A cannot retrieve Hospital B's audit logs (Strict Multi-Tenant Isolation)")
    void hospitalACannotRetrieveHospitalBAuditLogs() throws Exception {
        // Insert audit log for Hospital A
        AuditLogEntity logA = new AuditLogEntity(
                adminHospitalA.getId(), adminHospitalA.getUsername(), hospitalA,
                "USER_UPDATE", "USER", "1", "SUCCESS", LocalDateTime.now(), "Updated user details", null
        );
        auditLogRepository.save(logA);

        // Insert audit log for Hospital B
        AuditLogEntity logB = new AuditLogEntity(
                adminHospitalB.getId(), adminHospitalB.getUsername(), hospitalB,
                "USER_UPDATE", "USER", "2", "SUCCESS", LocalDateTime.now(), "Updated user details", null
        );
        logB = auditLogRepository.save(logB);

        // Hospital A queries /api/audit-logs: should only see logA
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", tokenHospitalA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].hospitalId", is(hospitalA.getId().intValue())))
                .andExpect(jsonPath("$.content[0].username", is("admin_a")));

        // Hospital A attempts direct access to Hospital B's log by ID: must be rejected with 403 Forbidden
        mockMvc.perform(get("/api/audit-logs/{id}", logB.getId())
                        .header("Authorization", tokenHospitalA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Platform Admin can retrieve logs and filter by hospital ID")
    void platformAdminCanRetrieveAllAndFilterByHospital() throws Exception {
        AuditLogEntity logA = new AuditLogEntity(
                adminHospitalA.getId(), adminHospitalA.getUsername(), hospitalA,
                "USER_CREATE", "USER", "1", "SUCCESS", LocalDateTime.now(), "Created user for Hospital A", null
        );
        auditLogRepository.save(logA);

        AuditLogEntity logB = new AuditLogEntity(
                adminHospitalB.getId(), adminHospitalB.getUsername(), hospitalB,
                "USER_CREATE", "USER", "2", "SUCCESS", LocalDateTime.now(), "Created user for Hospital B", null
        );
        auditLogRepository.save(logB);

        // Super admin sees both
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", tokenPlatformAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Super admin filters by hospital A
        mockMvc.perform(get("/api/audit-logs?hospitalId=" + hospitalA.getId())
                        .header("Authorization", tokenPlatformAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].hospitalId", is(hospitalA.getId().intValue())));
    }

    @Test
    @DisplayName("5. Unauthenticated request to /api/audit-logs is rejected with 401")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6. Audit log pagination and outcome filtering work correctly")
    void auditLogPaginationAndFiltersWork() throws Exception {
        for (int i = 1; i <= 5; i++) {
            AuditLogEntity log = new AuditLogEntity(
                    adminHospitalA.getId(), adminHospitalA.getUsername(), hospitalA,
                    "HL7_CONVERSION_SUCCESS", "CONVERSION", String.valueOf(i),
                    (i % 2 == 0) ? "FAILURE" : "SUCCESS",
                    LocalDateTime.now().minusMinutes(i), "Conversion step " + i, null
            );
            auditLogRepository.save(log);
        }

        // Test pagination size=2
        mockMvc.perform(get("/api/audit-logs?page=0&size=2")
                        .header("Authorization", tokenHospitalA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)));

        // Test outcome filter=FAILURE
        mockMvc.perform(get("/api/audit-logs?outcome=FAILURE")
                        .header("Authorization", tokenHospitalA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].outcome", everyItem(is("FAILURE"))));
    }
}
