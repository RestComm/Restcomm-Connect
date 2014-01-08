package org.mobicents.servlet.restcomm.util;

import java.net.URI;
import java.net.URISyntaxException;

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
    public static URI resolve(final String address, final int port, final URI uri) {
        String base = "http://" + address + ":" + port;
        try {
            return resolve(new URI(base), uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Badly formed URI: " + base, e);
        }
    }
}
