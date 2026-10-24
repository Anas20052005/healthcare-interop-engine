package com.example.healthcare.integration;

import com.example.healthcare.dto.delivery.SendConversionRequest;
import com.example.healthcare.dto.destination.DestinationCreateRequest;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.dto.hl7.HL7ConvertResponse;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.DestinationAuthType;
import com.example.healthcare.entity.DestinationEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.DeliveryResultRepository;
import com.example.healthcare.repository.DestinationRepository;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.JwtService;
import com.example.healthcare.service.MockFhirService;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FhirDeliveryIntegrationTest {

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
    private ConversionRepository conversionRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private DeliveryResultRepository deliveryResultRepository;

    @Autowired
    private MockFhirService mockFhirService;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    private DestinationEntity mockDestination;
    private DestinationEntity hospitalBDestination;

    private static final String SYNTHETIC_HL7_MSG =
            "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610101200||ADT^A01|MSG-PRIORITY5-001|P|2.5\r" +
            "PID|||PAT-P5-99||Verma^Anita||19920820|F\r" +
            "PV1||I|ICU-CARDIO\r" +
            "OBX|1|NM|GLUCOSE^Blood Glucose||105|mg/dL|70-110|N";

    @BeforeEach
    void setUp() {
        deliveryResultRepository.deleteAll();
        destinationRepository.deleteAll();
        conversionRepository.deleteAll();
        mockFhirService.clearReceivedResources();

        hospitalA = hospitalRepository.save(new Hospital("Alpha Medical Center", "ALPHA_MED_P5"));
        hospitalB = hospitalRepository.save(new Hospital("Beta Health System", "BETA_SYS_P5"));

        userA = userRepository.save(new User("doc_alpha_p5", passwordEncoder.encode("password"), "USER", hospitalA, true));
        userB = userRepository.save(new User("doc_beta_p5", passwordEncoder.encode("password"), "USER", hospitalB, true));

        tokenA = jwtService.generateToken(userA.getUsername(), userA.getRole(), hospitalA.getId());
        tokenB = jwtService.generateToken(userB.getUsername(), userB.getRole(), hospitalB.getId());

        // Create platform mock destination
        mockDestination = destinationRepository.save(new DestinationEntity(
                "Local Mock FHIR Destination",
                "http://localhost:8080/api/mock-fhir",
                "R4",
                DestinationAuthType.NONE,
                true,
                true,
                null // Platform-wide
        ));

        // Create Hospital B private destination
        hospitalBDestination = destinationRepository.save(new DestinationEntity(
                "Hospital B Private Destination",
                "https://fhir.hospital-b.org/r4",
                "R4",
                DestinationAuthType.NONE,
                true,
                false,
                hospitalB
        ));
    }

    @Test
    @DisplayName("End-to-End: Convert HL7 -> Send to Mock FHIR Server -> Success recorded in DB and inspectable in mock buffer")
    void testConvertAndSendToMockDestinationSuccess() throws Exception {
        // Step 1: Convert HL7 to FHIR R4
        HL7ConvertRequest convertRequest = new HL7ConvertRequest(SYNTHETIC_HL7_MSG);
        String convertResult = mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convertRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.fhirOutput").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        HL7ConvertResponse convertResponse = objectMapper.readValue(convertResult, HL7ConvertResponse.class);
        Long conversionId = convertResponse.getConversionId();
        assertNotNull(conversionId);

        // Verify conversionEntity stored fhirPayload
        ConversionEntity storedConversion = conversionRepository.findById(conversionId).orElseThrow();
        assertNotNull(storedConversion.getFhirPayload());

        // Step 2: Explicitly Send conversion to Local Mock FHIR Server
        SendConversionRequest sendRequest = new SendConversionRequest(mockDestination.getId());
        mockMvc.perform(post("/api/hl7/conversions/" + conversionId + "/send")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.httpStatusCode").value(201))
                .andExpect(jsonPath("$.conversionId").value(conversionId))
                .andExpect(jsonPath("$.destinationId").value(mockDestination.getId()));

        // Step 3: Verify Delivery Result persisted in database
        assertEquals(1, deliveryResultRepository.findByConversionIdOrderByAttemptTimestampDesc(conversionId).size());

        // Step 4: Verify Mock FHIR server received the resource
        mockMvc.perform(get("/api/mock-fhir/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].resourceType").value("Bundle"));

        // Step 5: Verify delivery history retrieval endpoint
        mockMvc.perform(get("/api/hl7/conversions/" + conversionId + "/deliveries")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Tenant Isolation: Hospital A user cannot send Hospital B's conversion (HTTP 403)")
    void testTenantIsolationConversionSendDenied() throws Exception {
        // Create conversion for Hospital B
        ConversionEntity conversionB = conversionRepository.save(new ConversionEntity(
                "CTRL-B-1", "ADT^A01", "SUCCESS", "PAT-B-1", null, hospitalB
        ));
        conversionB.setFhirPayload("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}");
        conversionRepository.save(conversionB);

        SendConversionRequest sendRequest = new SendConversionRequest(mockDestination.getId());

        mockMvc.perform(post("/api/hl7/conversions/" + conversionB.getId() + "/send")
                        .header("Authorization", "Bearer " + tokenA) // User from Hospital A
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Tenant Isolation: Hospital A user cannot send to Hospital B's private destination (HTTP 403)")
    void testTenantIsolationDestinationSendDenied() throws Exception {
        // Create conversion for Hospital A
        ConversionEntity conversionA = conversionRepository.save(new ConversionEntity(
                "CTRL-A-1", "ADT^A01", "SUCCESS", "PAT-A-1", null, hospitalA
        ));
        conversionA.setFhirPayload("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}");
        conversionRepository.save(conversionA);

        // Attempt to send to Hospital B's private destination
        SendConversionRequest sendRequest = new SendConversionRequest(hospitalBDestination.getId());

        mockMvc.perform(post("/api/hl7/conversions/" + conversionA.getId() + "/send")
                        .header("Authorization", "Bearer " + tokenA) // User from Hospital A
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Creating and listing destinations enforces tenant visibility")
    void testDestinationConfigurationAndListing() throws Exception {
        DestinationCreateRequest createRequest = new DestinationCreateRequest(
                "Hospital A Regional FHIR",
                "https://fhir.regional-hospital.org/r4",
                "R4",
                DestinationAuthType.BEARER_TOKEN,
                "super-secret-token",
                true
        );

        // Create as User A
        mockMvc.perform(post("/api/destinations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hospital A Regional FHIR"))
                .andExpect(jsonPath("$.hasAuthToken").value(true));

        // List destinations as User A (should see Hospital A's destination and Platform-wide destination)
        mockMvc.perform(get("/api/destinations")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // List destinations as User B (should see Hospital B's destination and Platform-wide destination, but NOT Hospital A's)
        mockMvc.perform(get("/api/destinations")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Unauthenticated delivery requests are rejected with HTTP 401")
    void testUnauthenticatedDeliveryRejected() throws Exception {
        SendConversionRequest sendRequest = new SendConversionRequest(mockDestination.getId());

        mockMvc.perform(post("/api/hl7/conversions/1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sending to disabled destination is rejected with HTTP 400")
    void testSendToDisabledDestinationRejected() throws Exception {
        DestinationEntity disabledDest = destinationRepository.save(new DestinationEntity(
                "Disabled Destination",
                "https://fhir.disabled.org/r4",
                "R4",
                DestinationAuthType.NONE,
                false, // disabled
                false,
                hospitalA
        ));

        ConversionEntity conv = conversionRepository.save(new ConversionEntity(
                "CTRL-DIS-1", "ADT^A01", "SUCCESS", "PAT-1", null, hospitalA
        ));
        conv.setFhirPayload("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}");
        conversionRepository.save(conv);

        SendConversionRequest sendRequest = new SendConversionRequest(disabledDest.getId());

        mockMvc.perform(post("/api/hl7/conversions/" + conv.getId() + "/send")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("disabled")));
    }

    @Test
    @DisplayName("Sending with non-existent conversion ID returns HTTP 404")
    void testSendNonExistentConversionReturns404() throws Exception {
        SendConversionRequest sendRequest = new SendConversionRequest(mockDestination.getId());

        mockMvc.perform(post("/api/hl7/conversions/999999/send")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sending with non-existent destination ID returns HTTP 404")
    void testSendNonExistentDestinationReturns404() throws Exception {
        ConversionEntity conv = conversionRepository.save(new ConversionEntity(
                "CTRL-NONE-1", "ADT^A01", "SUCCESS", "PAT-1", null, hospitalA
        ));
        conv.setFhirPayload("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}");
        conversionRepository.save(conv);

        SendConversionRequest sendRequest = new SendConversionRequest(999999L);

        mockMvc.perform(post("/api/hl7/conversions/" + conv.getId() + "/send")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant Isolation: Hospital A user cannot view Hospital B conversion delivery history (HTTP 403)")
    void testTenantIsolationConversionDeliveriesDenied() throws Exception {
        ConversionEntity convB = conversionRepository.save(new ConversionEntity(
                "CTRL-B-HIST", "ADT^A01", "SUCCESS", "PAT-B-HIST", null, hospitalB
        ));
        convB.setFhirPayload("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}");
        conversionRepository.save(convB);

        deliveryResultRepository.save(new com.example.healthcare.entity.DeliveryResultEntity(
                convB.getId(), mockDestination.getId(), "Mock Dest", java.time.LocalDateTime.now(),
                "SUCCESS", 201, null, "Mock accepted", hospitalB
        ));

        // User A attempts to view Hospital B's conversion deliveries
        mockMvc.perform(get("/api/hl7/conversions/" + convB.getId() + "/deliveries")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/hl7/deliveries/{id} returns delivery details for authorized hospital and enforces tenant isolation")
    void testGetDeliveryByIdEndpoint() throws Exception {
        ConversionEntity convA = conversionRepository.save(new ConversionEntity(
                "CTRL-A-DELIV", "ADT^A01", "SUCCESS", "PAT-A-DELIV", null, hospitalA
        ));
        convA.setFhirPayload("{\"resourceType\": \"Bundle\", \"type\": \"collection\"}");
        conversionRepository.save(convA);

        var deliveryA = deliveryResultRepository.save(new com.example.healthcare.entity.DeliveryResultEntity(
                convA.getId(), mockDestination.getId(), "Mock Dest", java.time.LocalDateTime.now(),
                "SUCCESS", 201, null, "Mock FHIR server: accepted Bundle (1 entries)", hospitalA
        ));

        // User A succeeds
        mockMvc.perform(get("/api/hl7/deliveries/" + deliveryA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deliveryA.getId()))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.httpStatusCode").value(201))
                .andExpect(jsonPath("$.responseSummary").value(org.hamcrest.Matchers.containsString("Mock FHIR server")));

        // User B is denied access (HTTP 403)
        mockMvc.perform(get("/api/hl7/deliveries/" + deliveryA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Unauthenticated access rejected (HTTP 401)
        mockMvc.perform(get("/api/hl7/deliveries/" + deliveryA.getId()))
                .andExpect(status().isUnauthorized());
    }
}
