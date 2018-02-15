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

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.restcomm.connect.dao.exceptions.AccountHierarchyDepthCrossed;
import org.restcomm.connect.http.exceptions.InsufficientPermission;
import org.restcomm.connect.http.exceptions.NotAuthenticated;
import org.restcomm.connect.http.exceptions.OperatedAccountMissing;
import org.restcomm.connect.http.exceptions.ResourceAccountMissmatch;
import org.restcomm.connect.commons.exceptions.RestcommRuntimeException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Generates 401 for AuthorizationException instead of a 500 (and an exception
 * to the log). It helps to handle authorization errors when they occur in
 * *Endpoint.init() method where there is no explicit way of returning a
 * response (feature was needed after shiro removal).
 *
 * @author Orestis Tsakiridis
 */
@Provider
public class RestcommRuntimeExceptionMapper implements ExceptionMapper<RestcommRuntimeException> {

    static final Logger logger = Logger.getLogger(RestcommRuntimeExceptionMapper.class.getName());
    static final Map<Class, Response> exceptionMap = new HashMap();

    static {
        exceptionMap.put(NotAuthenticated.class,
                Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic realm=\"Restcomm realm\"").build());
        exceptionMap.put(InsufficientPermission.class,
                Response.status(Response.Status.FORBIDDEN).build());
        exceptionMap.put(OperatedAccountMissing.class,
                Response.status(Response.Status.NOT_FOUND).build());
        exceptionMap.put(ResourceAccountMissmatch.class,
                Response.status(Response.Status.BAD_REQUEST).build());
        exceptionMap.put(AccountHierarchyDepthCrossed.class,
                Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        exceptionMap.put(NotAuthenticated.class,
                Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic realm=\"Restcomm realm\"").build());

    }

    @Override
    public Response toResponse(RestcommRuntimeException e) {
        if (logger.isDebugEnabled()) {
            logger.debug("Converting response to a corresponding http status.");
        }
        Response res = null;

        if (exceptionMap.containsKey(e.getClass())) {
            res = exceptionMap.get(e.getClass());
        } else {
            // map all other types of auth errors to 403
            res = Response.status(Response.Status.FORBIDDEN).build();
        }
        return res;
    }
}
