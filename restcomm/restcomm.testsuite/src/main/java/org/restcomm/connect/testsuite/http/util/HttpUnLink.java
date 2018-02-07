package org.restcomm.connect.testsuite.http.util;

import java.net.URI;
import org.apache.http.client.methods.HttpRequestBase;

public class HttpUnLink extends HttpRequestBase {

    public static final String METHOD_NAME = "UNLINK";

    public HttpUnLink() {
        super();
    }

    public HttpUnLink(final URI uri) {
        super();
        setURI(uri);
    }

    /**
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpUnLink(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }

}
