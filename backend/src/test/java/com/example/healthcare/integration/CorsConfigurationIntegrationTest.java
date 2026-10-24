package com.example.healthcare.integration;

import com.example.healthcare.dto.auth.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigurationIntegrationTest {

    private static final String PROD_ORIGIN = "https://healthcare-interop-engine-frontend.vercel.app";
    private static final String DEV_ORIGIN_5173 = "http://localhost:5173";
    private static final String DEV_ORIGIN_3000 = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "https://unauthorized-domain.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("CORS Preflight OPTIONS /api/auth/login from production Vercel frontend should succeed with correct CORS headers")
    void preflightLoginFromProductionOriginShouldSucceed() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, PROD_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PROD_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("OPTIONS")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("content-type")));
    }

    @Test
    @DisplayName("CORS Preflight OPTIONS on protected endpoint without JWT should succeed before authentication")
    void preflightProtectedEndpointShouldSucceedWithoutAuth() throws Exception {
        mockMvc.perform(options("/api/dashboard/stats")
                        .header(HttpHeaders.ORIGIN, PROD_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PROD_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }

    @Test
    @DisplayName("CORS Preflight OPTIONS from localhost:5173 development origin should succeed")
    void preflightFromViteDevOriginShouldSucceed() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, DEV_ORIGIN_5173)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_ORIGIN_5173))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS Preflight OPTIONS from localhost:3000 development origin should succeed")
    void preflightFromReactDevOriginShouldSucceed() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, DEV_ORIGIN_3000)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_ORIGIN_3000))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS Preflight OPTIONS from disallowed origin should be rejected with 403 and no allow-origin header")
    void preflightFromDisallowedOriginShouldBeRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Actual POST /api/auth/login from production origin should include CORS allow-origin header")
    void actualLoginRequestFromProductionOriginShouldIncludeCorsHeader() throws Exception {
        LoginRequest request = new LoginRequest("admin", "admin123");

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, PROD_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PROD_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }
}
