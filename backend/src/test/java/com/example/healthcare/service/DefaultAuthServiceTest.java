package com.example.healthcare.service;

import com.example.healthcare.dto.auth.LoginRequest;
import com.example.healthcare.dto.auth.LoginResponse;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DefaultAuthService authService;

    @Test
    @DisplayName("Should successfully authenticate user and return Bearer token with hospitalId claim")
    void shouldAuthenticateAndReturnToken() {
        LoginRequest request = new LoginRequest("admin", "admin123");
        UserDetails userDetails = new User("admin", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        com.example.healthcare.entity.User dbUser = new com.example.healthcare.entity.User("admin", "hash", "ADMIN", true);
        Hospital hospital = new Hospital("City Hospital", "CITYHOSP");
        hospital.setId(10L);
        dbUser.setHospital(hospital);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(dbUser));
        when(jwtService.generateToken(eq("admin"), eq("ROLE_ADMIN"), eq(10L))).thenReturn("sample.jwt.token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("sample.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken("admin", "ROLE_ADMIN", 10L);
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when authentication fails")
    void shouldPropagateBadCredentialsException() {
        LoginRequest request = new LoginRequest("admin", "wrong_password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
