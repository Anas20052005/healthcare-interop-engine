package com.example.healthcare.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new user or administrator in the system.
 * Complies with HIPAA/privacy regulations: password is never logged or exposed in responses.
 */
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username can only contain alphanumeric characters, underscores, hyphens, and dots")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(ROLE_)?(ADMIN|USER)$", message = "Role must be either ADMIN or USER")
    private String role = "USER";

    private Long hospitalId;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String username, String password, String role) {
        this(username, password, role, null);
    }

    public CreateUserRequest(String username, String password, String role, Long hospitalId) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.hospitalId = hospitalId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }
}
