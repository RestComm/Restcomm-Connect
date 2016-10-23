package org.restcomm.connect.rvd.jsonvalidation.exceptions;

import org.restcomm.connect.rvd.exceptions.RvdException;

/**
 * Thrown when an error prevents validation. For instance when the schema files cannot be found
 * or an IO error occurs. This error DOES NOT mean that a project is invalid. Such validation
 * error are returned as a ValidationResult object and are treated differently.
 */
public class ValidationFrameworkException extends RvdException {

    public ValidationFrameworkException() {
        // TODO Auto-generated constructor stub
    }

    public ValidationFrameworkException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public ValidationFrameworkException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
