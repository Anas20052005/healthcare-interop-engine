package com.example.healthcare.service;

import com.example.healthcare.dto.auth.LoginRequest;
import com.example.healthcare.dto.auth.LoginResponse;

/**
 * Service handling authentication operations.
 */
public interface AuthService {

    /**
     * Authenticates user credentials and issues a JWT token.
     *
     * @param request login request with username and password
     * @return login response containing the issued JWT token
     */
    LoginResponse login(LoginRequest request);
}
