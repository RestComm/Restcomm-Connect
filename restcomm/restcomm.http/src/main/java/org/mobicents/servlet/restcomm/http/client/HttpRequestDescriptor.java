/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.http.client;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class HttpRequestDescriptor {
    private final URI uri;
    private final String method;
    private final List<NameValuePair> parameters;

    public HttpRequestDescriptor(final URI uri, final String method, final List<NameValuePair> parameters) {
        super();
        this.uri = base(uri);
        this.method = method;
        if (parameters != null) {
            this.parameters = parameters;
        } else {
            this.parameters = new ArrayList<NameValuePair>();
        }
        final String query = uri.getQuery();
        if (query != null) {
            final List<NameValuePair> other = URLEncodedUtils.parse(uri, "UTF-8");
            parameters.addAll(other);
        }
    }

    public HttpRequestDescriptor(final URI uri, final String method) throws UnsupportedEncodingException, URISyntaxException {
        this(uri, method, null);
    }

    private URI base(final URI uri) {
        try {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setScheme(uri.getScheme());
            uriBuilder.setHost(uri.getHost());
            uriBuilder.setPort(uri.getPort());
            uriBuilder.setPath(uri.getPath());

            if(uri.getUserInfo() != null){
                uriBuilder.setUserInfo(uri.getUserInfo());
            }
            return uriBuilder.build();
        } catch (final URISyntaxException ignored) {
            // This should never happen since we are using a valid URI to construct ours.
            return null;
        }
    }

    public String getMethod() {
        return method;
    }

    public List<NameValuePair> getParameters() {
        return parameters;
    }

    public String getParametersAsString() {
        return URLEncodedUtils.format(parameters, "UTF-8");
    }

    public URI getUri() {
        return uri;
    }
}
