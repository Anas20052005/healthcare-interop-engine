package com.example.healthcare.dto.auth;

/**
 * Response DTO returned upon successful authentication containing JWT.
 */
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";

    public LoginResponse() {
    }

    public LoginResponse(String token) {
        this.token = token;
        this.tokenType = "Bearer";
    }

    public LoginResponse(String token, String tokenType) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
