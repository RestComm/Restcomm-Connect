package org.mobicents.servlet.restcomm.rvd.interpreter.exceptions;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;

/**
 * The external (customer-provided) service has failed. A faulty http status code has been returned to ES proxy
 * @author "Tsakiridis Orestis"
 *
 */
public class ExternalServiceFailed extends InterpreterException {

    public ExternalServiceFailed() {
        // TODO Auto-generated constructor stub
    }

    public ExternalServiceFailed(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public ExternalServiceFailed(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
