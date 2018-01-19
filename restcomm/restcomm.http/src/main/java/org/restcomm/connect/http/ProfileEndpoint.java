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
package org.restcomm.connect.http;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.ProfileAssociationsDao;

public class ProfileEndpoint extends SecuredEndpoint {
    //TODO compose schema location
    protected static final String PROFILE_CONTENT_TYPE = "application/instance+json;schema=";
    protected static final String PROFILE_SCHEMA_CONTENT_TYPE = "application/schema+json";
    protected static final String LINK_HEADER = "Link";

    protected Configuration runtimeConfiguration;
    protected Configuration rootConfiguration; // top-level configuration element
    //protected ProfilesDAO profileDao;
    protected ProfileAssociationsDao profileAssociationsDao;

    public ProfileEndpoint() {
        super();
    }

    // used for testing
    public ProfileEndpoint(ServletContext context, HttpServletRequest request) {
        super(context, request);
    }

    @PostConstruct
    void init() {
        rootConfiguration = (Configuration) context.getAttribute(Configuration.class.getName());
        runtimeConfiguration = rootConfiguration.subset("runtime-settings");
        super.init(runtimeConfiguration);
        profileAssociationsDao = ((DaoManager) context.getAttribute(DaoManager.class.getName())).getProfileAssociationsDao();
    }

    public Response getProfiles(UriInfo info) {

        return Response.ok("{}", MediaType.APPLICATION_JSON).build();
    }

    public Response unlinkProfile(String profileSid, HttpHeaders headers) {
        return Response.ok().build();
    }

    public Response linkProfile(String profileSid, HttpHeaders headers) {
        return Response.ok().build();
    }

    public Response deleteProfile(String profileSid) {
        return Response.ok().build();
    }

    public Response updateProfile(String profileSid, InputStream body) {
        return Response.ok().build();
    }

    public Response getProfile(String profileSid) {
        Response.ResponseBuilder ok = Response.ok();ok.header(LINK_HEADER, "http://cloud.restcomm.comm/Profiles/PO1234");
        return Response.ok().build();
    }

    public Response createProfile(InputStream body) {
        Response response ;
        try {
            URI location = new URI("http://cloud.restcomm.comm/Profiles/PO1234");
            response = Response.created(location).build();
        } catch (URISyntaxException ex) {
            response = Response.serverError().build();
        }
        return response;
    }

    public Response getProfileSchema() {
        return Response.ok("{}", PROFILE_SCHEMA_CONTENT_TYPE).build();
    }
}
