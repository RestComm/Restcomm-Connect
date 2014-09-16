package org.mobicents.servlet.restcomm.rvd.security;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

/**
 * A filter that keeps authenticated sessions alive by extending the cookie expiration time.
 * It is crucial that this filter is executed AFTER the AuthenticationFilter
 * @author "Tsakiridis Orestis"
 *
 */
public class SessionKeepAliveFilter implements ResourceFilter, ContainerResponseFilter {
    static final Logger logger = Logger.getLogger(SessionKeepAliveFilter.class.getName());

    public SessionKeepAliveFilter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        //logger.info("Running SessionKeepAliveFilter");
        MultivaluedMap<String, String> cookiesMap = request.getCookieNameValueMap();
        List<String> cookies = cookiesMap.get("Cookie");
        List<String> ticketCookies = cookiesMap.get(RvdConfiguration.TICKET_COOKIE_NAME);
        if ( ticketCookies != null  &&  ticketCookies.size() > 0 ) {
            // If there is a valid ticket cookie in the request, extend its expiration time. Invalid ticket cookes are removed from the request by the Authentication filter which should have already run
            String ticketRaw = ticketCookies.get(0);
            NewCookie newCookie = SecurityUtils.createTicketCookieRaw(ticketRaw);
            response.getHttpHeaders().add("Set-Cookie", newCookie.toString());
            //logger.info("Renewed rvdticket cookie to: " + newCookie.toString());
        } else {
            //logger.info("No rvdticket cookie in request");
        }
        return response;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return this;
    }

}
