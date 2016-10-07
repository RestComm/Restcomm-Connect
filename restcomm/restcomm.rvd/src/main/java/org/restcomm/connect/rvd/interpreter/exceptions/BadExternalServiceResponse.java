package org.restcomm.connect.rvd.interpreter.exceptions;

import org.restcomm.connect.rvd.exceptions.InterpreterException;

/**
 * The response to an External Service request is invalid. Assignment expressions
 * may not be properly evaluated. If HTTP errors are returned use the RemoteServiceError exception.
 *
 * @author "Tsakiridis Orestis"
 *
 */
public class BadExternalServiceResponse extends InterpreterException {

    private static final long serialVersionUID = 8310785550104901820L;

    public BadExternalServiceResponse() {
        super();
        // TODO Auto-generated constructor stub
    }

    public BadExternalServiceResponse(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public BadExternalServiceResponse(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }


}
