package com.example.healthcare.integration;

import com.example.healthcare.dto.auth.LoginRequest;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.entity.User;
import com.example.healthcare.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.example.healthcare.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.example.healthcare.repository.HospitalRepository hospitalRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        com.example.healthcare.entity.Hospital testHospital = hospitalRepository.findByCode("TEST_AUTH_HOSP").orElseGet(() ->
                hospitalRepository.save(new com.example.healthcare.entity.Hospital("Test Auth Hospital", "TEST_AUTH_HOSP", "auth@test.org", "555-3333", "300 Auth Way", true)));

        userRepository.findByUsername("admin").ifPresent(admin -> {
            admin.setHospital(testHospital);
            userRepository.save(admin);
        });
    }

    private static final String SYNTHETIC_ADT_A01 =
            "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG0001|P|2.5\r" +
            "PID|||P1001||Sharma^Rahul||19950510|M\r" +
            "PV1||I|WARD-101\r" +
            "OBX|1|NM|GLUCOSE^Blood Glucose||105|mg/dL|70-110|N";

    // ==========================================
    // 1. PUBLIC ENDPOINTS
    // ==========================================

    @Test
    @DisplayName("GET /api/health should remain publicly accessible without authentication")
    void healthEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("POST /api/auth/login should be publicly accessible")
    void loginEndpointShouldBePublic() throws Exception {
        LoginRequest request = new LoginRequest("admin", "admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // ==========================================
    // 2. LOGIN AUTHENTICATION
    // ==========================================

    @Test
    @DisplayName("POST /api/auth/login should authenticate seeded admin and return valid JWT")
    void shouldLoginSuccessfullyWithSeededAdmin() throws Exception {
        LoginRequest request = new LoginRequest("admin", "admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = responseNode.get("token").asText();
        assertNotNull(token);
        assertNotNull(jwtService.extractUsername(token));
    }

    @Test
    @DisplayName("POST /api/auth/login with wrong password should return 401 Unauthorized")
    void shouldReturn401ForIncorrectPassword() throws Exception {
        LoginRequest request = new LoginRequest("admin", "wrong_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/auth/login with non-existent user should return 401 Unauthorized")
    void shouldReturn401ForUnknownUser() throws Exception {
        LoginRequest request = new LoginRequest("non_existent_user", "some_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ==========================================
    // 3. PROTECTED ENDPOINT & JWT VALIDATION
    // ==========================================

    @Test
    @DisplayName("POST /api/hl7/convert WITHOUT Authorization header should return 401 Unauthorized")
    void shouldReturn401WhenMissingJwt() throws Exception {
        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_ADT_A01);

        mockMvc.perform(post("/api/hl7/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/hl7/convert WITH invalid/malformed JWT should return 401 Unauthorized")
    void shouldReturn401WhenInvalidJwt() throws Exception {
        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_ADT_A01);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer invalid.malformed.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/hl7/convert WITH expired JWT should return 401 Unauthorized")
    void shouldReturn401WhenExpiredJwt() throws Exception {
        // Issue an expired token
        JwtService expiredJwtService = new JwtService("healthcare-interop-engine-secure-development-jwt-secret-key-32bytes-min", -5000);
        String expiredToken = expiredJwtService.generateToken("admin", "ROLE_ADMIN");

        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_ADT_A01);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/hl7/convert WITH valid JWT should return 200 OK and perform conversion")
    void shouldReturn200WhenValidJwtProvided() throws Exception {
        // 1. Login to get token
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = responseNode.get("token").asText();

        // 2. Call protected convert endpoint
        HL7ConvertRequest convertRequest = new HL7ConvertRequest(SYNTHETIC_ADT_A01);
        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convertRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.patientId").value("P1001"))
                .andExpect(jsonPath("$.fhirOutput").isNotEmpty());
    }

    // ==========================================
    // 4. PASSWORD HASHING & PHI PRIVACY
    // ==========================================

    @Test
    @DisplayName("Admin password stored in database must be BCrypt hashed and never plaintext")
    void adminPasswordMustBeBcryptHashed() {
        var adminOpt = userRepository.findByUsername("admin");
        org.junit.jupiter.api.Assertions.assertTrue(adminOpt.isPresent(), "Admin user must be seeded");

        var admin = adminOpt.get();
        org.junit.jupiter.api.Assertions.assertNotEquals("admin123", admin.getPassword(), "Password must NEVER be plaintext");
        org.junit.jupiter.api.Assertions.assertTrue(admin.getPassword().startsWith("$2a$") || admin.getPassword().startsWith("$2b$"),
                "Password must be a valid BCrypt hash");
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("admin123", admin.getPassword()),
                "BCrypt password must match admin123");
    }

    @Test
    @DisplayName("JWT claims must not contain PHI or complete HL7 messages")
    void jwtMustNotContainPhi() {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");

        String username = jwtService.extractUsername(token);
        String role = jwtService.extractRole(token);

        org.junit.jupiter.api.Assertions.assertEquals("admin", username);
        org.junit.jupiter.api.Assertions.assertEquals("ROLE_ADMIN", role);

        // Verify claims do not contain any patient identifiers or message text
        org.junit.jupiter.api.Assertions.assertFalse(token.contains("Sharma"));
        org.junit.jupiter.api.Assertions.assertFalse(token.contains("Rahul"));
        org.junit.jupiter.api.Assertions.assertFalse(token.contains("P1001"));
        org.junit.jupiter.api.Assertions.assertFalse(token.contains("GLUCOSE"));
        org.junit.jupiter.api.Assertions.assertFalse(token.contains("MSH|"));
    }

    // ==========================================
    // 5. USER CREATION & ROLE AUTHORIZATION
    // ==========================================

    @Test
    @DisplayName("POST /api/users without authentication should return 401 Unauthorized")
    void shouldReturn401WhenCreatingUserWithoutAuth() throws Exception {
        com.example.healthcare.dto.user.CreateUserRequest request =
                new com.example.healthcare.dto.user.CreateUserRequest("newuser", "Password123", "USER");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/users with non-admin role (ROLE_USER) should return 403 Forbidden")
    void shouldReturn403WhenNonAdminCreatesUser() throws Exception {
        // Save standard user in database so userDetailsService can load it
        userRepository.save(new User("regularuser", passwordEncoder.encode("password123"), "USER", true));
        String userToken = jwtService.generateToken("regularuser", "ROLE_USER");

        com.example.healthcare.dto.user.CreateUserRequest request =
                new com.example.healthcare.dto.user.CreateUserRequest("anotheruser", "Password123", "USER");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: insufficient permissions"));
    }

    @Test
    @DisplayName("POST /api/users with admin JWT should create user, return 201, and permit login with new credentials")
    void shouldCreateUserAndAllowSubsequentLogin() throws Exception {
        // 1. Obtain admin token
        String adminToken = jwtService.generateToken("admin", "ROLE_ADMIN");

        // 2. Create new user
        com.example.healthcare.dto.user.CreateUserRequest request =
                new com.example.healthcare.dto.user.CreateUserRequest("clinicaldoc", "DoctorPass123", "USER");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("clinicaldoc"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // 3. Verify user in database has BCrypt hashed password
        var savedOpt = userRepository.findByUsername("clinicaldoc");
        org.junit.jupiter.api.Assertions.assertTrue(savedOpt.isPresent());
        var savedUser = savedOpt.get();
        org.junit.jupiter.api.Assertions.assertNotEquals("DoctorPass123", savedUser.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("DoctorPass123", savedUser.getPassword()));

        // 4. Authenticate using newly created user credentials
        LoginRequest loginRequest = new LoginRequest("clinicaldoc", "DoctorPass123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/users with duplicate username should return 409 Conflict")
    void shouldReturn409ForDuplicateUsername() throws Exception {
        String adminToken = jwtService.generateToken("admin", "ROLE_ADMIN");

        // "admin" already exists from seeding
        com.example.healthcare.dto.user.CreateUserRequest request =
                new com.example.healthcare.dto.user.CreateUserRequest("admin", "AnotherPassword123", "ADMIN");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username 'admin' is already taken"));
    }
}
