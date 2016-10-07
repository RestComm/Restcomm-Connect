package org.restcomm.connect.rvd.identity;

import javax.servlet.http.HttpServletRequest;

/**
 * A class to encapsulate the logic of building an origin of an HTTP request (i.e. schema://host:port).
 * The reason is main to decouple from HttpServletRequest and have this conversion in a single place.
 *
 * @author Orestis Tsakiridis
 */
public class RequestOrigin {
    String origin;

    public RequestOrigin(String scheme, String host, Integer port) {
        if (port == 80 || port == -1)
            port = null;
        this.origin = scheme + "://" + host + (port == null ? "" : port);
    }

    public RequestOrigin(HttpServletRequest request) {
        this(request.getScheme(), request.getLocalName(), request.getLocalPort());
    }

    public String getOrigin() {
        return origin;
    }
}
