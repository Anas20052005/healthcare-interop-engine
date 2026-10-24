package com.example.healthcare.integration;

import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.dto.hl7.HL7ConvertResponse;
import com.example.healthcare.entity.ConversionEntity;
import com.example.healthcare.entity.EncounterEntity;
import com.example.healthcare.entity.ObservationEntity;
import com.example.healthcare.entity.PatientEntity;
import com.example.healthcare.repository.ConversionRepository;
import com.example.healthcare.repository.EncounterRepository;
import com.example.healthcare.repository.ObservationRepository;
import com.example.healthcare.repository.PatientRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HL7ConversionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private ConversionRepository conversionRepository;

    @Autowired
    private com.example.healthcare.security.JwtService jwtService;

    @Autowired
    private com.example.healthcare.repository.HospitalRepository hospitalRepository;

    @Autowired
    private com.example.healthcare.repository.UserRepository userRepository;

    private com.example.healthcare.entity.Hospital testHospital;

    @org.junit.jupiter.api.BeforeEach
    void cleanUp() {
        conversionRepository.deleteAll();
        observationRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();

        testHospital = hospitalRepository.findByCode("TEST_HOSP").orElseGet(() ->
                hospitalRepository.save(new com.example.healthcare.entity.Hospital("Test Hospital", "TEST_HOSP", "test@hospital.org", "555-1111", "100 Med Way", true)));

        userRepository.findByUsername("admin").ifPresent(admin -> {
            admin.setHospital(testHospital);
            userRepository.save(admin);
        });
    }

    private static final String SYNTHETIC_ADT_A01 =
            "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG0001|P|2.5\r" +
            "PID|||P1001||Sharma^Rahul||19950510|M\r" +
            "PV1||I|WARD-101||||||||||||||||||||||||||||||||202610080900\r" +
            "OBX|1|NM|GLUCOSE^Blood Glucose||105|mg/dL|70-110|N\r" +
            "OBX|2|ST|COMMENT^Clinical Comment||Patient stable|||F";

    @Test
    @DisplayName("End-to-End: POST /api/hl7/convert -> Parse -> Validate -> Map -> Persist -> Return FHIR Bundle")
    void testEndToEndConversionPipeline() throws Exception {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");
        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_ADT_A01);

        MvcResult mvcResult = mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.messageControlId").value("MSG0001"))
                .andExpect(jsonPath("$.messageType").value("ADT^A01"))
                .andExpect(jsonPath("$.patientId").value("P1001"))
                .andExpect(jsonPath("$.observationCount").value(2))
                .andExpect(jsonPath("$.fhirOutput").isNotEmpty())
                .andReturn();

        String responseJson = mvcResult.getResponse().getContentAsString();
        HL7ConvertResponse convertResponse = objectMapper.readValue(responseJson, HL7ConvertResponse.class);
        String fhirOutput = convertResponse.getFhirOutput();

        // 1. Verify FHIR Bundle content
        assertNotNull(fhirOutput);
        assertTrue(fhirOutput.contains("\"resourceType\": \"Bundle\"") || fhirOutput.contains("\"resourceType\":\"Bundle\""));
        assertTrue(fhirOutput.contains("\"resourceType\": \"Patient\"") || fhirOutput.contains("\"resourceType\":\"Patient\""));
        assertTrue(fhirOutput.contains("\"resourceType\": \"Encounter\"") || fhirOutput.contains("\"resourceType\":\"Encounter\""));
        assertTrue(fhirOutput.contains("\"resourceType\": \"Observation\"") || fhirOutput.contains("\"resourceType\":\"Observation\""));
        assertTrue(fhirOutput.contains("GLUCOSE"));
        assertTrue(fhirOutput.contains("Patient stable"));

        // 2. Verify Patient in MySQL
        Optional<PatientEntity> optPatient = patientRepository.findByPatientIdentifier("P1001");
        assertTrue(optPatient.isPresent(), "Patient P1001 must be persisted");
        PatientEntity patient = optPatient.get();
        assertEquals("Sharma", patient.getFamilyName());
        assertEquals("Rahul", patient.getGivenName());
        assertEquals(LocalDate.of(1995, 5, 10), patient.getBirthDate());
        assertEquals("male", patient.getGender());

        // 3. Verify Encounter in MySQL
        List<EncounterEntity> encounters = encounterRepository.findByPatientId(patient.getId());
        assertEquals(1, encounters.size(), "One encounter should be persisted for patient");
        EncounterEntity encounter = encounters.get(0);
        assertEquals("IMP", encounter.getPatientClass());
        assertEquals("WARD-101", encounter.getLocation());
        assertEquals(patient.getId(), encounter.getPatient().getId());

        // 4. Verify Multiple Observations in MySQL
        List<ObservationEntity> observations = observationRepository.findByPatientId(patient.getId());
        assertEquals(2, observations.size(), "Two observations must be persisted for patient");

        ObservationEntity obs1 = observations.stream()
                .filter(o -> "GLUCOSE".equals(o.getObservationCode()))
                .findFirst().orElseThrow();
        assertEquals("NM", obs1.getValueType());
        assertEquals(105.0, obs1.getValueQuantity());
        assertEquals("mg/dL", obs1.getUnit());
        assertEquals("70-110", obs1.getReferenceRange());

        ObservationEntity obs2 = observations.stream()
                .filter(o -> "COMMENT".equals(o.getObservationCode()))
                .findFirst().orElseThrow();
        assertEquals("ST", obs2.getValueType());
        assertEquals("Patient stable", obs2.getValueString());

        // 5. Verify Conversion Audit in MySQL
        Optional<ConversionEntity> optConversion = conversionRepository.findByMessageControlId("MSG0001");
        assertTrue(optConversion.isPresent(), "Conversion audit record must exist");
        ConversionEntity conversion = optConversion.get();
        assertEquals("SUCCESS", conversion.getStatus());
        assertEquals("ADT^A01", conversion.getMessageType());
        assertEquals("P1001", conversion.getPatientIdentifier());
        assertNotNull(conversion.getCreatedAt());
    }

    @Test
    @DisplayName("GET /api/hl7/history should return 401 Unauthorized when request is unauthenticated")
    void shouldReturn401WhenUnauthenticatedRequestToHistory() throws Exception {
        mockMvc.perform(get("/api/hl7/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/hl7/history should return 200 OK and empty array when no conversions exist")
    void shouldReturnEmptyHistoryWhenDatabaseIsEmpty() throws Exception {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");

        mockMvc.perform(get("/api/hl7/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/hl7/history should return 200 OK and conversion records after successful conversion")
    void shouldReturnConversionHistoryAfterSuccessfulConversion() throws Exception {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");

        // 1. Perform conversion
        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_ADT_A01);
        mockMvc.perform(post("/api/hl7/convert")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 2. Fetch history
        mockMvc.perform(get("/api/hl7/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].messageControlId").value("MSG0001"))
                .andExpect(jsonPath("$[0].messageType").value("ADT^A01"))
                .andExpect(jsonPath("$[0].patientId").value("P1001"))
                .andExpect(jsonPath("$[0].encounterId").value("ENC-P1001"))
                .andExpect(jsonPath("$[0].observationCount").value(2))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].timestamp").isNotEmpty());
    }
}
