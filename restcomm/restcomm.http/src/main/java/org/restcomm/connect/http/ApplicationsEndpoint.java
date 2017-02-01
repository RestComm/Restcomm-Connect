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

package org.restcomm.connect.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.http.converter.ApplicationConverter;
import org.restcomm.connect.http.converter.ApplicationListConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.ApplicationsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Application;
import org.restcomm.connect.dao.entities.ApplicationList;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.util.StringUtils;

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
        buffer.append("/").append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
                .append("/Applications/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        builder.setRcmlUrl(getUrl("RcmlUrl", data));
        if (data.containsKey("Kind")) {
            builder.setKind(Application.Kind.getValueOf(data.getFirst("Kind")));
        }
        return builder.build();
    }

    protected Response getApplication(final String accountSid, final String sid, final MediaType responseType) {
        Account account;
        secure(account = accountsDao.getAccount(accountSid), "RestComm:Read:Applications");
        Application application = null;
        if (Sid.pattern.matcher(sid).matches()) {
            application = dao.getApplication(new Sid(sid));
        } /*else {
            // disabled support for application retrieval based on FriendlyName. It makes no sense to have it if friendly-name based application uniqueness is no longer supported either.

            try {
                // Once not a valid sid, search using the parameter as name
                String name = URLDecoder.decode(String.valueOf(sid), "UTF-8");
                application = dao.getApplication(name);
            } catch (UnsupportedEncodingException e) {
                return status(BAD_REQUEST).entity(e.getMessage()).build();
            }
        }*/
        if (application == null) {
            return status(NOT_FOUND).build();
        } else {
            secure(account, application.getAccountSid(), SecuredType.SECURED_APP);
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
        Account account;
        account = accountsDao.getAccount(accountSid);
        secure(account, "RestComm:Read:Applications", SecuredType.SECURED_APP);
        final List<Application> applications = dao.getApplications(account.getSid());
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
        Account account;
        account = accountsDao.getAccount(accountSid);
        secure(account, "RestComm:Create:Applications", SecuredType.SECURED_APP);
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }

//        Application application = dao.getApplication(data.getFirst("FriendlyName"));
//        if (application == null) {
//            application = createFrom(new Sid(accountSid), data);
//            dao.addApplication(application);
//        } else if (!application.getAccountSid().toString().equals(account.getSid().toString())) {
//            return status(CONFLICT)
//                    .entity("A application with the same name was already created by another account. Please, choose a different name and try again.")
//                    .build();
//        }

        // application uniqueness now relies only on application SID. No checks on the FriendlyName will be done.
        Application application = createFrom(new Sid(accountSid), data);
        dao.addApplication(application);


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
        Account account;
        secure(account = accountsDao.getAccount(accountSid), "RestComm:Modify:Applications");
        final Application application = dao.getApplication(new Sid(sid));
        if (application == null) {
            return status(NOT_FOUND).build();
        } else {
            secure(account, application.getAccountSid(), SecuredType.SECURED_APP);
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

}
