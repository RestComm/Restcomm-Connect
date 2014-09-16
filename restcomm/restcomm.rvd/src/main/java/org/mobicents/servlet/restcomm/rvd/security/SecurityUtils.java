package org.mobicents.servlet.restcomm.rvd.security;

import javax.ws.rs.core.NewCookie;

import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;

public class SecurityUtils {

    /*
     * Encapsulate rvdticket cookie creation. It is done both when logging in and when renewing the ticket (in any authenticated request)
     */
    public static NewCookie createTicketCookieRaw( String ticketCookie ) {
        //return new newcookie(rvdconfiguration.ticket_cookie_name, ticketid, "/restcomm-rvd/services", null, null,1800, false );
        return new NewCookie(RvdConfiguration.TICKET_COOKIE_NAME, ticketCookie, "/restcomm-rvd", null, null,1800, false );
    }

    public static NewCookie createTicketCookie( Ticket ticket ) {
        //return new NewCookie(RvdConfiguration.TICKET_COOKIE_NAME, ticketId, "/restcomm-rvd/services", null, null,1800, false );.getTicketId()
        if ( ticket == null )
            return new NewCookie(RvdConfiguration.TICKET_COOKIE_NAME, "", "/restcomm-rvd", null, null,1800, false );
        else
            return new NewCookie(RvdConfiguration.TICKET_COOKIE_NAME, ticket.getUserId() + ":" + ticket.getTicketId(), "/restcomm-rvd", null, null,1800, false );
    }

}
