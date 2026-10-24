package com.example.healthcare.exception;

/**
 * Generic 404 Not Found exception for healthcare resources (conversions, patients, encounters, observations).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
