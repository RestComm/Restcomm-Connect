/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.http.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.status;
import javax.ws.rs.ext.Provider;
import org.apache.log4j.Logger;
import org.restcomm.connect.http.exceptionmappers.CustomReasonPhraseType;



@Provider
public class BodyLengthFilter implements ContainerRequestFilter {
    protected Logger logger = Logger.getLogger(ExtensionFilter.class);

    private static final int MAX_REQ_BODY_SIZE = 10000000;

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        String length = request.getHeaderValue(HttpHeaders.CONTENT_LENGTH);
        if (length != null) {
            int bodySize = Integer.parseInt(length);
            if (bodySize > MAX_REQ_BODY_SIZE) {
                logger.debug("Request rejected by size");
                CustomReasonPhraseType customReasonPhraseType = new CustomReasonPhraseType(Status.Family.CLIENT_ERROR, 413, "Length larger than " + MAX_REQ_BODY_SIZE);
                Response build = status(customReasonPhraseType).build();
                throw new WebApplicationException(build);
            }
        }

        return request;
    }
}
