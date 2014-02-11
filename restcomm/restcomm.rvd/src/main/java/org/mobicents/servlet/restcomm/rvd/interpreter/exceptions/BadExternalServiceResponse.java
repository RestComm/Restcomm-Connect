package org.mobicents.servlet.restcomm.rvd.interpreter.exceptions;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;

/**
 * The response to an External Service request is invalid. Assignment expressions
 * may not be properly evaluated
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

}
