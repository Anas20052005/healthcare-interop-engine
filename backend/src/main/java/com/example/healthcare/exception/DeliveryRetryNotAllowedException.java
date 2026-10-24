package com.example.healthcare.exception;

/**
 * Thrown when an attempt to retry a delivery is rejected
 * (e.g., delivery already succeeded, already in progress, or destination invalid).
 */
public class DeliveryRetryNotAllowedException extends RuntimeException {

    public DeliveryRetryNotAllowedException(String message) {
        super(message);
    }
}
