package org.restcomm.connect.rvd.exceptions;

/**
 * @author Orestis Tsakiridis
 */
public class AuthorizationException extends RuntimeException {
    public AuthorizationException() {
    }

    public AuthorizationException(String message) {
        super(message);
    }
}
