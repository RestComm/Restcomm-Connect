package org.mobicents.servlet.restcomm.rvd.security;

import javax.ws.rs.core.NewCookie;

import org.apache.commons.codec.binary.Base64;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.security.exceptions.InvalidTicketCookie;

import java.nio.charset.Charset;

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

    public static String getUsernameFromTicketCookie(String ticketCookie) throws InvalidTicketCookie {
        String[] ticketParts = ticketCookie.split(":");
        if ( ticketParts.length == 2 ) {
            return ticketParts[0];
        } else
            throw new InvalidTicketCookie("Ivalid ticket cookie: " + ticketCookie);
    }

    public static String getTicketIdFromTicketCookie(String ticketCookie) throws InvalidTicketCookie {
        String[] ticketParts = ticketCookie.split(":");
        if ( ticketParts.length == 2 ) {
            return ticketParts[1];
        } else
            throw new InvalidTicketCookie("Ivalid ticket cookie: " + ticketCookie);
    }

    /**
     * Extracts username and password from a Basic HTTP "Authorization" header. Expects only the value
     * of the header. Thus, for header "Authorization: xyx" it expects only the "xyz" part.
     *
     * @param headerValue
     * @return a BasicAuthCredentials object or null if no credentials are found or a parsing error occurs
     */
    public static BasicAuthCredentials parseBasicAuthHeader(String headerValue) {
        if (headerValue != null) {
            String[] parts = headerValue.split(" ");
            if (parts.length >= 2 && parts[0].equals("Basic")) {
                String base64Credentials = parts[1].trim();
                String credentials = new String(Base64.decodeBase64(base64Credentials), Charset.forName("UTF-8"));
                // credentials = username:password
                final String[] values = credentials.split(":",2);
                if (values.length >= 2) {
                    BasicAuthCredentials credentialsObj = new BasicAuthCredentials(values[0], values[1]);
                    return credentialsObj;
                }

            }
        }
        return null;
    }

}
