package com.example.healthcare.controller;

import com.example.healthcare.dto.hospital.HospitalCreateRequest;
import com.example.healthcare.dto.hospital.HospitalResponse;
import com.example.healthcare.dto.hospital.HospitalUpdateRequest;
import com.example.healthcare.exception.DuplicateHospitalCodeException;
import com.example.healthcare.exception.GlobalExceptionHandler;
import com.example.healthcare.exception.HospitalNotFoundException;
import com.example.healthcare.security.JwtAuthenticationFilter;
import com.example.healthcare.security.SecurityConfig;
import com.example.healthcare.service.HospitalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HospitalController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HospitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HospitalService hospitalService;

    @Test
    @DisplayName("POST /api/hospitals should return 201 Created and hospital details")
    void shouldCreateHospital() throws Exception {
        HospitalResponse response = new HospitalResponse(
                1L, "City General", "CITY_GEN", "contact@citygen.org", "555-1000", "100 Health Way",
                true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(hospitalService.createHospital(any(HospitalCreateRequest.class))).thenReturn(response);

        HospitalCreateRequest request = new HospitalCreateRequest("City General", "CITY_GEN");

        mockMvc.perform(post("/api/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("City General"))
                .andExpect(jsonPath("$.code").value("CITY_GEN"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("POST /api/hospitals should return 400 Bad Request when name or code is missing")
    void shouldReturn400WhenNameOrCodeMissing() throws Exception {
        HospitalCreateRequest request = new HospitalCreateRequest("", "");

        mockMvc.perform(post("/api/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/hospitals should return 409 Conflict when code is duplicate")
    void shouldReturn409WhenCodeIsDuplicate() throws Exception {
        when(hospitalService.createHospital(any(HospitalCreateRequest.class)))
                .thenThrow(new DuplicateHospitalCodeException("Hospital code 'CITY_GEN' is already in use"));

        HospitalCreateRequest request = new HospitalCreateRequest("City General", "CITY_GEN");

        mockMvc.perform(post("/api/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Hospital code 'CITY_GEN' is already in use"));
    }

    @Test
    @DisplayName("GET /api/hospitals should return 200 OK and list")
    void shouldGetAllHospitals() throws Exception {
        HospitalResponse h1 = new HospitalResponse(1L, "Apollo", "APOLLO", null, null, null, true, null, null);
        when(hospitalService.getAllHospitals()).thenReturn(List.of(h1));

        mockMvc.perform(get("/api/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Apollo"))
                .andExpect(jsonPath("$[0].code").value("APOLLO"));
    }

    @Test
    @DisplayName("GET /api/hospitals/{id} should return 200 OK when found")
    void shouldGetHospitalById() throws Exception {
        HospitalResponse response = new HospitalResponse(1L, "Apollo", "APOLLO", null, null, null, true, null, null);
        when(hospitalService.getHospitalById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Apollo"));
    }

    @Test
    @DisplayName("GET /api/hospitals/{id} should return 404 Not Found when not found")
    void shouldReturn404WhenHospitalNotFound() throws Exception {
        when(hospitalService.getHospitalById(99L))
                .thenThrow(new HospitalNotFoundException("Hospital not found with id: 99"));

        mockMvc.perform(get("/api/hospitals/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Hospital not found with id: 99"));
    }

    @Test
    @DisplayName("PUT /api/hospitals/{id} should return 200 OK with updated details")
    void shouldUpdateHospital() throws Exception {
        HospitalResponse response = new HospitalResponse(1L, "Updated Apollo", "APOLLO_NEW", null, null, null, true, null, null);
        when(hospitalService.updateHospital(eq(1L), any(HospitalUpdateRequest.class))).thenReturn(response);

        HospitalUpdateRequest request = new HospitalUpdateRequest("Updated Apollo", "APOLLO_NEW");

        mockMvc.perform(put("/api/hospitals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Apollo"))
                .andExpect(jsonPath("$.code").value("APOLLO_NEW"));
    }

    @Test
    @DisplayName("PUT /api/hospitals/{id} should return 404 Not Found when ID does not exist")
    void shouldReturn404OnUpdateNonexistentHospital() throws Exception {
        when(hospitalService.updateHospital(eq(99L), any(HospitalUpdateRequest.class)))
                .thenThrow(new HospitalNotFoundException("Hospital not found with id: 99"));

        HospitalUpdateRequest request = new HospitalUpdateRequest("Some Name", "SOME_CODE");

        mockMvc.perform(put("/api/hospitals/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
