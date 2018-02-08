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
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import javax.ws.rs.ext.Provider;
import org.apache.log4j.Logger;

/**
 * Allows nonCompliant Clients to call LINK/UNLINK methods using POST/PUT
 *
 * @author
 */
@Provider
public class MethodOverrideFilter implements ResourceFilter, ContainerRequestFilter {

    protected Logger logger = Logger.getLogger(MethodOverrideFilter.class);

    private static final String OVERRIDE_HDR = "X-HTTP-Method-Override";

    @Override
    public ContainerRequest filter(ContainerRequest cr) {
        if (cr.getHeaderValue(OVERRIDE_HDR) != null) {
            cr.setMethod(cr.getHeaderValue(OVERRIDE_HDR));
        }
        return cr;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return null;
    }

}
