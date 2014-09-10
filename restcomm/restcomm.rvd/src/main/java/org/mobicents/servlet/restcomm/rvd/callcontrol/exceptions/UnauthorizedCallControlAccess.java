package org.mobicents.servlet.restcomm.rvd.callcontrol.exceptions;
/**
 * Thrown when trying to access the CC services without proper accessToken parameter
 * @author "Tsakiridis Orestis"
 *
 */
public class UnauthorizedCallControlAccess extends CallControlException {

    public UnauthorizedCallControlAccess() {
        // TODO Auto-generated constructor stub
    }

    public UnauthorizedCallControlAccess(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public UnauthorizedCallControlAccess(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
