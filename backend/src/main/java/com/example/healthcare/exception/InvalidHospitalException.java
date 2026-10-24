package com.example.healthcare.exception;

/**
 * Exception thrown when hospital operations violate business constraints,
 * such as associating a user with an inactive/disabled hospital.
 */
public class InvalidHospitalException extends RuntimeException {

    public InvalidHospitalException(String message) {
        super(message);
    }
}
