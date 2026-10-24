package com.example.healthcare.exception;

/**
 * Exception thrown when a destination configuration is invalid or violates SSRF protection rules.
 */
public class InvalidDestinationException extends RuntimeException {

    public InvalidDestinationException(String message) {
        super(message);
    }
}
