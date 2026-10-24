package com.example.healthcare.controller;

import com.example.healthcare.dto.user.CreateUserRequest;
import com.example.healthcare.dto.user.CreateUserResponse;
import com.example.healthcare.exception.GlobalExceptionHandler;
import com.example.healthcare.exception.UserAlreadyExistsException;
import com.example.healthcare.security.JwtAuthenticationFilter;
import com.example.healthcare.security.SecurityConfig;
import com.example.healthcare.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/users should return 201 Created and user details without password")
    void shouldReturn201ForValidUserCreation() throws Exception {
        CreateUserResponse mockResponse = new CreateUserResponse(2L, "hospitaladmin", "ADMIN", true, "User created successfully");
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(mockResponse);

        CreateUserRequest request = new CreateUserRequest("hospitaladmin", "StrongPassword123", "ADMIN");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("hospitaladmin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/users should return 409 Conflict when username is duplicate")
    void shouldReturn409ForDuplicateUsername() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Username 'hospitaladmin' is already taken"));

        CreateUserRequest request = new CreateUserRequest("hospitaladmin", "StrongPassword123", "ADMIN");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username 'hospitaladmin' is already taken"));
    }

    @Test
    @DisplayName("POST /api/users should return 400 Bad Request when username is blank")
    void shouldReturn400WhenUsernameIsBlank() throws Exception {
        CreateUserRequest request = new CreateUserRequest("", "StrongPassword123", "ADMIN");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/users should return 400 Bad Request when password is shorter than 8 characters")
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        CreateUserRequest request = new CreateUserRequest("validuser", "short", "USER");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/users should return 400 Bad Request when role is invalid")
    void shouldReturn400WhenRoleIsInvalid() throws Exception {
        CreateUserRequest request = new CreateUserRequest("validuser", "StrongPassword123", "INVALID_ROLE");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
