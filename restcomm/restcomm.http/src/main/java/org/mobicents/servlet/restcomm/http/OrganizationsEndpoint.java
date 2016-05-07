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
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.OrganizationsDao;
import org.mobicents.servlet.restcomm.entities.Account;
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

    protected Organization createFrom(final MultivaluedMap<String, String> data) {
        final Organization.Builder builder = Organization.builder();
        final Sid sid = Sid.generate(Sid.Type.ORGANIZATION);
        builder.setSid(sid);
        builder.setFriendlyName(data.getFirst("FriendlyName"));
        builder.setNamespace(data.getFirst("Namespace"));
        builder.setApiVersion(getApiVersion(data));
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Organizations/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    protected Response getOrganization(final String sid, final MediaType responseType) {
        Account account = accountsDao.getAccount(String.valueOf(SecurityUtils.getSubject().getPrincipal()));
        try {
            secure(account, "RestComm:Read:Organizations");
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
                secureLevelControlOrganizations(account, organization, false);
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

    protected Response getOrganizations(final MediaType responseType) {
        Account account = accountsDao.getAccount(String.valueOf(SecurityUtils.getSubject().getPrincipal()));
        try {
            secure(account, "RestComm:Read:Organizations");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        List<Organization> organizations = new ArrayList<Organization>();
        final Subject subject = SecurityUtils.getSubject();
        if (subject.hasRole("Administrator")) {
            organizations = dao.getAllOrganizations();
        } else {
            Organization organization = dao.getOrganization(account.getOrganizationSid());
            organizations.add(organization); // Always be only one Organization, but list is used to keep the response standard
        }
        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new OrganizationList(organizations));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(organizations), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response putOrganization(final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        Organization organization = dao.getOrganization(data.getFirst("Namespace"));
        if (organization == null) {
            organization = createFrom(data);
            dao.addOrganization(organization);
        } else {
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

    protected Response deleteOrganization(final String sid) {
        Account account = accountsDao.getAccount(String.valueOf(SecurityUtils.getSubject().getPrincipal()));
        try {
            secure(account, "RestComm:Delete:Organizations");
            Organization organization = dao.getOrganization(new Sid(sid));
            if (organization != null) {
                secureLevelControlOrganizations(account, organization, true);
            }
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        accountsDao.migrateToDefaultOrganization(new Sid(sid));
        dao.removeOrganization(new Sid(sid));
        return ok().build();
    }

    private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
        if (!data.containsKey("FriendlyName")) {
            throw new NullPointerException("FriendlyName can not be null.");
        }
        if (!data.containsKey("Namespace")) {
            throw new NullPointerException("Namespace can not be null.");
        }
    }

    protected Response updateOrganization(final String sid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        Account account = accountsDao.getAccount(String.valueOf(SecurityUtils.getSubject().getPrincipal()));
        try {
            secure(account, "RestComm:Modify:Organizations");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Organization organization = dao.getOrganization(new Sid(sid));
        if (organization == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secureLevelControlOrganizations(account, organization, true);
            } catch (final AuthorizationException exception) {
                return status(UNAUTHORIZED).build();
            }
            Organization updatedOrganization = update(organization, data);
            dao.updateOrganization(updatedOrganization);
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(updatedOrganization);
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

    protected boolean secureLevelControlOrganizations(Account account, Organization organization, boolean adminRoleRequired) {
        final Subject subject = SecurityUtils.getSubject();
        final boolean isRestCommAdmin = subject.hasRole("Administrator");
        final boolean isOrganizationAdmin = subject.hasRole("Organization Administrator");
        if (adminRoleRequired) {
            if (!(isRestCommAdmin || isOrganizationAdmin)) {
                throw new AuthorizationException();
            }
            if (isOrganizationAdmin) {
                return isAccountOfOrganization(account, organization);
            }
        } else if (!isRestCommAdmin) {
            return isAccountOfOrganization(account, organization);
        }
        return true;
    }

    private boolean isAccountOfOrganization(Account account, Organization organization) {
        String organizationSid = String.valueOf(organization.getSid());
        String accountSid = String.valueOf(account.getOrganizationSid());
        if (!organizationSid.equalsIgnoreCase(accountSid)) {
            throw new AuthorizationException();
        }
        return true;
    }

}
