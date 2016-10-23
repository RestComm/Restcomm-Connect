package org.restcomm.connect.rvd.exceptions.callcontrol;
/**
 * Thrown when trying to access the call control services without proper accessToken parameter
 *
 * @author "Tsakiridis Orestis"
 */
public class UnauthorizedCallControlAccess extends CallControlException {

    private String remoteIP;


    public UnauthorizedCallControlAccess(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public UnauthorizedCallControlAccess(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    public UnauthorizedCallControlAccess setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
        return this;
    }
}
