package org.restcomm.connect.rvd.restcomm;

/**
 *
 * @author Orestis Tsakiridis
 */
public class IdentityInstanceResponse {
    String sid;
    String name;

    public IdentityInstanceResponse(String sid, String name) {
        this.sid = sid;
        this.name = name;
    }

    public String getSid() {
        return sid;
    }

    public String getName() {
        return name;
    }
}