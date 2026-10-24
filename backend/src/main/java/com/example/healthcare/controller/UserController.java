package com.example.healthcare.controller;

import com.example.healthcare.dto.user.CreateUserRequest;
import com.example.healthcare.dto.user.CreateUserResponse;
import com.example.healthcare.dto.user.UpdateUserRequest;
import com.example.healthcare.dto.user.UserResponse;
import com.example.healthcare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller providing user and administrator management endpoints.
 * All endpoints in this controller require ADMIN authority and enforce tenant isolation.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new system user or administrator.
     * Authorized only for users with ROLE_ADMIN.
     * Tenant boundary enforced: hospital admins can only create accounts within their own hospital.
     *
     * @param request creation request with username, password, and role
     * @return HTTP 201 with created user details (excluding password)
     */
    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all users accessible to the authenticated administrator.
     * Platform admins receive all users; hospital admins receive only users in their hospital.
     *
     * @return HTTP 200 with list of user responses
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves a single user by ID with tenant isolation boundary checks.
     *
     * @param id user ID
     * @return HTTP 200 with user details
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing user account.
     * Enforces strict tenant isolation: hospital admins can only manage accounts within their own hospital,
     * and cannot reassign hospital context or escalate privileges to platform admin.
     *
     * @param id user ID
     * @param request update request
     * @return HTTP 200 with updated user details
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }
}
