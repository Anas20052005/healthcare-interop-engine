package com.example.healthcare.integration;

import com.example.healthcare.dto.hospital.HospitalCreateRequest;
import com.example.healthcare.dto.hospital.HospitalUpdateRequest;
import com.example.healthcare.dto.user.CreateUserRequest;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HospitalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.healthcare.repository.AuditLogRepository auditLogRepository;

    @Autowired
    private com.example.healthcare.repository.DeliveryResultRepository deliveryResultRepository;

    @Autowired
    private com.example.healthcare.repository.DestinationRepository destinationRepository;

    @Autowired
    private com.example.healthcare.repository.ConversionRepository conversionRepository;

    @Autowired
    private com.example.healthcare.repository.ObservationRepository observationRepository;

    @Autowired
    private com.example.healthcare.repository.EncounterRepository encounterRepository;

    @Autowired
    private com.example.healthcare.repository.PatientRepository patientRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("doctor1")) {
            userRepository.save(new User("doctor1", "$2a$10$dummyHashStringForTests", "USER", true));
        }
        adminToken = jwtService.generateToken("admin", "ROLE_ADMIN");
        userToken = jwtService.generateToken("doctor1", "ROLE_USER");
        auditLogRepository.deleteAll();
        deliveryResultRepository.deleteAll();
        destinationRepository.deleteAll();
        conversionRepository.deleteAll();
        observationRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.findAll().forEach(u -> {
            if (u.getHospital() != null) {
                u.setHospital(null);
                userRepository.save(u);
            }
            if (!"admin".equals(u.getUsername()) && !"doctor1".equals(u.getUsername())) {
                userRepository.delete(u);
            }
        });
        hospitalRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/hospitals without authentication should return 401 Unauthorized")
    void shouldRejectUnauthenticatedHospitalCreation() throws Exception {
        HospitalCreateRequest request = new HospitalCreateRequest("Memorial Hospital", "MEM_01");

        mockMvc.perform(post("/api/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/hospitals with ROLE_USER should return 403 Forbidden")
    void shouldForbidNonAdminHospitalCreation() throws Exception {
        HospitalCreateRequest request = new HospitalCreateRequest("Memorial Hospital", "MEM_01");

        mockMvc.perform(post("/api/hospitals")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/hospitals with ROLE_ADMIN should create and persist hospital")
    void shouldCreateHospitalWithAdmin() throws Exception {
        HospitalCreateRequest request = new HospitalCreateRequest(
                "Memorial Hospital",
                "mem_01",
                "info@memorial.org",
                "555-0199",
                "100 Hospital Way",
                true
        );

        mockMvc.perform(post("/api/hospitals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Memorial Hospital"))
                .andExpect(jsonPath("$.code").value("MEM_01"))
                .andExpect(jsonPath("$.enabled").value(true));

        assertTrue(hospitalRepository.existsByCode("MEM_01"));
    }

    @Test
    @DisplayName("POST /api/hospitals should return 409 Conflict when hospital code already exists")
    void shouldRejectDuplicateHospitalCode() throws Exception {
        Hospital existing = new Hospital("First Hospital", "HOSP_CODE", null, null, null, true);
        hospitalRepository.save(existing);

        HospitalCreateRequest request = new HospitalCreateRequest("Second Hospital", "hosp_code");

        mockMvc.perform(post("/api/hospitals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Hospital code 'HOSP_CODE' is already in use"));
    }

    @Test
    @DisplayName("GET /api/hospitals should return 200 OK and list all hospitals for platform administrator")
    void shouldListHospitalsForPlatformAdmin() throws Exception {
        Hospital h1 = new Hospital("Hospital Alpha", "ALPHA", null, null, null, true);
        Hospital h2 = new Hospital("Hospital Beta", "BETA", null, null, null, true);
        hospitalRepository.save(h1);
        hospitalRepository.save(h2);

        mockMvc.perform(get("/api/hospitals")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Hospital Alpha"))
                .andExpect(jsonPath("$[1].name").value("Hospital Beta"));
    }

    @Test
    @DisplayName("GET /api/hospitals/{id} should return 200 OK with hospital details for platform admin")
    void shouldGetHospitalDetails() throws Exception {
        Hospital h = new Hospital("St. Jude", "STJUDE", "info@stjude.org", "555-4321", "Care Lane", true);
        Hospital saved = hospitalRepository.save(h);

        mockMvc.perform(get("/api/hospitals/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("St. Jude"))
                .andExpect(jsonPath("$.code").value("STJUDE"));
    }

    @Test
    @DisplayName("GET /api/hospitals/{id} with invalid ID should return 404 Not Found")
    void shouldReturn404ForNonexistentHospital() throws Exception {
        mockMvc.perform(get("/api/hospitals/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/hospitals/{id} with ROLE_ADMIN should update hospital")
    void shouldUpdateHospitalWithAdmin() throws Exception {
        Hospital h = new Hospital("Original Clinic", "ORIG_01", null, null, null, true);
        Hospital saved = hospitalRepository.save(h);

        HospitalUpdateRequest updateRequest = new HospitalUpdateRequest(
                "Renamed Medical Center",
                "RENAMED_01",
                "new@clinic.org",
                "555-8888",
                "New Address",
                false
        );

        mockMvc.perform(put("/api/hospitals/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Renamed Medical Center"))
                .andExpect(jsonPath("$.code").value("RENAMED_01"))
                .andExpect(jsonPath("$.enabled").value(false));

        Hospital updated = hospitalRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Renamed Medical Center", updated.getName());
        assertEquals("RENAMED_01", updated.getCode());
        assertEquals(false, updated.isEnabled());
    }

    @Test
    @DisplayName("POST /api/users should associate new user with hospital when hospitalId is provided")
    void shouldRegisterUserWithHospitalAssociation() throws Exception {
        Hospital h = new Hospital("Apollo Hospital", "APOLLO_MAIN", null, null, null, true);
        Hospital savedHospital = hospitalRepository.save(h);

        CreateUserRequest request = new CreateUserRequest(
                "dr_sharma",
                "DoctorPass123",
                "USER",
                savedHospital.getId()
        );

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("dr_sharma"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.hospitalId").value(savedHospital.getId()))
                .andExpect(jsonPath("$.hospitalName").value("Apollo Hospital"))
                .andExpect(jsonPath("$.hospitalCode").value("APOLLO_MAIN"));

        User persistedUser = userRepository.findByUsername("dr_sharma").orElseThrow();
        assertNotNull(persistedUser.getHospital());
        assertEquals(savedHospital.getId(), persistedUser.getHospital().getId());
    }

    @Test
    @DisplayName("POST /api/users should return 404 when hospitalId does not exist")
    void shouldRejectUserCreationWithNonexistentHospital() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "dr_unknown",
                "DoctorPass123",
                "USER",
                88888L
        );

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Hospital not found with id: 88888"));
    }

    @Test
    @DisplayName("POST /api/users should return 400 when hospital is disabled")
    void shouldRejectUserCreationWithDisabledHospital() throws Exception {
        Hospital disabled = new Hospital("Closed Center", "CLOSED", null, null, null, false);
        Hospital saved = hospitalRepository.save(disabled);

        CreateUserRequest request = new CreateUserRequest(
                "dr_closed",
                "DoctorPass123",
                "USER",
                saved.getId()
        );

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot assign user to disabled hospital: Closed Center"));
    }
}
