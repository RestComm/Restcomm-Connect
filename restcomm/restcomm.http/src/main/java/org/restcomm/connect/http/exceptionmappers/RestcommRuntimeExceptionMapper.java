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

package org.restcomm.connect.http.exceptionmappers;

import org.apache.log4j.Logger;
import org.restcomm.connect.dao.exceptions.AccountHierarchyDepthCrossed;
import org.restcomm.connect.common.exceptions.InsufficientPermission;
import org.restcomm.connect.http.exceptions.NotAuthenticated;
import org.restcomm.connect.http.exceptions.OperatedAccountMissing;
import org.restcomm.connect.http.exceptions.ResourceAccountMissmatch;
import org.restcomm.connect.commons.exceptions.RestcommRuntimeException;

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
public class RestcommRuntimeExceptionMapper implements ExceptionMapper<RestcommRuntimeException> {
    static final Logger logger = Logger.getLogger(RestcommRuntimeExceptionMapper.class.getName());

    @Override
    public Response toResponse(RestcommRuntimeException e) {
        if (logger.isDebugEnabled())
            logger.debug("Converting response to a corresponding http status.");

        if (e instanceof NotAuthenticated) {
            return Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate","Basic realm=\"Restcomm realm\"").build();
        }
        else
        if (e instanceof InsufficientPermission)
            return Response.status(Response.Status.FORBIDDEN).build();
        else
        if (e instanceof OperatedAccountMissing){
            // Return 404 if the account specified in the url (operated account) was missing - NULL.
            return Response.status(Response.Status.NOT_FOUND).build();
        } else
        if (e instanceof ResourceAccountMissmatch) {
            // in case the resource does belong to the user that it is accessed under, the url is wrong. Throw 400.
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else
        if (e instanceof AccountHierarchyDepthCrossed) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        else {
            // map all other types of auth errors to 403
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }
}
