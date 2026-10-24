package com.example.healthcare.exception;

/**
 * Exception thrown when an authenticated user attempts to access or modify data
 * belonging to a different hospital tenant (cross-tenant violation).
 */
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }

    public TenantAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
