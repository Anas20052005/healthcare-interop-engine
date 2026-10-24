package com.example.healthcare.exception;

/**
 * Exception thrown when a requested hospital cannot be found.
 */
public class HospitalNotFoundException extends RuntimeException {

    public HospitalNotFoundException(String message) {
        super(message);
    }
}
