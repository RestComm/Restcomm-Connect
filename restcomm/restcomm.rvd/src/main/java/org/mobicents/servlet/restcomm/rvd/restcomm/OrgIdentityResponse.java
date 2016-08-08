package org.mobicents.servlet.restcomm.rvd.restcomm;

/**
 *
 * @author Orestis Tsakiridis
 */
public class OrgIdentityResponse {
    String sid;
    String name;

    public OrgIdentityResponse(String sid, String name) {
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