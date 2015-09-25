package org.mobicents.servlet.restcomm.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.mobicents.servlet.restcomm.HttpConnector;
import org.mobicents.servlet.restcomm.HttpConnectorList;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * Utility class to manipulate URI.
 * @author Henrique Rosa
 */
@ThreadSafe
public final class UriUtils {

    /**
     * Default constructor.
     */
    private UriUtils() {
        super();
    }

    /**
     * Resolves a relative URI.
     * @param base The base of the URI
     * @param uri The relative URI.
     * @return The absolute URI
     */
    public static URI resolve(final URI base, final URI uri) {
        if (base.equals(uri)) {
            return uri;
        } else {
            if (!uri.isAbsolute()) {
                return base.resolve(uri);
            } else {
                return uri;
            }
        }
    }

    /**
     * Resolves a relative URI.
     * @param address The IP address of the base URI .
     * @param port The port of the base URI.
     * @param uri The relative URI
     * @return The absolute URI
     */
    public static URI resolve(final ServletContext context, final String localAddress, final URI uri) {
        HttpConnectorList httpConnectorList = (HttpConnectorList) context.getAttribute(HttpConnectorList.class.getName());
        HttpConnector httpConnector = null;
        if (httpConnectorList != null && !httpConnectorList.getConnectors().isEmpty()) {
            List<HttpConnector> connectors = httpConnectorList.getConnectors();
            Iterator<HttpConnector> iterator = connectors.iterator();
            while (iterator.hasNext()) {
                HttpConnector connector = iterator.next();
                if (connector.isSecure()) {
                    httpConnector = connector;
                }
            }
            if (httpConnector == null) {
                httpConnector = connectors.get(0);
            }
        }
        //HttpConnector address could be a local address while the request came from a public address
        String address;
        if (httpConnector.getAddress().equalsIgnoreCase(localAddress)) {
            address = httpConnector.getAddress();
        } else {
            address = localAddress;
        }
        String base = httpConnector.getScheme()+"://" + address + ":" + httpConnector.getPort();
        try {
            return resolve(new URI(base), uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Badly formed URI: " + base, e);
        }
    }
}
