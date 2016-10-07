package org.restcomm.connect.rvd.interpreter.exceptions;

import org.restcomm.connect.rvd.exceptions.InterpreterException;

public class InvalidAccessOperationAction extends InterpreterException {

    /**
     * Invalid access operation action was found. This error may be thrown when building the
     * app. Getting ths error at runtime means that your validation of ExternalService
     * does not work well
     */
    private static final long serialVersionUID = -6768735386662256452L;

}
