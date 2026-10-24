package com.example.healthcare.service;

import com.example.healthcare.dto.user.CreateUserRequest;
import com.example.healthcare.dto.user.CreateUserResponse;
import com.example.healthcare.dto.user.UpdateUserRequest;
import com.example.healthcare.dto.user.UserResponse;
import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.exception.HospitalNotFoundException;
import com.example.healthcare.exception.HospitalRequiredException;
import com.example.healthcare.exception.InvalidHospitalException;
import com.example.healthcare.exception.ResourceNotFoundException;
import com.example.healthcare.exception.TenantAccessDeniedException;
import com.example.healthcare.exception.UserAlreadyExistsException;
import com.example.healthcare.repository.HospitalRepository;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.security.TenantSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of UserService managing user creation and retrieval
 * with strict multi-tenant isolation, HIPAA compliance, and BCrypt password encryption.
 * Never logs passwords, plaintext secrets, or exposes password hashes.
 */
@Service
public class DefaultUserService implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultUserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HospitalRepository hospitalRepository;
    private final TenantSecurityService tenantSecurityService;
    private final AuditService auditService;

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultUserService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              HospitalRepository hospitalRepository,
                              TenantSecurityService tenantSecurityService,
                              AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.hospitalRepository = hospitalRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.auditService = auditService;
    }

    public DefaultUserService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              HospitalRepository hospitalRepository,
                              TenantSecurityService tenantSecurityService) {
        this(userRepository, passwordEncoder, hospitalRepository, tenantSecurityService, null);
    }

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        String trimmedUsername = request.getUsername().trim();

        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new UserAlreadyExistsException("Username '" + trimmedUsername + "' is already taken");
        }

        // Normalize role (strip optional ROLE_ prefix and standardize to uppercase)
        String rawRole = request.getRole() != null ? request.getRole().trim().toUpperCase() : "USER";
        String normalizedRole = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;

        Hospital hospital = null;
        Optional<User> callerOpt = tenantSecurityService.getCurrentUser();

        if (callerOpt.isPresent()) {
            if (tenantSecurityService.isPlatformAdmin()) {
                // Platform admin can assign user to any hospital or leave as platform user
                if (request.getHospitalId() != null) {
                    hospital = hospitalRepository.findById(request.getHospitalId())
                            .orElseThrow(() -> new HospitalNotFoundException("Hospital not found with id: " + request.getHospitalId()));

                    if (!hospital.isEnabled()) {
                        throw new InvalidHospitalException("Cannot assign user to disabled hospital: " + hospital.getName());
                    }
                }
            } else if (tenantSecurityService.isHospitalAdmin()) {
                Hospital callerHospital = tenantSecurityService.requireCurrentHospital();

                // Hospital admin cannot specify a different hospital
                if (request.getHospitalId() != null && !request.getHospitalId().equals(callerHospital.getId())) {
                    throw new TenantAccessDeniedException("Hospital administrator cannot create users for another hospital");
                }

                // If request attempted to create a platform-level admin (no hospital + ADMIN role), reject
                if (request.getHospitalId() == null && "ADMIN".equals(normalizedRole)) {
                    throw new TenantAccessDeniedException("Hospital administrator cannot create platform-level administrators");
                }

                // Associate automatically with the hospital admin's hospital
                hospital = callerHospital;
                if (!hospital.isEnabled()) {
                    throw new InvalidHospitalException("Cannot assign user to disabled hospital: " + hospital.getName());
                }
            } else {
                throw new TenantAccessDeniedException("Insufficient privileges to create user accounts");
            }
        } else {
            // Unauthenticated invocation (e.g. system bootstrap or direct test calls)
            if (request.getHospitalId() != null) {
                hospital = hospitalRepository.findById(request.getHospitalId())
                        .orElseThrow(() -> new HospitalNotFoundException("Hospital not found with id: " + request.getHospitalId()));

                if (!hospital.isEnabled()) {
                    throw new InvalidHospitalException("Cannot assign user to disabled hospital: " + hospital.getName());
                }
            }
        }

        // BCrypt encode the password - never store plaintext
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(trimmedUsername, encodedPassword, normalizedRole, true);
        if (hospital != null) {
            user.setHospital(hospital);
            logger.info("Associating user '{}' with hospital '{}' (ID: {})", trimmedUsername, hospital.getName(), hospital.getId());
        } else {
            logger.info("Creating user '{}' without hospital association (platform-level user)", trimmedUsername);
        }

        User savedUser = userRepository.save(user);

        logger.info("User '{}' with role '{}' created successfully", savedUser.getUsername(), savedUser.getRole());

        if (auditService != null) {
            auditService.log(
                    "USER_CREATE",
                    "USER",
                    String.valueOf(savedUser.getId()),
                    "SUCCESS",
                    "Created user '" + savedUser.getUsername() + "' with role '" + savedUser.getRole() + "'",
                    savedUser.getHospital()
            );
        }

        return new CreateUserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole(),
                savedUser.isEnabled(),
                hospital != null ? hospital.getId() : null,
                hospital != null ? hospital.getName() : null,
                hospital != null ? hospital.getCode() : null,
                "User created successfully"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        if (tenantSecurityService.isPlatformAdmin()) {
            return userRepository.findAll().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        Hospital hospital = tenantSecurityService.requireCurrentHospital();
        return userRepository.findAllByHospitalId(hospital.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (tenantSecurityService.isPlatformAdmin()) {
            return toResponse(user);
        }

        Hospital hospital = tenantSecurityService.requireCurrentHospital();
        if (user.getHospital() == null || !user.getHospital().getId().equals(hospital.getId())) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        boolean isPlatformAdmin = tenantSecurityService.isPlatformAdmin();
        boolean isHospitalAdmin = tenantSecurityService.isHospitalAdmin();

        if (isPlatformAdmin) {
            // Platform admin can update hospital association, role, enabled, password
            if (Boolean.TRUE.equals(request.getClearHospital())) {
                user.setHospital(null);
            } else if (request.getHospitalId() != null) {
                Hospital hospital = hospitalRepository.findById(request.getHospitalId())
                        .orElseThrow(() -> new HospitalNotFoundException("Hospital not found with id: " + request.getHospitalId()));
                if (!hospital.isEnabled()) {
                    throw new InvalidHospitalException("Cannot assign user to disabled hospital: " + hospital.getName());
                }
                user.setHospital(hospital);
            }
            if (request.getRole() != null) {
                String rawRole = request.getRole().trim().toUpperCase();
                String normalizedRole = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
                user.setRole(normalizedRole);
            }
        } else if (isHospitalAdmin) {
            Hospital callerHospital = tenantSecurityService.requireCurrentHospital();
            // User must belong to caller's hospital
            if (user.getHospital() == null || !user.getHospital().getId().equals(callerHospital.getId())) {
                throw new ResourceNotFoundException("User not found with id: " + id);
            }

            // Hospital admin cannot move user to another hospital
            if (request.getHospitalId() != null && !request.getHospitalId().equals(callerHospital.getId())) {
                throw new TenantAccessDeniedException("Hospital administrator cannot reassign users to another hospital");
            }

            // Hospital admin cannot dissociate user from hospital
            if (Boolean.TRUE.equals(request.getClearHospital())) {
                throw new TenantAccessDeniedException("Hospital administrator cannot dissociate user from hospital");
            }

            // Hospital admin cannot promote user to platform admin
            if (request.getRole() != null) {
                String rawRole = request.getRole().trim().toUpperCase();
                String normalizedRole = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
                user.setRole(normalizedRole);
            }
        } else {
            throw new TenantAccessDeniedException("Insufficient privileges to update user accounts");
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        logger.info("User '{}' (ID: {}) updated successfully", updatedUser.getUsername(), updatedUser.getId());

        if (auditService != null) {
            auditService.log(
                    "USER_UPDATE",
                    "USER",
                    String.valueOf(updatedUser.getId()),
                    "SUCCESS",
                    "Updated user '" + updatedUser.getUsername() + "' (role: " + updatedUser.getRole() + ", enabled: " + updatedUser.isEnabled() + ")",
                    updatedUser.getHospital()
            );
        }

        return toResponse(updatedUser);
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isEnabled(),
                user.getHospital() != null ? user.getHospital().getId() : null,
                user.getHospital() != null ? user.getHospital().getName() : null,
                user.getHospital() != null ? user.getHospital().getCode() : null
        );
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
