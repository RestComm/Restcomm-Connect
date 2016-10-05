package org.restcomm.connect.rvd.exceptions.callcontrol;

import org.restcomm.connect.rvd.exceptions.RvdException;

/**
 * The base class for all callcontrol related exceptions.
 * @author "Tsakiridis Orestis"
 *
 */
public class CallControlException extends RvdException {

    // the http status code the service that caused the exception should return
    private Integer statusCode;

    public CallControlException() {
        // TODO Auto-generated constructor stub
    }

    public CallControlException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public CallControlException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public CallControlException setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

}
