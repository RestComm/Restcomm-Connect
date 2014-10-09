package org.mobicents.servlet.restcomm.rvd.http;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.model.LoginForm;
import org.mobicents.servlet.restcomm.rvd.security.AuthenticationService;
import org.mobicents.servlet.restcomm.rvd.security.SecurityUtils;
import org.mobicents.servlet.restcomm.rvd.security.Ticket;
import org.mobicents.servlet.restcomm.rvd.security.TicketRepository;
import org.mobicents.servlet.restcomm.rvd.security.exceptions.RvdSecurityException;

import com.google.gson.Gson;


@Path("auth")
public class LoginRestService extends RestService {
    static final Logger logger = Logger.getLogger(LoginRestService.class.getName());

    @Context
    ServletContext servletContext;
    RvdConfiguration rvdSettings;

    @PostConstruct
    void init() {
        rvdSettings = rvdSettings.getInstance(servletContext);
    }

    /*
    @GET
    @Path("login")
    public Response login(@Context HttpServletRequest request) {
        //logger.debug("Running login");

        // get username/password from request and authenticate against Restcomm
        // ...
        String userId = "administrator@company.com";
        String password = "RestComm";
        AuthenticationService authService = new AuthenticationService(rvdSettings, request);

        try {
            if ( authService.authenticate(userId, password) ) {
                logger.debug("User " + userId + " authenticated");

                // if authentication succeeds create a ticket for this user and return its id
                TicketRepository tickets = TicketRepository.getInstance();
                Ticket ticket = new Ticket(userId);
                tickets.putTicket( ticket );

                return Response.ok().cookie( SecurityUtils.createTicketCookie(ticket) ).build();
            }
            else {
                logger.debug("Authentication failed for user " + userId);
                return Response.status(Status.UNAUTHORIZED).cookie( SecurityUtils.createTicketCookie(null) ).build();
            }
        } catch (RvdSecurityException e) {
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, null);
        }
    }
    */

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("login")
    public Response postLogin(@Context HttpServletRequest request) throws IOException { // TODO make sure IOException is handled properly!
        //logger.debug("Running login");

        String data = IOUtils.toString(request.getInputStream());
        Gson gson = new Gson();
        LoginForm credentials = gson.fromJson(data,LoginForm.class);
        AuthenticationService authService = new AuthenticationService(rvdSettings, request);

        try {
            if ( authService.authenticate(credentials.getUsername(), credentials.getPassword()) ) {
                logger.debug("User " + credentials.getUsername() + " authenticated");

                // if authentication succeeds create a ticket for this user and return its id
                TicketRepository tickets = TicketRepository.getInstance();
                Ticket ticket = new Ticket(credentials.getUsername());
                tickets.putTicket( ticket );

                //return Response.ok().cookie( new NewCookie(RvdConfiguration.TICKET_COOKIE_NAME, ticket.getTicketId(), "/restcomm-rvd/services", null, null,3600, false ) ).build();
                return Response.ok().cookie( SecurityUtils.createTicketCookie(ticket) ).build();
            }
            else {
                logger.debug("Authentication failed for user " + credentials.getUsername());
                return Response.status(Status.UNAUTHORIZED).build();
            }
        } catch (RvdSecurityException e) {
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, null);
        }
    }


    @GET
    @Path("logout")
    public Response logout(@CookieParam(value = RvdConfiguration.TICKET_COOKIE_NAME) String ticketCookieValue) {
        TicketRepository tickets = TicketRepository.getInstance();
        logger.debug("Invalidating ticket " + ticketCookieValue);
        tickets.invalidateTicket(ticketCookieValue);
        // removing the cookie by setting the max-age to 0
        return Response.ok().cookie( SecurityUtils.createTicketCookie(null) ).build();
    }



}
