package com.example.healthcare.exception;

/**
 * Exception thrown when a FHIR payload fails syntax or structure validation.
 */
public class InvalidFhirPayloadException extends RuntimeException {

    public InvalidFhirPayloadException(String message) {
        super(message);
    }
}
