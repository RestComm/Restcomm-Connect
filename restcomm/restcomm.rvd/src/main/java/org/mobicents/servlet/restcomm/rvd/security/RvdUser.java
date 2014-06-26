package org.mobicents.servlet.restcomm.rvd.security;

import java.security.Principal;

public class RvdUser implements Principal {

    private String name; // the name of the Restcomm user

    public RvdUser() {
    }

    public RvdUser(String name) {
        super();
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "" + name;
    }
}
