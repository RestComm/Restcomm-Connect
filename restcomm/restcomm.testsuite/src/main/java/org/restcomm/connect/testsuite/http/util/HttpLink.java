package org.restcomm.connect.testsuite.http.util;

import java.net.URI;
import org.apache.http.client.methods.HttpRequestBase;

public class HttpLink extends HttpRequestBase {

    public static final String METHOD_NAME = "LINK";

    public HttpLink() {
        super();
    }

    public HttpLink(final URI uri) {
        super();
        setURI(uri);
    }

    /**
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpLink(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }

}
