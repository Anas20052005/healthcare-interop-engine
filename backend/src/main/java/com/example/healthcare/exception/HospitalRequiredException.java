package com.example.healthcare.exception;

/**
 * Exception thrown when a user without an assigned hospital attempts to access
 * or perform operations on hospital-specific healthcare data.
 */
public class HospitalRequiredException extends RuntimeException {

    public HospitalRequiredException(String message) {
        super(message);
    }

    public HospitalRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
