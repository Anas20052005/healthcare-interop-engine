package com.example.healthcare.exception;

/**
 * Exception thrown when an HL7 message type is not supported by the system.
 */
public class UnsupportedHL7MessageException extends InvalidHL7Exception {

    public UnsupportedHL7MessageException(String message) {
        super(message);
    }

    public UnsupportedHL7MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
