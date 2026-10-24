package com.example.healthcare.dto.hospital;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing hospital.
 */
public class HospitalUpdateRequest {

    @NotBlank(message = "Hospital name is required")
    @Size(min = 2, max = 150, message = "Hospital name must be between 2 and 150 characters")
    private String name;

    @NotBlank(message = "Hospital code is required")
    @Size(min = 2, max = 50, message = "Hospital code must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Hospital code can only contain alphanumeric characters, underscores, hyphens, and dots")
    private String code;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    private String phone;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    private Boolean enabled;

    public HospitalUpdateRequest() {
    }

    public HospitalUpdateRequest(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public HospitalUpdateRequest(String name, String code, String email, String phone, String address, Boolean enabled) {
        this.name = name;
        this.code = code;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
