package org.acme.auth.service.exception;

/**
 * Exception thrown when a user is not found by their Distinguished Name (DN).
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String dn) {
        super("User not found with DN: " + dn);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
