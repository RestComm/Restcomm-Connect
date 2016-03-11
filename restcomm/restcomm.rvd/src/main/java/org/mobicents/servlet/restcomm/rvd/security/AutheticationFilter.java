package org.mobicents.servlet.restcomm.rvd.security;



import java.nio.charset.Charset;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.exceptions.UserNotAuthenticated;
import org.mobicents.servlet.restcomm.rvd.http.RvdResponse;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommAccountInfoResponse;
import org.mobicents.servlet.restcomm.rvd.security.exceptions.RvdSecurityException;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

public class AutheticationFilter implements ResourceFilter, ContainerRequestFilter {
    static final Logger logger = Logger.getLogger(AutheticationFilter.class.getName());

    public AutheticationFilter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return null;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        // ticket repository maintenance
        TicketRepository tickets =  TicketRepository.getInstance();
        tickets.remindStaleTicketRemoval();

        // handle basic http authentication headers
        boolean basicAuthStatus = false;
        Ticket basicAuthTicket = null;
        String authHeader = request.getHeaderValue("Authorization");
        String[] basicAuthCredentials = extractBasicAuthCredentials(authHeader);
        if ( basicAuthCredentials != null ) {
            String username = basicAuthCredentials[0];
            String authToken = basicAuthCredentials[1];
            if ( ! RvdUtils.isEmpty(username) && ! RvdUtils.isEmpty(authToken) ) {
                TicketRepository ticketRepo = TicketRepository.getInstance();
                // is there already a cached ticket for this username ?
                basicAuthTicket = ticketRepo.findTicket(username);
                if ( basicAuthTicket != null ) {
                    // ok, we have it cached. Let's authenticate against it to avoid contacting Restcomm again
                    if ( authToken.equals(basicAuthTicket.getAuthenticationToken() ) ) {
                        basicAuthTicket.accessedNow();
                        basicAuthStatus = true;
                        return wrapResponse(request, basicAuthTicket.getUserId(), basicAuthTicket.getTicketId());
                    }
                } else {
                    // try to authenticate
                    AuthenticationService authService = new AuthenticationService();
                    try {
                        RestcommAccountInfoResponse account = authService.authenticate(username, authToken);
                        if ( account != null ) {
                            Ticket newTicket = new Ticket(username, account.getEmail_address()); // username is whatever is authenticated by Restcomm. Email_Address is the exact email_address as stored in the account. In case Restcomm authenticates loosely there are not necessarily exactly the same.
                            newTicket.setAuthenticationToken(basicAuthCredentials[1]);
                            newTicket.setCookieBased(false);
                            tickets.putTicket( newTicket );
                            // create the user for the context
                            return wrapResponse(request, account.getEmail_address(), newTicket.getTicketId());
                        }
                    } catch (RvdSecurityException e1) {
                        logger.error("Internal error while authentication against restcomm for user '" + basicAuthCredentials[0] + "'", e1 );
                    }
                }
            }
        }

        Ticket cookieAuthTicket = null;
        boolean cookieAuthStatus = false;
        Cookie ticketCookie = request.getCookies().get(RvdConfiguration.TICKET_COOKIE_NAME);
        if ( ticketCookie != null ) {
            String rawTicket = ticketCookie.getValue();
            String[] ticketParts = rawTicket.split(":");

            if ( ticketParts.length == 2 ) {
                String ticketUsername = ticketParts[0];
                String ticketId = ticketParts[1];

                cookieAuthTicket = tickets.findTicket(ticketId);
                if ( cookieAuthTicket != null ) {
                    if ( cookieAuthTicket.getUserId() != null && cookieAuthTicket.getUserId().equals(ticketUsername) ) {
                        cookieAuthTicket.accessedNow();
                        cookieAuthStatus = true;
                        return wrapResponse(request, cookieAuthTicket.getUserId(), cookieAuthTicket.getTicketId());
                    }
                }
            }
            // Since access was not granted, this is probably an bad cookie. Remove it from the request so that it won't be renewed from the SessionKeepAliveFilter
            if ( !cookieAuthStatus )
                request.getCookies().remove(RvdConfiguration.TICKET_COOKIE_NAME);
        }

        logger.debug("denied access for request ");
        RvdException e = new UserNotAuthenticated();
        RvdResponse rvdResponse = new RvdResponse(RvdResponse.Status.ERROR).setException(e);
        Response res = Response.status(Status.UNAUTHORIZED).entity(rvdResponse.asJson()).type(MediaType.APPLICATION_JSON).build();
        throw new WebApplicationException( res );
    }

    private ContainerRequest wrapResponse(ContainerRequest request, String username, String ticketId) {
        RvdUser user = new RvdUser(username, ticketId);
        SecurityContext securityContext = new RvdSecurityContext(user);
        request.setSecurityContext(securityContext);
        return request;
    }

    /**
     * Extracts Basic HTTP authentication from a header and returns a
     * two element array. In case of error it returns null
     *
     * @param authHeader
     * @return
     */
    private String[] extractBasicAuthCredentials(String authHeader) {
        if (authHeader != null) {
            String[] parts = authHeader.split(" ");
            if (parts.length >= 2 && parts[0].equals("Basic")) {
                String base64Credentials = parts[1].trim();
                String credentials = new String(Base64.decodeBase64(base64Credentials), Charset.forName("UTF-8"));
                // credentials = username:password
                final String[] values = credentials.split(":",2);
                if (values.length >= 2) {
                    String[] res = new String[2];
                    res[0] = values[0];
                    res[1] = values[1];
                    return res;
                }

            }
        }
        return null;
    }

}
