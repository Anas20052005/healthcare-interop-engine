package com.example.healthcare.exception;

/**
 * Exception thrown when attempting to create or update a hospital with a code that already exists.
 */
public class DuplicateHospitalCodeException extends RuntimeException {

    public DuplicateHospitalCodeException(String message) {
        super(message);
    }
}
