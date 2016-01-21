/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
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
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.ApplicationList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.ApplicationConverter;
import org.mobicents.servlet.restcomm.http.converter.ApplicationListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author guilherme.jansen@telestax.com
 */
@NotThreadSafe
public class ApplicationsEndpoint extends SecuredEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected ApplicationsDao dao;
    protected Gson gson;
    protected XStream xstream;
    protected AccountsDao accountsDao;

    public ApplicationsEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        dao = storage.getApplicationsDao();
        accountsDao = storage.getAccountsDao();
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        final ApplicationConverter converter = new ApplicationConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Application.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new ApplicationListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    private Application createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
        final Application.Builder builder = Application.builder();
        final Sid sid = Sid.generate(Sid.Type.APPLICATION);
        builder.setSid(sid);
        builder.setFriendlyName(data.getFirst("FriendlyName"));
        builder.setAccountSid(accountSid);
        builder.setApiVersion(getApiVersion(data));
        builder.setHasVoiceCallerIdLookup(new Boolean(data.getFirst("VoiceCallerIdLookup")));
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
                .append("/Applications/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        builder.setRcmlUrl(getUrl("RcmlUrl", data));
        if (data.containsKey("Kind")) {
            builder.setKind(Application.Kind.getValueOf(data.getFirst("Kind")));
        }
        return builder.build();
    }

    protected Response getApplication(final String accountSid, final String sid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:Applications");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        Application application = null;
        if (Sid.pattern.matcher(sid).matches()) {
            application = dao.getApplication(new Sid(sid));
        } else {
            try {
                // Once not a valid sid, search using the parameter as name
                String name = URLDecoder.decode(String.valueOf(sid), "UTF-8");
                application = dao.getApplication(name);
            } catch (UnsupportedEncodingException e) {
                return status(BAD_REQUEST).entity(e.getMessage()).build();
            }
        }
        if (application == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secureLevelControlApplications(accountSid, application);
            } catch (AuthorizationException e) {
                return status(UNAUTHORIZED).build();
            }
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(application);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(application), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    protected Response getApplications(final String accountSid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:Applications");
            secureLevelControlApplications(accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final List<Application> applications = dao.getApplications(new Sid(accountSid));
        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new ApplicationList(applications));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(applications), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    public Response putApplication(final String accountSid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:Applications");
            secureLevelControlApplications(accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }

        Application application = dao.getApplication(data.getFirst("FriendlyName"));
        if (application == null) {
            application = createFrom(new Sid(accountSid), data);
            dao.addApplication(application);
        } else if (!application.getAccountSid().toString().equals(accountSid)) {
            return status(CONFLICT)
                    .entity("A application with the same name was already created by another account. Please, choose a different name and try again.")
                    .build();
        }

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(application);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(application), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
        if (!data.containsKey("FriendlyName")) {
            throw new NullPointerException("Friendly name can not be null.");
        }
    }

    protected Response updateApplication(final String accountSid, final String sid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Modify:Applications");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Application application = dao.getApplication(new Sid(sid));
        if (application == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secureLevelControlApplications(accountSid, application);
            } catch (AuthorizationException e) {
                return status(UNAUTHORIZED).build();
            }
            final Application applicationUpdate = update(application, data);
            dao.updateApplication(applicationUpdate);
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(applicationUpdate);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(applicationUpdate), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    private Application update(final Application application, final MultivaluedMap<String, String> data) {
        Application result = application;
        if (data.containsKey("FriendlyName")) {
            result = result.setFriendlyName(data.getFirst("FriendlyName"));
        }
        if (data.containsKey("VoiceCallerIdLookup")) {
            result = result.setVoiceCallerIdLookup(new Boolean(data.getFirst("VoiceCallerIdLookup")));
        }
        if (data.containsKey("RcmlUrl")) {
            result = result.setRcmlUrl(getUrl("RcmlUrl", data));
        }
        if (data.containsKey("Kind")) {
            result = result.setKind(Application.Kind.getValueOf(data.getFirst("Kind")));
        }
        return result;
    }

    protected boolean secureLevelControlApplications(String accountSid, Application app) {
        String sidPrincipal = String.valueOf(SecurityUtils.getSubject().getPrincipal());
        if (!sidPrincipal.equals(String.valueOf(accountSid))) {
            throw new AuthorizationException();
        } else if (app != null && app.getAccountSid() != null && !sidPrincipal.equals(String.valueOf(app.getAccountSid()))) {
            throw new AuthorizationException();
        }
        return true;
    }
}
