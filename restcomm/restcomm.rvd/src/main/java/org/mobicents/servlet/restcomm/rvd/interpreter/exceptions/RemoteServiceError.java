package org.mobicents.servlet.restcomm.rvd.interpreter.exceptions;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;

/**
 * The remote service failed returning an HTTP error code
 * @author "Tsakiridis Orestis"
 *
 */
public class RemoteServiceError extends InterpreterException {

    public RemoteServiceError() {
        // TODO Auto-generated constructor stub
    }

    public RemoteServiceError(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public RemoteServiceError(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
