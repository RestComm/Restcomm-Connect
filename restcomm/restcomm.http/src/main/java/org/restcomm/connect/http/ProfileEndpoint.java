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
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.ProfileAssociationsDao;
import org.restcomm.connect.dao.ProfilesDao;
import org.restcomm.connect.dao.entities.Profile;
import org.restcomm.connect.dao.entities.ProfileAssociation;
import org.restcomm.connect.http.exceptions.OperatedAccountMissing;
import org.restcomm.connect.http.exceptions.ResourceAccountMissmatch;

public class ProfileEndpoint {

    protected Logger logger = Logger.getLogger(ProfileEndpoint.class);

    //TODO compose schema location
    protected static final String PROFILE_CONTENT_TYPE = "application/instance+json;schema=\"http://127.0.0.1\"";
    protected static final String PROFILE_SCHEMA_CONTENT_TYPE = "application/schema+json";
    protected static final String PROFILE_REL_TYPE = "related";
    protected static final String SCHEMA_REL_TYPE = "schema";
    protected static final String DESCRIBED_REL_TYPE = "describedby";
    protected static final String LINK_HEADER = "Link";
    protected static final String PROFILE_ENCODING = "UTF-8";

    @Context
    protected ServletContext context;

    private Configuration runtimeConfiguration;
    private Configuration rootConfiguration; // top-level configuration element
    private ProfilesDao profilesDao;
    private ProfileAssociationsDao profileAssociationsDao;

    private JsonNode schemaJson;
    private JsonSchema profileSchema;

    public ProfileEndpoint() {
        super();
    }

    @PostConstruct
    void init() {
        rootConfiguration = (Configuration) context.getAttribute(Configuration.class.getName());
        runtimeConfiguration = rootConfiguration.subset("runtime-settings");
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

    public Response unlinkProfile(String profileSidStr, HttpHeaders headers) {
        List<String> requestHeader = checkLinkHeader(headers);
        Link link = Link.valueOf(requestHeader.get(0));
        checkRelType(link);
        String targetSid = retrieveSid(link.getUri());
        profileAssociationsDao.deleteProfileAssociationByTargetSid(targetSid);
        return Response.ok().build();
    }

    private String retrieveSid(URI uri) {
        Path paths = Paths.get(uri.getPath());
        return paths.getName(paths.getNameCount() - 1).toString();
    }

    private void checkRelType(Link link) {
        if (!link.getRel().equals(PROFILE_REL_TYPE)) {
            throw new ResourceAccountMissmatch("rel type not supported");
        }
    }

    private List<String> checkLinkHeader(HttpHeaders headers) {
        List<String> requestHeader = headers.getRequestHeader(LINK_HEADER);
        if (requestHeader.size() != 1) {
            throw new ResourceAccountMissmatch();
        }
        return requestHeader;
    }

    public Response linkProfile(String profileSidStr, HttpHeaders headers, UriInfo uriInfo) {
        List<String> requestHeader = checkLinkHeader(headers);
        Link link = Link.valueOf(requestHeader.get(0));
        checkRelType(link);
        Sid targetSid = new Sid(retrieveSid(link.getUri()));
        Sid profileSid = new Sid(profileSidStr);
        ProfileAssociation assoc = new ProfileAssociation(profileSid, targetSid, new Date(), new Date());
        profileAssociationsDao.addProfileAssociation(assoc);
        return Response.ok().build();

    }

    public Response deleteProfile(String profileSid) {
        profilesDao.deleteProfile(profileSid);
        return Response.ok().build();
    }

    private Profile checkProfileExists(String profileSid) throws SQLException {
        Profile profile = profilesDao.getProfile(profileSid);
        if (profile != null) {
            return profile;
        } else {
            throw new OperatedAccountMissing("Profile not found:" + profileSid);
        }
    }

    public Response updateProfile(String profileSid, InputStream body) {
        try {
            checkProfileExists(profileSid);
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

    public Link composeSchemaLink(UriInfo info) throws MalformedURLException {
        URI build = info.getBaseUriBuilder().path("rc-profile-schema").build();
        return Link.fromUri(build).rel(DESCRIBED_REL_TYPE).build();
    }

    public Link composeLink(Sid targetSid, UriInfo info) throws MalformedURLException {
        String sid = targetSid.toString();
        URI uri = null;
        Link.Builder link = null;
        switch (sid.substring(1, 2)) {
            case "AC":
                uri = info.getBaseUriBuilder().path(AccountsJsonEndpoint.class).path(sid).build();
                link = Link.fromUri(uri).title("Accounts");
                break;
            case "OR":
                uri = info.getBaseUriBuilder().path(OrganizationsJsonEndpoint.class).path(sid).build();
                link = Link.fromUri(uri).title("Organizations");
                break;
            default:
        }
        if (link != null) {
            return link.type(PROFILE_REL_TYPE).build();
        } else {
            return null;
        }
    }

    public Response getProfile(String profileSid, UriInfo info) {
        try {
            Profile profile = checkProfileExists(profileSid);
            Response.ResponseBuilder ok = Response.ok(profile.getProfileDocument());
            List<ProfileAssociation> profileAssociationsByProfileSid = profileAssociationsDao.getProfileAssociationsByProfileSid(profileSid);
            for (ProfileAssociation assoc : profileAssociationsByProfileSid) {
                Link composeLink = composeLink(assoc.getTargetSid(), info);
                ok.header(LINK_HEADER, composeLink.toString());
            }
            ok.header(LINK_HEADER, composeSchemaLink(info));
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
                response = Response.created(location).type(PROFILE_CONTENT_TYPE).entity(profileStr).build();
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
