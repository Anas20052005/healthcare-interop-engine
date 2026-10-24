package com.example.healthcare.service;

import com.example.healthcare.dto.user.CreateUserRequest;
import com.example.healthcare.dto.user.CreateUserResponse;
import com.example.healthcare.dto.user.UserResponse;

import java.util.List;

/**
 * Service defining user management operations with multi-tenant isolation.
 */
public interface UserService {

    /**
     * Creates a new user or administrator with BCrypt password encryption.
     * Enforces tenant isolation: hospital admins can only create users in their own hospital,
     * and cannot create platform administrators.
     *
     * @param request creation request with username, password, and role
     * @return response with created user details (excluding password)
     */
    CreateUserResponse createUser(CreateUserRequest request);

    /**
     * Lists users scoped to the authenticated caller's tenant context.
     * Platform admins see all users; hospital admins see only users belonging to their hospital.
     *
     * @return list of user responses
     */
    List<UserResponse> getAllUsers();

    /**
     * Retrieves a user by ID with strict tenant boundary checks.
     *
     * @param id user ID
     * @return user details
     */
    UserResponse getUserById(Long id);

    /**
     * Updates an existing user account with tenant isolation enforcement.
     * Platform administrators can update any user's role, hospital, and status.
     * Hospital administrators can only update users within their own hospital and cannot
     * change hospital association or escalate roles.
     *
     * @param id user ID
     * @param request update request
     * @return updated user details
     */
    UserResponse updateUser(Long id, com.example.healthcare.dto.user.UpdateUserRequest request);
}
