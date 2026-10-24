package com.example.healthcare.exception;

/**
 * Exception thrown when a requested destination is not found.
 */
public class DestinationNotFoundException extends RuntimeException {

    public DestinationNotFoundException(String message) {
        super(message);
    }
}
