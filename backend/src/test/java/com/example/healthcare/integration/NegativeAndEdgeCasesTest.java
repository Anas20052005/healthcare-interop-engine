package com.example.healthcare.integration;

import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.entity.PatientEntity;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.EncounterRepository;
import com.example.healthcare.repository.ObservationRepository;
import com.example.healthcare.repository.PatientRepository;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NegativeAndEdgeCasesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private ConversionRepository conversionRepository;

    @Autowired
    private com.example.healthcare.repository.HospitalRepository hospitalRepository;

    @Autowired
    private com.example.healthcare.repository.UserRepository userRepository;

    private String validToken;

    @BeforeEach
    void setUp() {
        com.example.healthcare.entity.Hospital testHospital = hospitalRepository.findByCode("TEST_EDGE_HOSP").orElseGet(() ->
                hospitalRepository.save(new com.example.healthcare.entity.Hospital("Edge Hospital", "TEST_EDGE_HOSP", "edge@hosp.org", "555-2222", "200 Edge Way", true)));

        userRepository.findByUsername("admin").ifPresent(admin -> {
            admin.setHospital(testHospital);
            userRepository.save(admin);
        });

        validToken = jwtService.generateToken("admin", "ROLE_ADMIN", testHospital.getId());
        conversionRepository.deleteAll();
        observationRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Empty HL7 message returns HTTP 400 Bad Request")
    void testEmptyHl7MessageReturns400() throws Exception {
        HL7ConvertRequest request = new HL7ConvertRequest("");

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("2. Malformed HL7 message not starting with MSH returns HTTP 400 Bad Request")
    void testMalformedHl7MessageReturns400() throws Exception {
        HL7ConvertRequest request = new HL7ConvertRequest("PID|1||P1001||Smith^John");

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("HL7 message must start with an MSH segment"));
    }

    @Test
    @DisplayName("3. Missing PID segment returns HTTP 400 Bad Request")
    void testMissingPidReturns400() throws Exception {
        String noPidMessage = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG001|P|2.5\r" +
                "PV1||I|WARD-1";
        HL7ConvertRequest request = new HL7ConvertRequest(noPidMessage);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Patient information (PID) is required"));
    }

    @Test
    @DisplayName("4. Missing required patient identifier in PID returns HTTP 400 Bad Request")
    void testMissingPatientIdentifierReturns400() throws Exception {
        String noIdMessage = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG002|P|2.5\r" +
                "PID|||||Sharma^Rahul||19950510|M";
        HL7ConvertRequest request = new HL7ConvertRequest(noIdMessage);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Patient identifier is required"));
    }

    @Test
    @DisplayName("5. Invalid date format in PID birthdate returns HTTP 400 Bad Request")
    void testInvalidDateReturns400() throws Exception {
        String invalidDateMessage = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG003|P|2.5\r" +
                "PID|||P1001||Sharma^Rahul||1995-05-10|M";
        HL7ConvertRequest request = new HL7ConvertRequest(invalidDateMessage);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid birth date: 1995-05-10"));
    }

    @Test
    @DisplayName("6. Invalid numeric OBX value returns HTTP 400 Bad Request")
    void testInvalidNumericObxReturns400() throws Exception {
        String nonNumericObxMessage = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG004|P|2.5\r" +
                "PID|||P1001||Sharma^Rahul||19950510|M\r" +
                "OBX|1|NM|GLUCOSE||not_a_number|mg/dL|70-110|N";
        HL7ConvertRequest request = new HL7ConvertRequest(nonNumericObxMessage);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid numeric observation value: not_a_number"));
    }

    @Test
    @DisplayName("7. Missing optional PV1 segment successfully converts with HTTP 200 OK")
    void testMissingOptionalPv1Succeeds() throws Exception {
        String noPv1Message = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG005|P|2.5\r" +
                "PID|||P-NOPV1||Deshmukh^Rohan||19850510|M\r" +
                "OBX|1|NM|GLUCOSE||99|mg/dL|70-110|N";
        HL7ConvertRequest request = new HL7ConvertRequest(noPv1Message);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.patientId").value("P-NOPV1"))
                .andExpect(jsonPath("$.observationCount").value(1));
    }

    @Test
    @DisplayName("8. Multiple OBX segments successfully converted and counted")
    void testMultipleObxSucceeds() throws Exception {
        String multiObxMessage = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG006|P|2.5\r" +
                "PID|||P-MULTI||Gupta^Anjali||19920315|F\r" +
                "PV1||I|WARD-1\r" +
                "OBX|1|NM|GLUCOSE||100|mg/dL|70-110|N\r" +
                "OBX|2|NM|HEMOGLOBIN||14.0|g/dL|12-16|N\r" +
                "OBX|3|ST|COMMENT||Fasting test verified|||F";
        HL7ConvertRequest request = new HL7ConvertRequest(multiObxMessage);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.observationCount").value(3));
    }

    @Test
    @DisplayName("9. Empty OBX value returns HTTP 400 Bad Request")
    void testEmptyObxValueReturns400() throws Exception {
        String emptyObxValMessage = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG007|P|2.5\r" +
                "PID|||P1001||Sharma^Rahul||19950510|M\r" +
                "OBX|1|NM|GLUCOSE|||mg/dL|70-110|N";
        HL7ConvertRequest request = new HL7ConvertRequest(emptyObxValMessage);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Observation value is required for observation 1"));
    }

    @Test
    @DisplayName("10. Unsupported message type ADT^A08 returns HTTP 400 Bad Request")
    void testUnsupportedMessageTypeReturns400() throws Exception {
        String unsupportedTypeMessage = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A08|MSG008|P|2.5\r" +
                "PID|||P1001||Sharma^Rahul||19950510|M";
        HL7ConvertRequest request = new HL7ConvertRequest(unsupportedTypeMessage);

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported HL7 message type: ADT^A08. Only ADT^A01 is supported by the current version."));
    }

    @Test
    @DisplayName("11. Duplicate patient ID reuses existing patient record and does not duplicate in database")
    void testDuplicatePatientIdReusesPatient() throws Exception {
        String msg1 = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG-DUP1|P|2.5\r" +
                "PID|||P-REUSE||Original^Name||19900101|M";
        String msg2 = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG-DUP2|P|2.5\r" +
                "PID|||P-REUSE||Updated^Name||19900101|M";

        // Convert first message
        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HL7ConvertRequest(msg1))))
                .andExpect(status().isOk());

        // Convert second message with same patient ID
        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HL7ConvertRequest(msg2))))
                .andExpect(status().isOk());

        // Confirm database contains exactly 1 patient with P-REUSE and updated name
        List<PatientEntity> patients = patientRepository.findAll().stream()
                .filter(p -> "P-REUSE".equals(p.getPatientIdentifier()))
                .toList();
        assertEquals(1, patients.size(), "Only one patient record must exist for P-REUSE");
        assertEquals("Updated", patients.get(0).getFamilyName());
    }

    @Test
    @DisplayName("12. Unauthorized request without JWT returns HTTP 401 Unauthorized")
    void testUnauthorizedRequestReturns401() throws Exception {
        String msg = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG010|P|2.5\r" +
                "PID|||P1001||Sharma^Rahul||19950510|M";

        mockMvc.perform(post("/api/hl7/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HL7ConvertRequest(msg))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("13. Invalid JWT signature/format returns HTTP 401 Unauthorized")
    void testInvalidJwtReturns401() throws Exception {
        String msg = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG011|P|2.5\r" +
                "PID|||P1001||Sharma^Rahul||19950510|M";

        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer fake.corrupted.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HL7ConvertRequest(msg))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("14. Malformed JSON request body returns clean HTTP 400 Bad Request")
    void testMalformedJsonRequestBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json-body"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }
}
