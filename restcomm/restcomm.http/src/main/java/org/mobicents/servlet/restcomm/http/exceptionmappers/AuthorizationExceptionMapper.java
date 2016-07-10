/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.http.exceptionmappers;

import org.mobicents.servlet.restcomm.http.exceptions.AccountNotLinked;
import org.mobicents.servlet.restcomm.http.exceptions.AuthorizationException;
import org.mobicents.servlet.restcomm.http.exceptions.InsufficientPermission;
import org.mobicents.servlet.restcomm.http.exceptions.NotAuthenticated;
import org.mobicents.servlet.restcomm.http.exceptions.NoMappedAccount;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Generates 401 for AuthorizationException instead of a 500 (and an exception to the log). It helps to handle
 * authorization errors when they occur in *Endpoint.init() method where there is no explicit way of returning
 * a response (feature was  needed after shiro removal).
 *
 * @author Orestis Tsakiridis
 */
@Provider
public class AuthorizationExceptionMapper implements ExceptionMapper<AuthorizationException> {
    @Override
    public Response toResponse(AuthorizationException e) {
        if (e instanceof NotAuthenticated) {
            return Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate","Basic realm=\"Restcomm realm\"").build();
        }
        else
        if (e instanceof InsufficientPermission)
            return Response.status(Response.Status.FORBIDDEN).build();
        else
        if (e instanceof AccountNotLinked) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"ACCOUNT_NOT_LINKED\"}").type(MediaType.APPLICATION_JSON_TYPE).build();
        } else
        if (e instanceof NoMappedAccount) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"NO_MAPPED_ACCOUNT\"}").type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        else {
            // map all other types of auth errors to 403
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }
}
