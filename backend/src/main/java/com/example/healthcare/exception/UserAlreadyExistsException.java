package com.example.healthcare.exception;

/**
 * Exception thrown when attempting to create a user with a duplicate username.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
