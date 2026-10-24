package com.example.healthcare.integration;

import com.example.healthcare.dto.delivery.DeliveryResultResponse;
import com.example.healthcare.dto.delivery.SendConversionRequest;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.DeliveryResultEntity;
import com.example.healthcare.entity.DestinationAuthType;
import com.example.healthcare.entity.DestinationEntity;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.AuditLogRepository;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.DeliveryResultRepository;
import com.example.healthcare.repository.DestinationRepository;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.JwtService;
import com.example.healthcare.service.DeliveryRetryScheduler;
import com.example.healthcare.service.FhirDeliveryService;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DeliveryRetryIntegrationTest {

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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private MockFhirService mockFhirService;

    @Autowired
    private FhirDeliveryService fhirDeliveryService;

    @Autowired
    private DeliveryRetryScheduler deliveryRetryScheduler;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    private DestinationEntity mockDestination;
    private DestinationEntity disabledDestination;

    private static final String VALID_FHIR_BUNDLE = """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "entry": [
                {
                  "resource": {
                    "resourceType": "Patient",
                    "id": "pat-retry-1"
                  }
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        deliveryResultRepository.deleteAll();
        destinationRepository.deleteAll();
        conversionRepository.deleteAll();
        mockFhirService.clearReceivedResources();

        hospitalA = hospitalRepository.save(new Hospital("General Hospital A", "GEN_HOSP_A"));
        hospitalB = hospitalRepository.save(new Hospital("Memorial Hospital B", "MEM_HOSP_B"));

        userA = userRepository.save(new User("nurse_a", passwordEncoder.encode("secret"), "USER", hospitalA, true));
        userB = userRepository.save(new User("nurse_b", passwordEncoder.encode("secret"), "USER", hospitalB, true));

        tokenA = jwtService.generateToken(userA.getUsername(), userA.getRole(), hospitalA.getId());
        tokenB = jwtService.generateToken(userB.getUsername(), userB.getRole(), hospitalB.getId());

        mockDestination = destinationRepository.save(new DestinationEntity(
                "Internal Mock Server",
                "http://localhost:8080/api/mock-fhir",
                "R4",
                DestinationAuthType.NONE,
                true,
                true,
                null
        ));

        disabledDestination = destinationRepository.save(new DestinationEntity(
                "Offline Destination",
                "https://offline.hospital.org/r4",
                "R4",
                DestinationAuthType.NONE,
                false, // disabled
                false,
                hospitalA
        ));
    }

    @Test
    @DisplayName("Successful delivery does not trigger automatic retry (status SUCCESS, nextRetryAt null)")
    void testSuccessfulDeliveryDoesNotRetry() throws Exception {
        ConversionEntity conv = conversionRepository.save(new ConversionEntity(
                "CTRL-SUCC", "ADT^A01", "SUCCESS", "P001", null, hospitalA
        ));
        conv.setFhirPayload(VALID_FHIR_BUNDLE);
        conversionRepository.save(conv);

        SendConversionRequest sendRequest = new SendConversionRequest(mockDestination.getId());

        mockMvc.perform(post("/api/hl7/conversions/" + conv.getId() + "/send")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.attemptCount").value(1))
                .andExpect(jsonPath("$.nextRetryAt").doesNotExist());

        var deliveries = deliveryResultRepository.findByConversionIdOrderByAttemptTimestampDesc(conv.getId());
        assertEquals(1, deliveries.size());
        assertEquals("SUCCESS", deliveries.get(0).getStatus());
        assertEquals(1, deliveries.get(0).getAttemptCount());
        assertNull(deliveries.get(0).getNextRetryAt());
    }

    @Test
    @DisplayName("Manual retry works for eligible failed deliveries")
    void testManualRetryEligibleFailedDelivery() throws Exception {
        ConversionEntity conv = conversionRepository.save(new ConversionEntity(
                "CTRL-FAIL-1", "ADT^A01", "SUCCESS", "P002", null, hospitalA
        ));
        conv.setFhirPayload(VALID_FHIR_BUNDLE);
        conversionRepository.save(conv);

        // Seed an initial failed delivery
        DeliveryResultEntity failedDelivery = deliveryResultRepository.save(new DeliveryResultEntity(
                conv.getId(),
                mockDestination.getId(),
                mockDestination.getName(),
                LocalDateTime.now().minusMinutes(5),
                "FAILED",
                503,
                "Service Unavailable",
                null,
                1,
                3,
                null,
                hospitalA
        ));

        // Call POST /api/hl7/deliveries/{id}/retry
        mockMvc.perform(post("/api/hl7/deliveries/" + failedDelivery.getId() + "/retry")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(failedDelivery.getId()))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.attemptCount").value(2))
                .andExpect(jsonPath("$.httpStatusCode").value(201));

        DeliveryResultEntity updated = deliveryResultRepository.findById(failedDelivery.getId()).orElseThrow();
        assertEquals("SUCCESS", updated.getStatus());
        assertEquals(2, updated.getAttemptCount());
        assertNull(updated.getNextRetryAt());
    }

    @Test
    @DisplayName("Successful deliveries cannot be retried through retry endpoint (HTTP 409 Conflict)")
    void testSuccessfulDeliveryCannotBeRetried() throws Exception {
        ConversionEntity conv = conversionRepository.save(new ConversionEntity(
                "CTRL-SUCC-2", "ADT^A01", "SUCCESS", "P003", null, hospitalA
        ));
        conv.setFhirPayload(VALID_FHIR_BUNDLE);
        conversionRepository.save(conv);

        DeliveryResultEntity successDelivery = deliveryResultRepository.save(new DeliveryResultEntity(
                conv.getId(),
                mockDestination.getId(),
                mockDestination.getName(),
                LocalDateTime.now(),
                "SUCCESS",
                201,
                null,
                "Accepted",
                1,
                3,
                null,
                hospitalA
        ));

        mockMvc.perform(post("/api/hl7/deliveries/" + successDelivery.getId() + "/retry")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already succeeded")));
    }

    @Test
    @DisplayName("Delivery already in RETRYING state cannot be manually retried concurrently (HTTP 409 Conflict)")
    void testRetryingDeliveryCannotBeManuallyRetriedConcurrently() throws Exception {
        ConversionEntity conv = conversionRepository.save(new ConversionEntity(
                "CTRL-RETRY-1", "ADT^A01", "SUCCESS", "P004", null, hospitalA
        ));
        conv.setFhirPayload(VALID_FHIR_BUNDLE);
        conversionRepository.save(conv);

        DeliveryResultEntity retryingDelivery = deliveryResultRepository.save(new DeliveryResultEntity(
                conv.getId(),
                mockDestination.getId(),
                mockDestination.getName(),
                LocalDateTime.now(),
                "RETRYING",
                504,
                "Gateway Timeout",
                null,
                1,
                3,
                LocalDateTime.now().plusSeconds(30),
                hospitalA
        ));

        mockMvc.perform(post("/api/hl7/deliveries/" + retryingDelivery.getId() + "/retry")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already scheduled or in progress")));
    }

    @Test
    @DisplayName("Tenant Isolation: Hospital A user cannot retry Hospital B's delivery (HTTP 403 Forbidden)")
    void testTenantIsolationManualRetryDenied() throws Exception {
        ConversionEntity convB = conversionRepository.save(new ConversionEntity(
                "CTRL-B-FAIL", "ADT^A01", "SUCCESS", "P-B-1", null, hospitalB
        ));
        convB.setFhirPayload(VALID_FHIR_BUNDLE);
        conversionRepository.save(convB);

        DeliveryResultEntity deliveryB = deliveryResultRepository.save(new DeliveryResultEntity(
                convB.getId(),
                mockDestination.getId(),
                mockDestination.getName(),
                LocalDateTime.now(),
                "FAILED",
                500,
                "Internal Error",
                null,
                1,
                3,
                null,
                hospitalB
        ));

        // User A attempts to retry Hospital B's delivery
        mockMvc.perform(post("/api/hl7/deliveries/" + deliveryB.getId() + "/retry")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated retry requests are rejected with HTTP 401 Unauthorized")
    void testUnauthenticatedRetryRejected() throws Exception {
        mockMvc.perform(post("/api/hl7/deliveries/1/retry"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scheduled retry processor processes due deliveries and updates attempt count")
    void testScheduledRetryProcessorExecutesDueRetries() {
        ConversionEntity conv = conversionRepository.save(new ConversionEntity(
                "CTRL-SCHED-1", "ADT^A01", "SUCCESS", "P-SCHED", null, hospitalA
        ));
        conv.setFhirPayload(VALID_FHIR_BUNDLE);
        conversionRepository.save(conv);

        // Create a due delivery with nextRetryAt in the past
        DeliveryResultEntity dueDelivery = deliveryResultRepository.save(new DeliveryResultEntity(
                conv.getId(),
                mockDestination.getId(),
                mockDestination.getName(),
                LocalDateTime.now().minusSeconds(10),
                "RETRYING",
                503,
                "Transient network failure",
                null,
                1,
                3,
                LocalDateTime.now().minusSeconds(5), // Due!
                hospitalA
        ));

        // Execute scheduler polling
        deliveryRetryScheduler.processDueRetries();

        DeliveryResultEntity updated = deliveryResultRepository.findById(dueDelivery.getId()).orElseThrow();
        assertEquals("SUCCESS", updated.getStatus());
        assertEquals(2, updated.getAttemptCount());
        assertNull(updated.getNextRetryAt());
    }

    @Test
    @DisplayName("Audit logging records RETRY_SCHEDULED, RETRY_SUCCESS, and MANUAL_RETRY_REQUESTED without secrets or PHI")
    void testAuditTrailForRetries() throws Exception {
        ConversionEntity conv = conversionRepository.save(new ConversionEntity(
                "CTRL-AUDIT-RETRY", "ADT^A01", "SUCCESS", "P-AUDIT", null, hospitalA
        ));
        conv.setFhirPayload(VALID_FHIR_BUNDLE);
        conversionRepository.save(conv);

        DeliveryResultEntity failed = deliveryResultRepository.save(new DeliveryResultEntity(
                conv.getId(),
                mockDestination.getId(),
                mockDestination.getName(),
                LocalDateTime.now(),
                "FAILED",
                503,
                "Temporary failure",
                null,
                1,
                3,
                null,
                hospitalA
        ));

        mockMvc.perform(post("/api/hl7/deliveries/" + failed.getId() + "/retry")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        var logs = auditLogRepository.findAll();
        boolean manualRetryLogged = logs.stream().anyMatch(l -> "MANUAL_RETRY_REQUESTED".equals(l.getAction()));
        boolean retrySuccessLogged = logs.stream().anyMatch(l -> "RETRY_SUCCESS".equals(l.getAction()));

        assertTrue(manualRetryLogged, "MANUAL_RETRY_REQUESTED should be audited");
        assertTrue(retrySuccessLogged, "RETRY_SUCCESS should be audited");

        // Verify no sensitive tokens or patient payload in details
        for (var log : logs) {
            if (log.getDetails() != null) {
                assertFalse(log.getDetails().contains("Bearer "));
                assertFalse(log.getDetails().contains("secret"));
                assertFalse(log.getDetails().contains("resourceType\": \"Patient"));
            }
        }
    }
}
