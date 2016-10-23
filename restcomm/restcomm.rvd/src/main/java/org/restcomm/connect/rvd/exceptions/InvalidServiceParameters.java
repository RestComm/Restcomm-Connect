package org.restcomm.connect.rvd.exceptions;

/*
 * Generic error when invalid parameters are passed to an any service. It usually is a trivial error.
 */
public class InvalidServiceParameters extends RvdException {

    public InvalidServiceParameters() {
        // TODO Auto-generated constructor stub
    }

    public InvalidServiceParameters(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public InvalidServiceParameters(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
