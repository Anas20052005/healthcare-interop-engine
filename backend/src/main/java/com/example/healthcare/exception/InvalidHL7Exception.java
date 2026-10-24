package com.example.healthcare.exception;

/**
 * Exception thrown when an HL7 message is syntactically invalid or unparseable.
 */
public class InvalidHL7Exception extends RuntimeException {

    public InvalidHL7Exception(String message) {
        super(message);
    }

    public InvalidHL7Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
