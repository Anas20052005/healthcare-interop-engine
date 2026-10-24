package com.example.healthcare.integration;

import com.example.healthcare.dto.auth.LoginRequest;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.dto.user.CreateUserRequest;
import com.example.healthcare.dto.user.UpdateUserRequest;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.EncounterEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.ObservationEntity;
import com.example.healthcare.entity.PatientEntity;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.EncounterRepository;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.ObservationRepository;
import com.example.healthcare.repository.PatientRepository;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests validating Priority 4: Complete Multi-Tenant Data Isolation.
 * Verifies all 15 required security scenarios across Hospital A and Hospital B boundaries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TenantIsolationIntegrationTest {

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
    private PatientRepository patientRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private ConversionRepository conversionRepository;

    private Hospital hospitalA;
    private Hospital hospitalB;

    private User userA1;
    private User adminA;
    private User userB1;
    private User adminB;
    private User userNone;
    private User platformAdmin;

    private String tokenUserA;
    private String tokenAdminA;
    private String tokenUserB;
    private String tokenAdminB;
    private String tokenNone;
    private String tokenPlatformAdmin;

    private ConversionEntity conversionA;
    private ConversionEntity conversionB;
    private PatientEntity patientA;
    private PatientEntity patientB;
    private EncounterEntity encounterA;
    private EncounterEntity encounterB;
    private ObservationEntity observationA;
    private ObservationEntity observationB;

    private static final String SYNTHETIC_HL7_MSG =
            "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG-A-999|P|2.5\r" +
            "PID|||PAT-TEST-1||Smith^John||19850615|M\r" +
            "PV1||I|ICU-01||||||||||||||||||||||||||||||||202610080900\r" +
            "OBX|1|NM|GLUCOSE^Blood Glucose||110|mg/dL|70-110|N";

    @BeforeEach
    void setUp() {
        // Clear existing test records
        conversionRepository.deleteAll();
        observationRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();

        // 1. Setup Two Distinct Hospitals
        hospitalA = hospitalRepository.findByCode("HOSP_A").orElseGet(() ->
                hospitalRepository.save(new Hospital("Hospital Alpha", "HOSP_A", "alpha@test.com", "555-0101", "100 Alpha Way", true)));

        hospitalB = hospitalRepository.findByCode("HOSP_B").orElseGet(() ->
                hospitalRepository.save(new Hospital("Hospital Beta", "HOSP_B", "beta@test.com", "555-0202", "200 Beta Way", true)));

        // 2. Setup Users with trusted database-backed hospital associations
        userA1 = createOrUpdateUser("user_a1", "UserA1Password!", "USER", hospitalA);
        adminA = createOrUpdateUser("admin_a", "AdminAPassword!", "ADMIN", hospitalA);

        userB1 = createOrUpdateUser("user_b1", "UserB1Password!", "USER", hospitalB);
        adminB = createOrUpdateUser("admin_b", "AdminBPassword!", "ADMIN", hospitalB);

        userNone = createOrUpdateUser("user_none", "UserNonePassword!", "USER", null);
        platformAdmin = createOrUpdateUser("super_admin", "SuperAdminPassword!", "ADMIN", null);

        // 3. Generate JWT tokens with database hospitalId claims
        tokenUserA = jwtService.generateToken(userA1.getUsername(), userA1.getRole(), hospitalA.getId());
        tokenAdminA = jwtService.generateToken(adminA.getUsername(), adminA.getRole(), hospitalA.getId());
        tokenUserB = jwtService.generateToken(userB1.getUsername(), userB1.getRole(), hospitalB.getId());
        tokenAdminB = jwtService.generateToken(adminB.getUsername(), adminB.getRole(), hospitalB.getId());
        tokenNone = jwtService.generateToken(userNone.getUsername(), userNone.getRole(), null);
        tokenPlatformAdmin = jwtService.generateToken(platformAdmin.getUsername(), platformAdmin.getRole(), null);

        // 4. Seed Clinical & Conversion Records for Hospital A
        patientA = patientRepository.save(new PatientEntity("PAT_A_001", "Smith", "John", LocalDate.of(1985, 6, 15), "male", hospitalA));
        encounterA = encounterRepository.save(new EncounterEntity("ENC_A_001", patientA, "IMP", "ICU-01", null, null, hospitalA));
        observationA = observationRepository.save(new ObservationEntity(patientA, encounterA, "GLUCOSE", "NM", null, 110.0, "mg/dL", "70-110", "final", hospitalA));

        conversionA = new ConversionEntity("MSG_CTRL_A1", "ADT^A01", "SUCCESS", "PAT_A_001", null, hospitalA);
        conversionA.setEncounterIdentifier("ENC_A_001");
        conversionA.setObservationCount(1);
        conversionA = conversionRepository.save(conversionA);

        // 5. Seed Clinical & Conversion Records for Hospital B
        patientB = patientRepository.save(new PatientEntity("PAT_B_001", "Doe", "Jane", LocalDate.of(1990, 8, 20), "female", hospitalB));
        encounterB = encounterRepository.save(new EncounterEntity("ENC_B_001", patientB, "IMP", "WARD-B", null, null, hospitalB));
        observationB = observationRepository.save(new ObservationEntity(patientB, encounterB, "HEMOGLOBIN", "NM", null, 13.5, "g/dL", "12-16", "final", hospitalB));

        conversionB = new ConversionEntity("MSG_CTRL_B1", "ADT^A01", "SUCCESS", "PAT_B_001", null, hospitalB);
        conversionB.setEncounterIdentifier("ENC_B_001");
        conversionB.setObservationCount(1);
        conversionB = conversionRepository.save(conversionB);
    }

    private User createOrUpdateUser(String username, String rawPassword, String role, Hospital hospital) {
        return userRepository.findByUsername(username)
                .map(existing -> {
                    existing.setPassword(passwordEncoder.encode(rawPassword));
                    existing.setRole(role);
                    existing.setHospital(hospital);
                    existing.setEnabled(true);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User u = new User(username, passwordEncoder.encode(rawPassword), role, hospital, true);
                    return userRepository.save(u);
                });
    }

    @Test
    @DisplayName("Scenario 1: Hospital A user can access Hospital A history")
    void test1_HospitalAUserCanAccessHospitalAHistory() throws Exception {
        mockMvc.perform(get("/api/hl7/history")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].messageControlId").value("MSG_CTRL_A1"))
                .andExpect(jsonPath("$[0].patientId").value("PAT_A_001"))
                .andExpect(jsonPath("$[0].hospitalId").value(hospitalA.getId()));
    }

    @Test
    @DisplayName("Scenario 2: Hospital A user cannot access Hospital B history")
    void test2_HospitalAUserCannotAccessHospitalBHistory() throws Exception {
        mockMvc.perform(get("/api/hl7/history")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].messageControlId", not(hasSize(0))))
                .andExpect(jsonPath("$[*].messageControlId", not(org.hamcrest.Matchers.hasItem("MSG_CTRL_B1"))))
                .andExpect(jsonPath("$[*].patientId", not(org.hamcrest.Matchers.hasItem("PAT_B_001"))));
    }

    @Test
    @DisplayName("Scenario 3: Hospital B user can access Hospital B history")
    void test3_HospitalBUserCanAccessHospitalBHistory() throws Exception {
        mockMvc.perform(get("/api/hl7/history")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].messageControlId").value("MSG_CTRL_B1"))
                .andExpect(jsonPath("$[0].patientId").value("PAT_B_001"))
                .andExpect(jsonPath("$[0].hospitalId").value(hospitalB.getId()));
    }

    @Test
    @DisplayName("Scenario 4: Hospital A user cannot retrieve Hospital B conversion details by ID tampering")
    void test4_HospitalAUserCannotRetrieveHospitalBConversionDetails() throws Exception {
        // Attempting direct access to Hospital B's conversion ID using Hospital A credentials
        mockMvc.perform(get("/api/hl7/conversions/" + conversionB.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Access denied")));
    }

    @Test
    @DisplayName("Scenario 5: Hospital A user cannot retrieve Hospital B patients, encounters or observations")
    void test5_HospitalAUserCannotRetrieveHospitalBClinicalData() throws Exception {
        // 1. Hospital B Patient
        mockMvc.perform(get("/api/patients/" + patientB.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());

        // 2. Hospital B Encounter
        mockMvc.perform(get("/api/encounters/" + encounterB.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());

        // 3. Hospital B Observation
        mockMvc.perform(get("/api/observations/" + observationB.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 6: Hospital A user cannot modify or delete Hospital B records")
    void test6_HospitalAUserCannotModifyOrDeleteHospitalBRecords() throws Exception {
        // Hospital A user attempts to delete Hospital B patient
        mockMvc.perform(delete("/api/patients/" + patientB.getId())
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isForbidden());

        // Verify patient B still exists in MySQL
        assertEquals(true, patientRepository.existsById(patientB.getId()));
    }

    @Test
    @DisplayName("Scenario 7: Hospital A user cannot create a conversion under Hospital B")
    void test7_HospitalAUserCannotCreateConversionUnderHospitalB() throws Exception {
        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_HL7_MSG);

        // Client attempts to create conversion using Hospital A token
        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify the persisted conversion is strictly owned by Hospital A, not Hospital B
        ConversionEntity saved = conversionRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalA.getId()).get(0);
        assertNotNull(saved);
        assertEquals(hospitalA.getId(), saved.getHospital().getId());

        // Verify Hospital B's conversion count did not change
        assertEquals(1, conversionRepository.countByHospitalId(hospitalB.getId()));
    }

    @Test
    @DisplayName("Scenario 8: Hospital A admin cannot create or move a user into Hospital B")
    void test8_HospitalAAdminCannotCreateOrMoveUserIntoHospitalB() throws Exception {
        // Hospital A admin attempts to create user in Hospital B by setting hospitalId = hospitalB.getId()
        CreateUserRequest request = new CreateUserRequest("intruder_b", "Password123!", "USER", hospitalB.getId());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot create users for another hospital")));
    }

    @Test
    @DisplayName("Scenario 9: A user without a hospital assignment cannot access hospital-specific data")
    void test9_UserWithoutHospitalAssignmentCannotAccessHospitalData() throws Exception {
        // User with no hospital assignment attempts to view conversion history
        mockMvc.perform(get("/api/hl7/history")
                        .header("Authorization", "Bearer " + tokenNone))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned hospital")));

        // User with no hospital assignment attempts to perform conversion
        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_HL7_MSG);
        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + tokenNone)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assigned hospital")));
    }

    @Test
    @DisplayName("Scenario 10: An unauthenticated request receives 401 where authentication is required")
    void test10_UnauthenticatedRequestReceives401() throws Exception {
        mockMvc.perform(get("/api/hl7/history"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 11: Existing login continues working and returns valid JWT")
    void test11_ExistingLoginContinuesWorking() throws Exception {
        LoginRequest request = new LoginRequest("user_a1", "UserA1Password!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Scenario 12: Existing HL7-to-FHIR conversion works for an assigned hospital user")
    void test12_ConversionWorksForAssignedHospitalUser() throws Exception {
        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_HL7_MSG);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.fhirOutput").isNotEmpty())
                .andExpect(jsonPath("$.messageControlId").value("MSG-A-999"));
    }

    @Test
    @DisplayName("Scenario 13: Existing History API returns only the authenticated user's hospital data")
    void test13_HistoryApiReturnsOnlyAuthenticatedHospitalData() throws Exception {
        mockMvc.perform(get("/api/hl7/history")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].hospitalId", everyItem(is(hospitalA.getId().intValue()))));

        mockMvc.perform(get("/api/hl7/history")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].hospitalId", everyItem(is(hospitalB.getId().intValue()))));
    }

    @Test
    @DisplayName("Scenario 14: Dashboard statistics are strictly hospital-scoped")
    void test14_DashboardStatisticsAreHospitalScoped() throws Exception {
        // Hospital A stats
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalName").value("Hospital Alpha"))
                .andExpect(jsonPath("$.hospitalCode").value("HOSP_A"))
                .andExpect(jsonPath("$.totalConversions").value(1))
                .andExpect(jsonPath("$.patientsProcessed").value(1));

        // Hospital B stats
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalName").value("Hospital Beta"))
                .andExpect(jsonPath("$.hospitalCode").value("HOSP_B"))
                .andExpect(jsonPath("$.totalConversions").value(1))
                .andExpect(jsonPath("$.patientsProcessed").value(1));
    }

    @Test
    @DisplayName("Scenario 15: Hospital A administrator cannot obtain platform-wide data unless explicitly authorized")
    void test15_HospitalAAdminCannotObtainPlatformWideData() throws Exception {
        // Hospital A admin queries /api/hospitals - receives ONLY Hospital A
        mockMvc.perform(get("/api/hospitals")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("HOSP_A"));

        // Platform admin queries /api/hospitals - receives all hospitals
        mockMvc.perform(get("/api/hospitals")
                        .header("Authorization", "Bearer " + tokenPlatformAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Scenario 16: Hospital Admin cannot update user in another hospital or reassign hospital")
    void test16_HospitalAdminCannotUpdateUserInAnotherHospital() throws Exception {
        // Hospital A admin attempts to update Hospital B user -> rejected with 404 (conceals other tenant user)
        UpdateUserRequest crossUpdateRequest = new UpdateUserRequest("USER", hospitalA.getId(), false);
        mockMvc.perform(put("/api/users/" + userB1.getId())
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossUpdateRequest)))
                .andExpect(status().isNotFound());

        // Hospital A admin attempts to reassign Hospital A user to Hospital B -> rejected with 403
        UpdateUserRequest reassignRequest = new UpdateUserRequest("USER", hospitalB.getId(), true);
        mockMvc.perform(put("/api/users/" + userA1.getId())
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reassignRequest)))
                .andExpect(status().isForbidden());

        // Platform Admin CAN update and assign hospital
        UpdateUserRequest platformUpdateRequest = new UpdateUserRequest("ADMIN", hospitalA.getId(), true);
        mockMvc.perform(put("/api/users/" + userNone.getId())
                        .header("Authorization", "Bearer " + tokenPlatformAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(platformUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalId").value(hospitalA.getId()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
