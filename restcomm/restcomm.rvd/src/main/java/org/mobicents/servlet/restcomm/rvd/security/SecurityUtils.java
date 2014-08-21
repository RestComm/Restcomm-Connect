package org.mobicents.servlet.restcomm.rvd.security;

import javax.ws.rs.core.NewCookie;

import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;

public class SecurityUtils {

    /*
     * Encapsulate rvdticket cookie creation. It is done both when logging in and when renewing the ticket (in any authenticated request)
     */
    public static NewCookie createTicketCookie( String ticketId ) {
        return new NewCookie(RvdConfiguration.TICKET_COOKIE_NAME, ticketId, "/restcomm-rvd/services", null, null,1800, false );
    }

}
