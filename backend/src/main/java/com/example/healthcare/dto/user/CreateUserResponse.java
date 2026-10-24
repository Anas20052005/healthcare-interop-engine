package com.example.healthcare.dto.user;

/**
 * Response DTO returned after user creation.
 * Complies with HIPAA/privacy regulations: NEVER contains password or password hash.
 */
public class CreateUserResponse {

    private Long id;
    private String username;
    private String role;
    private boolean enabled;
    private Long hospitalId;
    private String hospitalName;
    private String hospitalCode;
    private String message;

    public CreateUserResponse() {
    }

    public CreateUserResponse(Long id, String username, String role, boolean enabled, String message) {
        this(id, username, role, enabled, null, null, null, message);
    }

    public CreateUserResponse(Long id, String username, String role, boolean enabled,
                              Long hospitalId, String hospitalName, String hospitalCode, String message) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.hospitalCode = hospitalCode;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
