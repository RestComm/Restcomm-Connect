package org.mobicents.servlet.restcomm.rvd.http;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.security.AuthenticationService;
import org.mobicents.servlet.restcomm.rvd.security.Ticket;
import org.mobicents.servlet.restcomm.rvd.security.TicketRepository;
import org.mobicents.servlet.restcomm.rvd.security.exceptions.RvdSecurityException;


@Path("auth")
public class LoginRestService extends RestService {
    static final Logger logger = Logger.getLogger(LoginRestService.class.getName());

    @Context
    ServletContext servletContext;

    RvdSettings rvdSettings;

    @PostConstruct
    void init() {
        rvdSettings = rvdSettings.getInstance(servletContext);
    }

    @GET
    @Path("login")
    public Response login(@Context HttpServletRequest request) {
        logger.debug("Running login");

        // get username/password from request and authenticate against Restcomm
        // ...
        String userId = "administrator@company.com";
        String password = "RestComm";
        AuthenticationService authService = new AuthenticationService(rvdSettings, request);

        try {
            if ( authService.authenticate(userId, password) ) {
                logger.debug("User " + userId + " authenticate succesfully");

                // if authentication succeeds create a ticket for this user and return its id
                TicketRepository tickets = TicketRepository.getInstance();
                Ticket ticket = new Ticket(userId);
                tickets.putTicket( ticket );

                return Response.ok().cookie( new NewCookie(RvdSettings.TICKET_COOKIE_NAME, ticket.getTicketId(), "/restcomm-rvd/services", null, null,3600, false ) ).build();
            }
            else {
                logger.debug("Authentication error for user " + userId);
                return Response.status(Status.UNAUTHORIZED).build();
            }
        } catch (RvdSecurityException e) {
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, null);
        }
    }


    @GET
    @Path("logout")
    public Response logout(@CookieParam(value = RvdSettings.TICKET_COOKIE_NAME) String ticketCookieValue) {
        TicketRepository tickets = TicketRepository.getInstance();
        logger.debug("Invalidating ticket " + ticketCookieValue);
        tickets.invalidateTicket(ticketCookieValue);

        // removing the cookie by setting the max-age to 0
        return Response.ok().cookie( new NewCookie(RvdSettings.TICKET_COOKIE_NAME, "", "/restcomm-rvd/services", null, null,0, false ) ).build();
    }



}
