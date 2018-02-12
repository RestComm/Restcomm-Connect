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
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.status;
import javax.ws.rs.ext.Provider;
import org.apache.log4j.Logger;
import org.restcomm.connect.extension.api.ApiRequest;
import org.restcomm.connect.extension.api.ExtensionType;
import org.restcomm.connect.extension.api.RestcommExtensionGeneric;
import org.restcomm.connect.extension.controller.ExtensionController;
import org.restcomm.connect.http.exceptionmappers.CustomReasonPhraseType;

/**
 * Checks if this REST API request is allowed by Extensions.
 *
 * @author
 */
@Provider
public class ExtensionFilter implements ResourceFilter, ContainerRequestFilter, ContainerResponseFilter {

    protected Logger logger = Logger.getLogger(ExtensionFilter.class);

    private static final Map<String, ApiRequest.Type> TYPE_MAP = new HashMap();

    static {
        TYPE_MAP.put("AvailablePhoneNumbers", ApiRequest.Type.AVAILABLEPHONENUMBER);
        TYPE_MAP.put("IncomingPhoneNumbers", ApiRequest.Type.INCOMINGPHONENUMBER);
        TYPE_MAP.put("AvailablePhoneNumbers.json", ApiRequest.Type.AVAILABLEPHONENUMBER);
        TYPE_MAP.put("IncomingPhoneNumbers.json", ApiRequest.Type.INCOMINGPHONENUMBER);
    }


    private int retrieveAccountPathPosition(ContainerRequest cr) {
        boolean accountFound = false;
        int i = 0;
        List<PathSegment> pathSegments = cr.getPathSegments();
        while (i < pathSegments.size() && !accountFound) {
            if (pathSegments.get(i).getPath().startsWith("Accounts")) {
                accountFound = true;
            }
        }
        if (accountFound && i + 1 < pathSegments.size()) {
            //next path to "Accounts"
            i = i + 1;
        } else {
            i = -1;
        }
        return i;
    }

    private String retrieveAccountSid(ContainerRequest cr) {
        String accountSid = null;
        List<PathSegment> pathSegments = cr.getPathSegments();
        int accountPos = retrieveAccountPathPosition(cr);
        if (accountPos >= 0) {
            accountSid = pathSegments.get(accountPos).getPath();
        }
        return accountSid;
    }

    private ApiRequest.Type mapType(ContainerRequest cr) {
        ApiRequest.Type type = null;
        List<PathSegment> pathSegments = cr.getPathSegments();
        int accountPos = retrieveAccountPathPosition(cr);
        if (accountPos >= 0 && accountPos + 1 < pathSegments.size()) {
            String subResPath = pathSegments.get(accountPos + 1).getPath();
            type = TYPE_MAP.get(subResPath);
        }
        return type;
    }

    @Override
    public ContainerRequest filter(ContainerRequest cr) {
        final String accountSid = retrieveAccountSid(cr);
        ApiRequest.Type type = mapType(cr);
        final MultivaluedMap<String, String> data = cr.getFormParameters();
        if (accountSid != null && type != null) {
            ApiRequest apiRequest = new ApiRequest(accountSid, data, type);
            if (!executePreApiAction(apiRequest)) {
                CustomReasonPhraseType customReasonPhraseType = new CustomReasonPhraseType(FORBIDDEN, "Extension blocked access");
                Response build = status(customReasonPhraseType).build();
                throw new WebApplicationException(build);
            }
        }

        return cr;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return this;
    }

    private boolean executePreApiAction(final ApiRequest apiRequest) {
        List<RestcommExtensionGeneric> extensions = ExtensionController.getInstance().getExtensions(ExtensionType.RestApi);
        ExtensionController ec = ExtensionController.getInstance();
        return ec.executePreApiAction(apiRequest, extensions).isAllowed();
    }

    private boolean executePostApiAction(final ApiRequest apiRequest) {
        List<RestcommExtensionGeneric> extensions = ExtensionController.getInstance().getExtensions(ExtensionType.RestApi);
        ExtensionController ec = ExtensionController.getInstance();
        return ec.executePostApiAction(apiRequest, extensions).isAllowed();
    }

    @Override
    public ContainerResponse filter(ContainerRequest cr, ContainerResponse response) {
        final String accountSid = retrieveAccountSid(cr);
        ApiRequest.Type type = mapType(cr);
        final MultivaluedMap<String, String> data = cr.getFormParameters();
        ApiRequest apiRequest = new ApiRequest(accountSid, data, type);
        executePostApiAction(apiRequest);
        return response;
    }

}
