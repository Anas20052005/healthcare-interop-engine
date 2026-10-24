package com.example.healthcare.exception;

/**
 * Exception thrown when an error occurs during HL7 to FHIR resource mapping.
 */
public class HL7MappingException extends RuntimeException {

    public HL7MappingException(String message) {
        super(message);
    }

    public HL7MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
