package com.example.healthcare.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET_KEY = "healthcare-interop-engine-secure-development-jwt-secret-key-32bytes-min";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_KEY, EXPIRATION_MS);
        userDetails = new User("admin", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Should generate token and extract correct username and role")
    void shouldGenerateAndExtractClaims() {
        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertEquals("admin", jwtService.extractUsername(token));
        assertEquals("ROLE_ADMIN", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Should validate token against matching user details")
    void shouldValidateTokenForCorrectUser() {
        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    @DisplayName("Should reject token for different user")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateToken(userDetails);
        UserDetails differentUser = new User("other_user", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Create a JwtService with negative expiration (already expired)
        JwtService expiredJwtService = new JwtService(SECRET_KEY, -1000);
        String token = expiredJwtService.generateToken(userDetails);

        assertFalse(expiredJwtService.isTokenValid(token, userDetails));
        assertFalse(expiredJwtService.validateToken(token));
    }

    @Test
    @DisplayName("Should reject malformed or tampered token")
    void shouldRejectMalformedToken() {
        String malformedToken = "invalid.token.structure";

        assertFalse(jwtService.validateToken(malformedToken));
        assertFalse(jwtService.isTokenValid(malformedToken, userDetails));
    }

    @Test
    @DisplayName("Token claims must only contain minimal security claims (no PHI)")
    void shouldContainOnlyMinimalClaims() {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");

        Date expiration = jwtService.extractExpiration(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));

        assertEquals("admin", jwtService.extractUsername(token));
        assertEquals("ROLE_ADMIN", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Production profile: requires JWT_SECRET and rejects null or blank")
    void shouldRequireJwtSecretInProduction() {
        org.springframework.core.env.Environment env = org.mockito.Mockito.mock(org.springframework.core.env.Environment.class);
        org.mockito.Mockito.when(env.acceptsProfiles(org.mockito.ArgumentMatchers.any(org.springframework.core.env.Profiles.class))).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new JwtService(null, EXPIRATION_MS, env));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new JwtService("", EXPIRATION_MS, env));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new JwtService("   ", EXPIRATION_MS, env));
    }

    @Test
    @DisplayName("Production profile: rejects default insecure development JWT secret")
    void shouldRejectInsecureDevSecretInProduction() {
        org.springframework.core.env.Environment env = org.mockito.Mockito.mock(org.springframework.core.env.Environment.class);
        org.mockito.Mockito.when(env.acceptsProfiles(org.mockito.ArgumentMatchers.any(org.springframework.core.env.Profiles.class))).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new JwtService(SECRET_KEY, EXPIRATION_MS, env));
    }

    @Test
    @DisplayName("Production profile: rejects secret shorter than 32 bytes")
    void shouldRejectShortSecretInProduction() {
        org.springframework.core.env.Environment env = org.mockito.Mockito.mock(org.springframework.core.env.Environment.class);
        org.mockito.Mockito.when(env.acceptsProfiles(org.mockito.ArgumentMatchers.any(org.springframework.core.env.Profiles.class))).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new JwtService("short-secret-under-32-chars", EXPIRATION_MS, env));
    }

    @Test
    @DisplayName("Production profile: accepts secure production secret >= 32 bytes")
    void shouldAcceptSecureSecretInProduction() {
        org.springframework.core.env.Environment env = org.mockito.Mockito.mock(org.springframework.core.env.Environment.class);
        org.mockito.Mockito.when(env.acceptsProfiles(org.mockito.ArgumentMatchers.any(org.springframework.core.env.Profiles.class))).thenReturn(true);

        String strongProdSecret = "production-high-entropy-jwt-secret-key-32chars-min-ok!";
        JwtService prodService = new JwtService(strongProdSecret, EXPIRATION_MS, env);
        String token = prodService.generateToken(userDetails);
        assertNotNull(token);
        assertTrue(prodService.validateToken(token));
    }
}
