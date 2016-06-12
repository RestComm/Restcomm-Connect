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
package org.mobicents.servlet.restcomm.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

import java.net.URI;
import java.util.List;

import static javax.ws.rs.core.MediaType.*;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.ClientList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.http.converter.ClientConverter;
import org.mobicents.servlet.restcomm.http.converter.ClientListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe
public abstract class ClientsEndpoint extends SecuredEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected ClientsDao dao;
    protected Gson gson;
    protected XStream xstream;
    protected AccountsDao accountsDao;

    public ClientsEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        dao = storage.getClientsDao();
        accountsDao = storage.getAccountsDao();
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        final ClientConverter converter = new ClientConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Client.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new ClientListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    private Client createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
        final Client.Builder builder = Client.builder();
        final Sid sid = Sid.generate(Sid.Type.CLIENT);
        builder.setSid(sid);
        builder.setApiVersion(getApiVersion(data));
        builder.setFriendlyName(getFriendlyName(data.getFirst("Login"), data));
        builder.setAccountSid(accountSid);
        builder.setLogin(data.getFirst("Login"));
        builder.setPassword(data.getFirst("Password"));
        builder.setStatus(getStatus(data));
        URI voiceUrl = getUrl("VoiceUrl", data);
        if (voiceUrl != null && voiceUrl.toString().equals("")) {
            voiceUrl=null;
        }
        builder.setVoiceUrl(voiceUrl);
        String method = getMethod("VoiceMethod", data);
        if (method == null || method.isEmpty() || method.equals("")) {
            method = "POST";
        }
        builder.setVoiceMethod(method);
        builder.setVoiceFallbackUrl(getUrl("VoiceFallbackUrl", data));
        builder.setVoiceFallbackMethod(getMethod("VoiceFallbackMethod", data));
        // skip null/empty VoiceApplicationSid's (i.e. leave null)
        if (data.containsKey("VoiceApplicationSid")) {
            if ( ! org.apache.commons.lang.StringUtils.isEmpty( data.getFirst("VoiceApplicationSid") ) )
                builder.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
        }
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
                .append("/Clients/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    protected Response getClient(final String accountSid, final String sid, final MediaType responseType) {
        Account operatedAccount = accountsDao.getAccount(accountSid);
        secure(operatedAccount, "RestComm:Read:Clients");
        final Client client = dao.getClient(new Sid(sid));
        if (client == null) {
            return status(NOT_FOUND).build();
        } else {
            secure(operatedAccount, client.getAccountSid(), SecuredType.SECURED_STANDARD);
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(client);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(client), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    protected Response getClients(final String accountSid, final MediaType responseType) {
        secure(accountsDao.getAccount(accountSid), "RestComm:Read:Clients");
        final List<Client> clients = dao.getClients(new Sid(accountSid));
        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new ClientList(clients));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(clients), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    private String getFriendlyName(final String login, final MultivaluedMap<String, String> data) {
        String friendlyName = login;
        if (data.containsKey("FriendlyName")) {
            friendlyName = data.getFirst("FriendlyName");
        }
        return friendlyName;
    }

    private int getStatus(final MultivaluedMap<String, String> data) {
        int status = Client.ENABLED;
        if (data.containsKey("Status")) {
            try {
                status = Integer.parseInt(data.getFirst("Status"));
            } catch (final NumberFormatException ignored) {
            }
        }
        return status;
    }

    public Response putClient(final String accountSid, final MultivaluedMap<String, String> data, final MediaType responseType) {
        secure(accountsDao.getAccount(accountSid), "RestComm:Create:Clients");
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }

        // Issue 109: https://bitbucket.org/telestax/telscale-restcomm/issue/109
        Client client = dao.getClient(data.getFirst("Login"));
        if (client == null) {
            client = createFrom(new Sid(accountSid), data);
            dao.addClient(client);
        } else if (!client.getAccountSid().toString().equals(accountSid)) {
            return status(CONFLICT)
                    .entity("A client with the same name was already created by another account. Please, choose a different name and try again.")
                    .build();
        }

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(client);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(client), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response updateClient(final String accountSid, final String sid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        Account operatedAccount = accountsDao.getAccount(accountSid);
        secure(operatedAccount, "RestComm:Modify:Clients");
        final Client client = dao.getClient(new Sid(sid));
        if (client == null) {
            return status(NOT_FOUND).build();
        } else {
            secure(operatedAccount, client.getAccountSid(), SecuredType.SECURED_STANDARD );
            dao.updateClient(update(client, data));
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(client);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(client), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
        if (!data.containsKey("Login")) {
            throw new NullPointerException("Login can not be null.");
        } else if (!data.containsKey("Password")) {
            throw new NullPointerException("Password can not be null.");
        }
    }

    private Client update(final Client client, final MultivaluedMap<String, String> data) {
        Client result = client;
        if (data.containsKey("FriendlyName")) {
            result = result.setFriendlyName(data.getFirst("FriendlyName"));
        }
        if (data.containsKey("Password")) {
            result = result.setPassword(data.getFirst("Password"));
        }
        if (data.containsKey("Status")) {
            result = result.setStatus(getStatus(data));
        }
        if (data.containsKey("VoiceUrl")) {
            URI uri = getUrl("VoiceUrl", data);
            result = result.setVoiceUrl(isEmpty(uri.toString()) ? null : uri);
        }
        if (data.containsKey("VoiceMethod")) {
            result = result.setVoiceMethod(getMethod("VoiceMethod", data));
        }
        if (data.containsKey("VoiceFallbackUrl")) {
            URI uri = getUrl("VoiceFallbackUrl", data);
            result = result.setVoiceFallbackUrl(isEmpty(uri.toString()) ? null :uri);
        }
        if (data.containsKey("VoiceFallbackMethod")) {
            result = result.setVoiceFallbackMethod(getMethod("VoiceFallbackMethod", data));
        }
        if (data.containsKey("VoiceApplicationSid")) {
            if (org.apache.commons.lang.StringUtils.isEmpty(data.getFirst("VoiceApplicationSid"))) {
                result = result.setVoiceApplicationSid(null);
            } else {
                result = result.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
            }
        }
        return result;
    }
}
