package org.mobicents.servlet.restcomm.identity.mocks;

import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author orestis.tsakiridis@gmail.com  - Orestis Tsakiridis
 */
public class Organization {
    Sid sid;
    String domain;

    public Organization(Sid sid, String domain) {
        this.sid = sid;
        this.domain = domain;
    }

    public Sid getSid() {
        return sid;
    }

    public void setSid(Sid sid) {
        this.sid = sid;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
