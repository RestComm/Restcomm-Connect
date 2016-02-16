/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.*;

import java.net.URI;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.OrganizationsDao;
import org.mobicents.servlet.restcomm.entities.Organization;
import org.mobicents.servlet.restcomm.entities.OrganizationList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.ClientListConverter;
import org.mobicents.servlet.restcomm.http.converter.OrganizationConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author guilherme.jansen@telestax.com
 */
@NotThreadSafe
public class OrganizationsEndpoint extends AbstractEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected OrganizationsDao dao;
    protected Gson gson;
    protected XStream xstream;
    protected AccountsDao accountsDao;

    public OrganizationsEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        dao = storage.getOrganizationsDao();
        accountsDao = storage.getAccountsDao();
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        final OrganizationConverter converter = new OrganizationConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Organization.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new ClientListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    protected Organization createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
        final Organization.Builder builder = Organization.builder();
        final Sid sid = Sid.generate(Sid.Type.ORGANIZATION);
        builder.setSid(sid);
        builder.setFriendlyName(data.getFirst("FriendlyName"));
        builder.setNamespace(data.getFirst("Namespace"));
        builder.setAccountSid(accountSid);
        builder.setApiVersion(getApiVersion(data));
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
                .append("/Organizations/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    protected Response getOrganization(final String accountSid, final String sid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:Organizations");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        Organization organization = null;
        if (Sid.pattern.matcher(sid).matches()) {
            organization = dao.getOrganization(new Sid(sid));
        } else {
            organization = dao.getOrganization(sid);
        }
        if (organization == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secureLevelControl(accountsDao, accountSid, String.valueOf(organization.getAccountSid()));
            } catch (final AuthorizationException exception) {
                return status(UNAUTHORIZED).build();
            }
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(organization);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(organization), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    protected Response getOrganizations(final String accountSid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:Organizations");
            secureLevelControl(accountsDao, accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final List<Organization> organizations = dao.getOrganizations(new Sid(accountSid));
        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new OrganizationList(organizations));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(organizations), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response putOrganization(final String accountSid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:Organizations");
            secureLevelControl(accountsDao, accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        Organization organization = dao.getOrganization(data.getFirst("Namespace"));
        if (organization == null) {
            organization = createFrom(new Sid(accountSid), data);
            dao.addOrganization(organization);
        } else if (!organization.getAccountSid().toString().equals(accountSid)) {
            return status(CONFLICT)
                    .entity("A organization with the same namespace was already created by another account. Please, choose a different namespace and try again.")
                    .build();
        }
        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(organization);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(organization), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
        if (!data.containsKey("FriendlyName")) {
            throw new NullPointerException("FriendlyName can not be null.");
        }
        if (!data.containsKey("Namespace")) {
            throw new NullPointerException("Namespace can not be null.");
        }
    }

    protected Response updateOrganization(final String accountSid, final String sid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Modify:Organizations");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Organization organization = dao.getOrganization(new Sid(sid));
        if (organization == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secureLevelControl(accountsDao, accountSid, String.valueOf(organization.getAccountSid()));
            } catch (final AuthorizationException exception) {
                return status(UNAUTHORIZED).build();
            }
            dao.updateOrganization(update(organization, data));
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(organization);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(organization), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    private Organization update(final Organization organization, final MultivaluedMap<String, String> data) {
        Organization result = organization;
        if (data.containsKey("FriendlyName")) {
            result = result.setFriendlyName(data.getFirst("FriendlyName"));
        }
        if (data.containsKey("Namespace")) {
            result = result.setNamespace(data.getFirst("Namespace"));
        }
        return result;
    }

}
