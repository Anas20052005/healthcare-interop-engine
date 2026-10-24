package com.example.healthcare.security;

import com.example.healthcare.entity.Hospital;
import com.example.healthcare.entity.User;
import com.example.healthcare.exception.HospitalRequiredException;
import com.example.healthcare.exception.TenantAccessDeniedException;
import com.example.healthcare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for resolving the current authenticated user and enforcing multi-tenant
 * hospital data isolation. Never trusts client-supplied tenant identifiers;
 * always resolves user and hospital ownership directly from trusted database state.
 */
@Service
@Transactional(readOnly = true)
public class TenantSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(TenantSecurityService.class);

    private final UserRepository userRepository;

    public TenantSecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the current authenticated user as an Optional, or Optional.empty() if unauthenticated.
     *
     * @return Optional containing User entity if authenticated, else Optional.empty()
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }

    /**
     * Resolves the current authenticated User directly from the database using the
     * security context principal username.
     *
     * @return current User entity from database
     * @throws AuthenticationCredentialsNotFoundException if no user is authenticated
     * @throws UsernameNotFoundException                   if authenticated principal is not found in database
     */
    public User getCurrentAuthenticatedUser() {
        return getCurrentUser()
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("No authenticated user found in security context"));
    }

    /**
     * Retrieves the hospital associated with the current authenticated user.
     * Throws HospitalRequiredException if the user does not have an assigned or active hospital.
     *
     * @return trusted Hospital entity
     * @throws HospitalRequiredException if user has no assigned hospital or hospital is disabled
     */
    public Hospital requireCurrentHospital() {
        User user = getCurrentAuthenticatedUser();
        Hospital hospital = user.getHospital();

        if (hospital == null) {
            logger.warn("Access rejected: user '{}' has no assigned hospital", user.getUsername());
            throw new HospitalRequiredException("User is not associated with an assigned hospital");
        }

        if (!hospital.isEnabled()) {
            logger.warn("Access rejected: hospital '{}' (ID: {}) for user '{}' is disabled",
                    hospital.getName(), hospital.getId(), user.getUsername());
            throw new HospitalRequiredException("Associated hospital '" + hospital.getName() + "' is currently disabled");
        }

        return hospital;
    }

    /**
     * Returns an Optional containing the current user's hospital if assigned.
     */
    public Optional<Hospital> getCurrentHospital() {
        return getCurrentUser().map(User::getHospital);
    }

    /**
     * Checks if the authenticated user is a platform-level administrator (ROLE_ADMIN with no hospital).
     */
    public boolean isPlatformAdmin() {
        return getCurrentUser().map(user -> {
            String role = user.getRole();
            boolean hasAdminRole = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
            return hasAdminRole && user.getHospital() == null;
        }).orElse(false);
    }

    /**
     * Checks if the authenticated user is a hospital-level administrator (ROLE_ADMIN with an assigned hospital).
     */
    public boolean isHospitalAdmin() {
        return getCurrentUser().map(user -> {
            String role = user.getRole();
            boolean hasAdminRole = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
            return hasAdminRole && user.getHospital() != null;
        }).orElse(false);
    }

    /**
     * Validates that the current user has authority to access data belonging to the specified hospital ID.
     * Platform administrators have platform-wide access when explicitly authorized.
     * Hospital users/admins are strictly restricted to their own hospital ID.
     *
     * @param targetHospitalId ID of the hospital owning the requested resource
     * @throws TenantAccessDeniedException if user attempts cross-tenant access
     * @throws HospitalRequiredException   if user has no hospital and is not platform admin
     */
    public void validateHospitalAccess(Long targetHospitalId) {
        if (isPlatformAdmin()) {
            return;
        }

        Hospital hospital = requireCurrentHospital();
        if (targetHospitalId == null || !hospital.getId().equals(targetHospitalId)) {
            logger.warn("Cross-tenant violation: user '{}' (hospital {}) attempted access to hospital {}",
                    getCurrentAuthenticatedUser().getUsername(), hospital.getId(), targetHospitalId);
            throw new TenantAccessDeniedException("Access denied: cannot access data belonging to a different hospital");
        }
    }

    /**
     * Validates that a persisted entity belonging to resourceHospital can be accessed by the current user.
     *
     * @param resourceHospital hospital owning the resource
     * @throws TenantAccessDeniedException if hospital does not match or resource is unassigned
     */
    public void validateResourceHospital(Hospital resourceHospital) {
        if (isPlatformAdmin()) {
            return;
        }

        if (resourceHospital == null) {
            logger.warn("Access rejected: resource has no valid hospital association");
            throw new TenantAccessDeniedException("Access denied: resource has no valid hospital association");
        }

        validateHospitalAccess(resourceHospital.getId());
    }
}
