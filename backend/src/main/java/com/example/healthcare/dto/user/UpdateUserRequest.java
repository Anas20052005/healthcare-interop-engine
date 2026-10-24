package com.example.healthcare.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing user account.
 * Allows updating role, hospital association, enabled status, and optionally password.
 */
public class UpdateUserRequest {

    @Pattern(regexp = "^(ROLE_)?(ADMIN|USER)$", message = "Role must be either ADMIN or USER")
    private String role;

    private Long hospitalId;

    private Boolean enabled;

    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long if provided")
    private String password;

    private Boolean clearHospital;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String role, Long hospitalId, Boolean enabled) {
        this.role = role;
        this.hospitalId = hospitalId;
        this.enabled = enabled;
    }

    public UpdateUserRequest(String role, Long hospitalId, Boolean enabled, Boolean clearHospital) {
        this.role = role;
        this.hospitalId = hospitalId;
        this.enabled = enabled;
        this.clearHospital = clearHospital;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getClearHospital() {
        return clearHospital;
    }

    public void setClearHospital(Boolean clearHospital) {
        this.clearHospital = clearHospital;
    }
}
