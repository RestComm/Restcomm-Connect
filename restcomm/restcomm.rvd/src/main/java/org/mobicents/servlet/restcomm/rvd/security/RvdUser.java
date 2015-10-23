package org.mobicents.servlet.restcomm.rvd.security;

import java.security.Principal;

public class RvdUser implements Principal {

    private String name; // the name of the Restcomm user
    private String ticketId;

    public RvdUser(String name, String ticketId) {
        super();
        this.name = name;
        this.ticketId = ticketId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTicketId() {
        return ticketId;
    }

    @Override
    public String toString() {
        return "" + name;
    }
}
