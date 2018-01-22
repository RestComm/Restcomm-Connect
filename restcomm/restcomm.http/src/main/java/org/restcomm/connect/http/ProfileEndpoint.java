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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.sun.jersey.api.client.ClientResponse.Status;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import java.nio.charset.Charset;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.ProfileAssociationsDao;
import org.restcomm.connect.dao.ProfilesDao;
import org.restcomm.connect.dao.entities.Profile;
import org.restcomm.connect.dao.entities.ProfileAssociation;

public class ProfileEndpoint extends SecuredEndpoint {

    //TODO compose schema location
    protected static final String PROFILE_CONTENT_TYPE = "application/instance+json;schema=";
    protected static final String PROFILE_SCHEMA_CONTENT_TYPE = "application/schema+json";
    protected static final String LINK_HEADER = "Link";
    protected static final String PROFILE_ENCODING = "UTF-8";

    private Configuration runtimeConfiguration;
    private Configuration rootConfiguration; // top-level configuration element
    private ProfilesDao profilesDao;
    private ProfileAssociationsDao profileAssociationsDao;

    private JsonNode schemaJson;
    private JsonSchema profileSchema;

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
        profilesDao = ((DaoManager) context.getAttribute(DaoManager.class.getName())).getProfilesDao();
        try {
            schemaJson = JsonLoader.fromResource("/org/restcomm/connect/http/schemas/rc-profile-schema.json");
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            profileSchema = factory.getJsonSchema(schemaJson);
        } catch (Exception e) {
            logger.error("Error starting Profile endpoint.", e);
        }
    }

    public Response getProfiles(UriInfo info) {
        try {
            List<Profile> allProfiles = profilesDao.getAllProfiles();
            return Response.ok(allProfiles, MediaType.APPLICATION_JSON).build();
        } catch (SQLException ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    public Response unlinkProfile(String profileSid, HttpHeaders headers) {
        List<String> requestHeader = headers.getRequestHeader(LINK_HEADER);
        if (requestHeader.size() == 1) {
            //TODO parse link header
            String targetSid = requestHeader.get(0);
            profileAssociationsDao.deleteProfileAssociationByTargetSid(targetSid);
            return Response.ok().build();
        } else {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    public Response linkProfile(String profileSidStr, HttpHeaders headers) {
        List<String> requestHeader = headers.getRequestHeader(LINK_HEADER);
        if (requestHeader.size() == 1) {
            Sid profileSid = new Sid(profileSidStr);
            //TODO parse link header
            Sid targetSid = new Sid(requestHeader.get(0));
            ProfileAssociation assoc = new ProfileAssociation(profileSid, targetSid, new Date(), new Date());
            profileAssociationsDao.addProfileAssociation(assoc);
            return Response.ok().build();
        } else {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    public Response deleteProfile(String profileSid) {
        profilesDao.deleteProfile(profileSid);
        return Response.ok().build();
    }

    public Response updateProfile(String profileSid, InputStream body) {
        try {
            String profileStr = IOUtils.toString(body, Charset.forName(PROFILE_ENCODING));
            final JsonNode profileJson = JsonLoader.fromString(profileStr);
            ProcessingReport report = profileSchema.validate(profileJson);
            if (report.isSuccess()) {
                Profile profile = new Profile(profileSid, profileStr.getBytes(), new Date(), new Date());
                profilesDao.updateProfile(profile);
                return Response.ok().build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(report.toString()).build();
            }
        } catch (Exception ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    public Response getProfile(String profileSid) {
        try {
            Profile profile = profilesDao.getProfile(profileSid);
            Response.ResponseBuilder ok = Response.ok(profile.getProfileDocument());
            List<ProfileAssociation> profileAssociationsByProfileSid = profileAssociationsDao.getProfileAssociationsByProfileSid(profileSid);
            for (ProfileAssociation assoc : profileAssociationsByProfileSid) {
                //TODO compose link header
                ok.header(LINK_HEADER, "http://cloud.restcomm.comm/Profiles/" + assoc.getTargetSid());
            }
            String profileStr = IOUtils.toString(profile.getProfileDocument(), PROFILE_ENCODING);
            ok.entity(profileStr);
            ok.lastModified(profile.getDateUpdated());
            ok.type(PROFILE_CONTENT_TYPE);
            return ok.build();
        } catch (Exception ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    public Response createProfile(InputStream body) {

        Response response;
        try {
            Sid profileSid = Sid.generate(Sid.Type.PROFILE);
            String profileStr = IOUtils.toString(body, Charset.forName(PROFILE_ENCODING));
            final JsonNode profileJson = JsonLoader.fromString(profileStr);
            ProcessingReport report = profileSchema.validate(profileJson);
            if (report.isSuccess()) {
                Profile profile = new Profile(profileSid.toString(), profileStr.getBytes(), new Date(), new Date());
                profilesDao.addProfile(profile);
                URI location = new URI("http://cloud.restcomm.comm/Profiles/" + profileSid);
                response = Response.created(location).build();
            } else {
                response = Response.status(Response.Status.BAD_REQUEST).entity(report.toString()).build();
            }
        } catch (Exception ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
        return response;
    }

    public Response getProfileSchema() {
        return Response.ok(schemaJson.asText(), PROFILE_SCHEMA_CONTENT_TYPE).build();
    }
}
