package org.mobicents.servlet.restcomm.rvd.security;



import java.net.URI;
import java.security.Principal;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

public class RvdResourceFilter implements ResourceFilter, ContainerRequestFilter {
    static final Logger logger = Logger.getLogger(RvdResourceFilter.class.getName());

    public RvdResourceFilter() {
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

        String path = request.getPath();
        URI baseuri = request.getBaseUri();
        Principal principal = request.getUserPrincipal();
        SecurityContext secContext = request.getSecurityContext();
        String authenticationScheme = secContext.getAuthenticationScheme();
        Principal principal2 = secContext.getUserPrincipal();
        
        Cookie ticketCookie = request.getCookies().get(RvdSettings.TICKET_COOKIE_NAME);
        if ( ticketCookie != null ) {
            String ticketId = ticketCookie.getValue();
            //throw new WebApplicationException();
            //String ticketId = "111"; // simulate retrieving ticketId from a header
            
            logger.debug("Received a request with ticket Id " + ticketId);
    
            TicketRepository tickets =  TicketRepository.getInstance();
            Ticket ticket = tickets.findTicket(ticketId);
            if ( ticket != null ) {
                logger.debug("granted access to request with ticket id" + ticketId);
                return request;
            }
        }
        
        logger.debug("denied access for request ");
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

}
