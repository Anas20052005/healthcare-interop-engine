package com.example.healthcare.service;

import com.example.healthcare.dto.auth.LoginRequest;
import com.example.healthcare.dto.auth.LoginResponse;
import com.example.healthcare.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Default implementation of AuthService using Spring Security AuthenticationManager and JwtService.
 * Complies with HIPAA/privacy regulations: never logs passwords, JWT tokens, secrets, or PHI.
 */
@Service
public class DefaultAuthService implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final com.example.healthcare.repository.UserRepository userRepository;
    private final AuditService auditService;

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultAuthService(AuthenticationManager authenticationManager,
                              JwtService jwtService,
                              com.example.healthcare.repository.UserRepository userRepository,
                              AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public DefaultAuthService(AuthenticationManager authenticationManager,
                              JwtService jwtService,
                              com.example.healthcare.repository.UserRepository userRepository) {
        this(authenticationManager, jwtService, userRepository, null);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request != null && request.getUsername() != null ? request.getUsername().trim() : "UNKNOWN";
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");

            com.example.healthcare.entity.User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            Long hospitalId = (user != null && user.getHospital() != null) ? user.getHospital().getId() : null;
            com.example.healthcare.entity.Hospital hospital = (user != null) ? user.getHospital() : null;

            logger.info("User '{}' successfully authenticated (hospital: {})", userDetails.getUsername(), hospitalId);
            String token = jwtService.generateToken(userDetails.getUsername(), role, hospitalId);

            if (auditService != null) {
                auditService.logSecurityEvent(
                        userDetails.getUsername(),
                        "LOGIN_SUCCESS",
                        "SUCCESS",
                        "User authenticated successfully with role " + role,
                        hospital
                );
            }

            return new LoginResponse(token, "Bearer");
        } catch (org.springframework.security.core.AuthenticationException ex) {
            logger.warn("Authentication failed for user '{}': {}", username, ex.getMessage());
            if (auditService != null) {
                com.example.healthcare.entity.User user = userRepository.findByUsername(username).orElse(null);
                com.example.healthcare.entity.Hospital hospital = (user != null) ? user.getHospital() : null;
                auditService.logSecurityEvent(
                        username,
                        "LOGIN_FAILURE",
                        "FAILURE",
                        "Authentication failed: invalid credentials",
                        hospital
                );
            }
            throw ex;
        }
    }
}
