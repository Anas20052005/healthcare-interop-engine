package com.example.healthcare.controller;

import com.example.healthcare.dto.hl7.ConversionHistoryResponse;
import com.example.healthcare.dto.hl7.HL7ConvertRequest;
import com.example.healthcare.dto.hl7.HL7ConvertResponse;
import com.example.healthcare.exception.GlobalExceptionHandler;
import com.example.healthcare.exception.InvalidHL7Exception;
import com.example.healthcare.exception.HL7MappingException;
import com.example.healthcare.service.HL7ConversionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.example.healthcare.security.SecurityConfig;
import com.example.healthcare.security.JwtAuthenticationFilter;

@WebMvcTest(
        controllers = HL7ConversionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HL7ConversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HL7ConversionService hl7ConversionService;

    @MockBean
    private com.example.healthcare.service.FhirDeliveryService fhirDeliveryService;

    private static final String SYNTHETIC_HL7 =
            "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG0001|P|2.5\r" +
            "PID|||P1001||Sharma^Rahul||19950510|M\r" +
            "PV1||I|WARD-101\r" +
            "OBX|1|NM|GLUCOSE^Blood Glucose||105|mg/dL|70-110|N";

    @Test
    @DisplayName("POST /api/hl7/convert should return 200 OK and conversion response for valid request")
    void shouldReturn200ForValidHL7Request() throws Exception {
        HL7ConvertResponse mockResponse = new HL7ConvertResponse(
                "SUCCESS",
                1L,
                "MSG0001",
                "ADT^A01",
                "P1001",
                "ENC-P1001",
                1,
                "{\"resourceType\":\"Bundle\"}"
        );

        when(hl7ConversionService.convert(any(HL7ConvertRequest.class))).thenReturn(mockResponse);

        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_HL7);

        mockMvc.perform(post("/api/hl7/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.conversionId").value(1))
                .andExpect(jsonPath("$.messageControlId").value("MSG0001"))
                .andExpect(jsonPath("$.messageType").value("ADT^A01"))
                .andExpect(jsonPath("$.patientId").value("P1001"))
                .andExpect(jsonPath("$.observationCount").value(1))
                .andExpect(jsonPath("$.fhirOutput").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/hl7/convert should return 400 Bad Request when HL7 message is blank")
    void shouldReturn400WhenMessageIsBlank() throws Exception {
        HL7ConvertRequest request = new HL7ConvertRequest("");

        mockMvc.perform(post("/api/hl7/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/hl7/convert should return 400 Bad Request when HL7 message is null")
    void shouldReturn400WhenMessageIsNull() throws Exception {
        HL7ConvertRequest request = new HL7ConvertRequest(null);

        mockMvc.perform(post("/api/hl7/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/hl7/convert should return 400 Bad Request when service throws InvalidHL7Exception")
    void shouldReturn400WhenServiceThrowsInvalidHL7Exception() throws Exception {
        when(hl7ConversionService.convert(any(HL7ConvertRequest.class)))
                .thenThrow(new InvalidHL7Exception("Unsupported message type: ORU^R01"));

        HL7ConvertRequest request = new HL7ConvertRequest("MSH|^~\\&|HIS|||||ORU^R01|MSG1|P|2.5");

        mockMvc.perform(post("/api/hl7/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported message type: ORU^R01"));
    }

    @Test
    @DisplayName("POST /api/hl7/convert should return 400 Bad Request when service throws HL7MappingException")
    void shouldReturn400WhenServiceThrowsHL7MappingException() throws Exception {
        when(hl7ConversionService.convert(any(HL7ConvertRequest.class)))
                .thenThrow(new HL7MappingException("Unsupported gender value: X"));

        HL7ConvertRequest request = new HL7ConvertRequest(SYNTHETIC_HL7);

        mockMvc.perform(post("/api/hl7/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported gender value: X"));
    }

    @Test
    @DisplayName("GET /api/hl7/history should return 200 OK and conversion history list")
    void shouldReturn200WithConversionHistory() throws Exception {
        ConversionHistoryResponse item = new ConversionHistoryResponse(
                101L,
                "MSG0001",
                "ADT^A01",
                "P1001",
                "ENC-P1001",
                2,
                "SUCCESS",
                null,
                LocalDateTime.now()
        );
        when(hl7ConversionService.getConversionHistory()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/hl7/history")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conversionId").value(101))
                .andExpect(jsonPath("$[0].messageControlId").value("MSG0001"))
                .andExpect(jsonPath("$[0].messageType").value("ADT^A01"))
                .andExpect(jsonPath("$[0].patientId").value("P1001"))
                .andExpect(jsonPath("$[0].encounterId").value("ENC-P1001"))
                .andExpect(jsonPath("$[0].observationCount").value(2))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/hl7/history should return 200 OK and empty array when no history exists")
    void shouldReturn200WithEmptyHistory() throws Exception {
        when(hl7ConversionService.getConversionHistory()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/hl7/history")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/hl7/history should return 500 Internal Server Error when service throws exception")
    void shouldReturn500WhenServiceThrowsException() throws Exception {
        when(hl7ConversionService.getConversionHistory())
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/hl7/history")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected internal error occurred."));
    }
}
