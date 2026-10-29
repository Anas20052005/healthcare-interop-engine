package com.example.healthcare.integration;

import com.example.healthcare.dto.auth.LoginRequest;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.demo.seed=true",
        "app.demo.username=demo_recruiter",
        "app.demo.password=RecruiterDemoTest123!",
        "app.demo.hospital-name=Recruiter Demo Hospital",
        "app.demo.hospital-code=DEMO-REC"
})
class DemoUserSecurityIntegrationTest {

    private static final String SYNTHETIC_HL7_MSG =
            "MSH|^~\\&|HIS|DEMO-REC|FHIR|SERVER|202610291200||ADT^A01|DEMO-MSG-001|P|2.5\r" +
            "PID|||DEMO-PAT-001||Recruiter^Candidate||19920315|M\r" +
            "PV1||I|DEMO-WARD||||||||||||||||||||||||||||||||202610291200\r" +
            "OBX|1|NM|GLUCOSE^Blood Glucose||98|mg/dL|70-110|N";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private ConversionRepository conversionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Hospital demoHospital;
    private Hospital otherHospital;
    private User demoUser;
    private String demoToken;

    @BeforeEach
    void setUp() {
        demoHospital = hospitalRepository.findByCode("DEMO-REC").orElseGet(() ->
                hospitalRepository.save(new Hospital("Recruiter Demo Hospital", "DEMO-REC", "demo@hospital.org", "555-0199", "100 Demo Way", true)));

        otherHospital = hospitalRepository.findByCode("OTHER-HOSP").orElseGet(() ->
                hospitalRepository.save(new Hospital("Other Regional Hospital", "OTHER-HOSP", "info@otherhosp.org", "555-0988", "200 Other Ave", true)));

        demoUser = userRepository.findByUsername("demo_recruiter").orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername("demo_recruiter");
            newUser.setPassword(passwordEncoder.encode("RecruiterDemoTest123!"));
            newUser.setRole("USER");
            newUser.setEnabled(true);
            newUser.setHospital(demoHospital);
            return userRepository.save(newUser);
        });

        // Ensure user has correct credentials, role and hospital in case it pre-existed
        demoUser.setPassword(passwordEncoder.encode("RecruiterDemoTest123!"));
        demoUser.setRole("USER");
        demoUser.setEnabled(true);
        demoUser.setHospital(demoHospital);
        demoUser = userRepository.save(demoUser);

        demoToken = jwtService.generateToken(demoUser.getUsername(), demoUser.getRole(), demoHospital.getId());
    }

    @Test
    @DisplayName("Demo user can authenticate successfully via /api/auth/login and receives USER role token")
    void demoUserCanLoginSuccessfully() throws Exception {
        LoginRequest loginRequest = new LoginRequest("demo_recruiter", "RecruiterDemoTest123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = responseNode.get("token").asText();
        assertNotNull(token);

        assertEquals("demo_recruiter", jwtService.extractUsername(token));
        assertEquals("ROLE_USER", jwtService.extractRole(token));
        assertEquals(demoHospital.getId(), jwtService.extractHospitalId(token));
        assertFalse(jwtService.extractRole(token).contains("ADMIN"), "Demo recruiter must never have ADMIN role");
    }

    @Test
    @DisplayName("Demo user is blocked with 401 when invalid password is provided")
    void demoUserFailsWithWrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest("demo_recruiter", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Demo user can access dashboard statistics for demo hospital")
    void demoUserCanAccessDashboardStats() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + demoToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Demo user can perform HL7 to FHIR message conversion in sandbox")
    void demoUserCanConvertHL7() throws Exception {
        HL7ConvertRequest request = new HL7ConvertRequest();
        request.setHl7Message(SYNTHETIC_HL7_MSG);

        mockMvc.perform(post("/api/hl7/convert")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + demoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.patientId").value("DEMO-PAT-001"));
    }

    @Test
    @DisplayName("Demo user cannot view conversions belonging to other hospitals (Multi-Tenant Isolation)")
    void demoUserCannotAccessOtherHospitalConversions() throws Exception {
        // Seed a conversion for otherHospital
        ConversionEntity otherConversion = new ConversionEntity(
                "OTHER-MSG-999", "ADT^A01", "SUCCESS", "PAT-OTHER-001", null, otherHospital);
        otherConversion.setFhirPayload("{\"resourceType\":\"Bundle\"}");
        otherConversion = conversionRepository.save(otherConversion);

        // Seed a conversion for demoHospital
        ConversionEntity demoConversion = new ConversionEntity(
                "DEMO-MSG-111", "ADT^A01", "SUCCESS", "PAT-DEMO-001", null, demoHospital);
        demoConversion.setFhirPayload("{\"resourceType\":\"Bundle\"}");
        demoConversion = conversionRepository.save(demoConversion);

        // Fetch history as demo user
        mockMvc.perform(get("/api/hl7/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + demoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].messageControlId", not(hasItem("OTHER-MSG-999"))));

        // Attempting direct access to other hospital's conversion returns 403 or 404
        mockMvc.perform(get("/api/hl7/history/" + otherConversion.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + demoToken))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() == 403 || result.getResponse().getStatus() == 404,
                        "Demo user must not access another hospital's conversion detail"));
    }

    @Test
    @DisplayName("Demo user is forbidden (403) from accessing admin user management endpoint /api/users")
    void demoUserCannotAccessUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + demoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Demo user is forbidden (403) from creating hospitals via /api/hospitals")
    void demoUserCannotCreateHospitals() throws Exception {
        Hospital newHospital = new Hospital("Rogue Hospital", "ROGUE", "rogue@test.com", "555-0000", "Nowhere", true);

        mockMvc.perform(post("/api/hospitals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + demoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newHospital)))
                .andExpect(status().isForbidden());
    }
}
