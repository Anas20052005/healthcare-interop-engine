package com.example.healthcare.integration;

import com.example.healthcare.dto.auth.LoginRequest;
import com.example.healthcare.dto.auth.LoginResponse;
import com.example.healthcare.dto.destination.DestinationCreateRequest;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.dto.hl7.HL7ConvertResponse;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.DeliveryResultRepository;
import com.example.healthcare.repository.DestinationRepository;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.PatientRepository;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.service.DestinationService;
import com.example.healthcare.service.FhirDeliveryService;
import com.example.healthcare.service.HL7ConversionService;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DashboardStatisticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversionRepository conversionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DeliveryResultRepository deliveryResultRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private DestinationService destinationService;

    @Autowired
    private HL7ConversionService conversionService;

    @Autowired
    private FhirDeliveryService deliveryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private String tokenUserA;
    private String tokenUserB;

    private static final String HL7_MESSAGE_A =
            "MSH|^~\\&|EPIC|HOSP_A|FACILITY|DEST|20261011120000||ADT^A01|MSG-DASH-01|P|2.5\r" +
            "PID|1||PAT-DASH-A1^^^HOSP_A||DOE^JANE||19850101|F|||123 MAIN^^METROPOLIS^NY^10001\r" +
            "PV1|1|I|ICU^101^1\r" +
            "OBX|1|NM|883-9^GLUCOSE||99|mg/dL|70-105|N|||F";

    private static final String HL7_MESSAGE_B =
            "MSH|^~\\&|CERNER|HOSP_B|FACILITY|DEST|20261011120500||ADT^A01|MSG-DASH-02|P|2.5\r" +
            "PID|1||PAT-DASH-B1^^^HOSP_B||SMITH^JOHN||19750512|M|||456 BROADWAY^^GOTHAM^NY^10002\r" +
            "PV1|1|I|MED^202^2\r" +
            "OBX|1|NM|883-9^GLUCOSE||120|mg/dL|70-105|H|||F";

    @Autowired
    private com.example.healthcare.repository.AuditLogRepository auditLogRepository;

    @Autowired
    private com.example.healthcare.repository.EncounterRepository encounterRepository;

    @Autowired
    private com.example.healthcare.repository.ObservationRepository observationRepository;

    @BeforeEach
    void setUp() throws Exception {
        deliveryResultRepository.deleteAll();
        conversionRepository.deleteAll();
        observationRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();
        destinationRepository.deleteAll();

        // 1. Create or retrieve Hospital A
        hospitalA = hospitalRepository.findByCode("HOSP_A").orElseGet(() ->
                hospitalRepository.save(new Hospital("Hospital Alpha", "HOSP_A", "alpha@test.com", "555-0101", "100 Alpha Way", true)));

        User userA = userRepository.findByUsername("doctor_a").orElse(null);
        if (userA == null) {
            userA = new User("doctor_a", passwordEncoder.encode("Pass123!"), "DOCTOR", hospitalA, true);
        } else {
            userA.setPassword(passwordEncoder.encode("Pass123!"));
            userA.setHospital(hospitalA);
            userA.setEnabled(true);
        }
        userRepository.save(userA);

        // 2. Create or retrieve Hospital B
        hospitalB = hospitalRepository.findByCode("HOSP_B").orElseGet(() ->
                hospitalRepository.save(new Hospital("Hospital Beta", "HOSP_B", "beta@test.com", "555-0202", "200 Beta Way", true)));

        User userB = userRepository.findByUsername("doctor_b").orElse(null);
        if (userB == null) {
            userB = new User("doctor_b", passwordEncoder.encode("Pass123!"), "DOCTOR", hospitalB, true);
        } else {
            userB.setPassword(passwordEncoder.encode("Pass123!"));
            userB.setHospital(hospitalB);
            userB.setEnabled(true);
        }
        userRepository.save(userB);

        // Authenticate userA and userB
        tokenUserA = authenticate("doctor_a", "Pass123!");
        tokenUserB = authenticate("doctor_b", "Pass123!");
    }

    private String authenticate(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse response = objectMapper.readValue(res.getResponse().getContentAsString(), LoginResponse.class);
        return response.getToken();
    }

    @Test
    @DisplayName("Empty database returns zero counts and 0.0 success rate without errors")
    void testEmptyDatabaseReturnsZeroCounts() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalName").value("Hospital Alpha"))
                .andExpect(jsonPath("$.hospitalCode").value("HOSP_A"))
                .andExpect(jsonPath("$.totalConversions").value(0))
                .andExpect(jsonPath("$.successfulConversions").value(0))
                .andExpect(jsonPath("$.failedConversions").value(0))
                .andExpect(jsonPath("$.conversionSuccessRate").value(0.0))
                .andExpect(jsonPath("$.patientsProcessed").value(0))
                .andExpect(jsonPath("$.encountersProcessed").value(0))
                .andExpect(jsonPath("$.observationsProcessed").value(0))
                .andExpect(jsonPath("$.totalDeliveries").value(0))
                .andExpect(jsonPath("$.successfulDeliveries").value(0))
                .andExpect(jsonPath("$.failedDeliveries").value(0))
                .andExpect(jsonPath("$.deliverySuccessRate").value(0.0))
                .andExpect(jsonPath("$.totalDestinations").value(0))
                .andExpect(jsonPath("$.enabledDestinations").value(0))
                .andExpect(jsonPath("$.engineStatus").value("OPERATIONAL"))
                .andExpect(jsonPath("$.databaseStatus").value("UP"))
                .andExpect(jsonPath("$.recentConversions", hasSize(0)))
                .andExpect(jsonPath("$.recentDeliveries", hasSize(0)));
    }

    @Test
    @DisplayName("Real conversions, deliveries, and destinations calculate exact statistics strictly scoped by tenant")
    void testRealConversionsAndDeliveriesCountedCorrectly() throws Exception {
        // 1. Hospital A performs conversion
        MvcResult convResult = mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HL7ConvertRequest(HL7_MESSAGE_A))))
                .andExpect(status().isOk())
                .andReturn();
        HL7ConvertResponse convA = objectMapper.readValue(convResult.getResponse().getContentAsString(), HL7ConvertResponse.class);

        // 2. Hospital A creates a mock destination
        DestinationCreateRequest destReq = new DestinationCreateRequest(
                "Hospital A Internal Mock",
                "http://localhost:8080/api/mock-fhir",
                "R4",
                com.example.healthcare.entity.DestinationAuthType.NONE,
                null,
                true
        );
        destReq.setMock(true);
        destReq.setHospitalId(hospitalA.getId());
        MvcResult destResult = mockMvc.perform(post("/api/destinations")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(destReq)))
                .andExpect(status().isCreated())
                .andReturn();
        com.example.healthcare.dto.destination.DestinationResponse destA =
                objectMapper.readValue(destResult.getResponse().getContentAsString(), com.example.healthcare.dto.destination.DestinationResponse.class);

        // 3. Deliver conversion to destination
        com.example.healthcare.dto.delivery.SendConversionRequest sendReq =
                new com.example.healthcare.dto.delivery.SendConversionRequest(destA.getId());
        mockMvc.perform(post("/api/hl7/conversions/" + convA.getConversionId() + "/send")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 4. Hospital B performs conversion
        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HL7ConvertRequest(HL7_MESSAGE_B))))
                .andExpect(status().isOk());

        // 5. Verify Hospital A dashboard stats: exactly 1 conversion, 1 patient, 1 observation, 1 delivery, 1 destination
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalName").value("Hospital Alpha"))
                .andExpect(jsonPath("$.totalConversions").value(1))
                .andExpect(jsonPath("$.successfulConversions").value(1))
                .andExpect(jsonPath("$.failedConversions").value(0))
                .andExpect(jsonPath("$.conversionSuccessRate").value(100.0))
                .andExpect(jsonPath("$.patientsProcessed").value(1))
                .andExpect(jsonPath("$.observationsProcessed").value(1))
                .andExpect(jsonPath("$.totalDeliveries").value(1))
                .andExpect(jsonPath("$.successfulDeliveries").value(1))
                .andExpect(jsonPath("$.failedDeliveries").value(0))
                .andExpect(jsonPath("$.deliverySuccessRate").value(100.0))
                .andExpect(jsonPath("$.totalDestinations").value(1))
                .andExpect(jsonPath("$.enabledDestinations").value(1))
                .andExpect(jsonPath("$.recentConversions", hasSize(1)))
                .andExpect(jsonPath("$.recentDeliveries", hasSize(1)));

        // 6. Verify Hospital B dashboard stats: exactly 1 conversion, 0 deliveries, 0 destinations
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitalName").value("Hospital Beta"))
                .andExpect(jsonPath("$.totalConversions").value(1))
                .andExpect(jsonPath("$.totalDeliveries").value(0))
                .andExpect(jsonPath("$.successfulDeliveries").value(0))
                .andExpect(jsonPath("$.totalDestinations").value(0))
                .andExpect(jsonPath("$.recentConversions", hasSize(1)))
                .andExpect(jsonPath("$.recentDeliveries", hasSize(0)));
    }

    @Test
    @DisplayName("Unauthenticated request to dashboard stats returns 401 Unauthorized")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }
}
