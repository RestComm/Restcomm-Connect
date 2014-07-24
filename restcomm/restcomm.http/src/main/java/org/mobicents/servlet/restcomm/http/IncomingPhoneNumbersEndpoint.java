/*
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.thoughtworks.xstream.XStream;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumberList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.IncomingPhoneNumberConverter;
import org.mobicents.servlet.restcomm.http.converter.IncomingPhoneNumberListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com
 */
@NotThreadSafe
public abstract class IncomingPhoneNumbersEndpoint extends AbstractEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected IncomingPhoneNumbersDao dao;
    protected Gson gson;
    protected XStream xstream;
    protected AccountsDao accountsDao;
    protected Configuration voipInnovationsConfiguration;
    protected Configuration telestaxProxyConfiguration;
    protected Boolean telestaxProxyEnabled;
    protected String uri, username, password, endpoint;

    private String header;

    public IncomingPhoneNumbersEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        final Configuration runtime = configuration.subset("runtime-settings");
        super.init(runtime);
        dao = storage.getIncomingPhoneNumbersDao();
        accountsDao = storage.getAccountsDao();
        final IncomingPhoneNumberConverter converter = new IncomingPhoneNumberConverter(runtime);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(IncomingPhoneNumber.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new IncomingPhoneNumberListConverter(runtime));
        xstream.registerConverter(new RestCommResponseConverter(runtime));

        voipInnovationsConfiguration = configuration.subset("voip-innovations");
        telestaxProxyConfiguration = runtime.subset("telestax-proxy");
        telestaxProxyEnabled = telestaxProxyConfiguration.getBoolean("enabled", false);
        if (telestaxProxyEnabled) {
            uri = telestaxProxyConfiguration.getString("uri");
            username = telestaxProxyConfiguration.getString("login");
            password = telestaxProxyConfiguration.getString("password");
            endpoint = telestaxProxyConfiguration.getString("endpoint");
        } else {
            uri = voipInnovationsConfiguration.getString("uri");
            username = voipInnovationsConfiguration.getString("login");
            password = voipInnovationsConfiguration.getString("password");
            endpoint = voipInnovationsConfiguration.getString("endpoint");
        }
        this.header = header(username, password);
    }

    private String header(final String login, final String password) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("<header><sender>");
        buffer.append("<login>").append(login).append("</login>");
        buffer.append("<password>").append(password).append("</password>");
        buffer.append("</sender></header>");
        return buffer.toString();
    }

    protected boolean assignDid(final String did) {
        if (did != null && !did.isEmpty()) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("<request id=\"\">");
            buffer.append(header);
            buffer.append("<body>");
            buffer.append("<requesttype>").append("assignDID").append("</requesttype>");
            buffer.append("<item>");
            buffer.append("<did>").append(did).append("</did>");
            buffer.append("<endpointgroup>").append(endpoint).append("</endpointgroup>");
            buffer.append("</item>");
            buffer.append("</body>");
            buffer.append("</request>");
            final String body = buffer.toString();
            final HttpPost post = new HttpPost(uri);
            try {
                List<NameValuePair> parameters = new ArrayList<NameValuePair>();
                parameters.add(new BasicNameValuePair("apidata", body));
                post.setEntity(new UrlEncodedFormEntity(parameters));
                final DefaultHttpClient client = new DefaultHttpClient();
                final HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final String content = StringUtils.toString(response.getEntity().getContent());
                    if (content.contains("<statuscode>100</statuscode>")) {
                        return true;
                    }
                }
            } catch (final Exception ignored) {
            }
        }
        return false;
    }

    protected boolean isValidDid(final String did) {
        if (did != null && !did.isEmpty()) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("<request id=\"\">");
            buffer.append(header);
            buffer.append("<body>");
            buffer.append("<requesttype>").append("queryDID").append("</requesttype>");
            buffer.append("<item>");
            buffer.append("<did>").append(did).append("</did>");
            buffer.append("</item>");
            buffer.append("</body>");
            buffer.append("</request>");
            final String body = buffer.toString();
            final HttpPost post = new HttpPost(uri);
            try {
                List<NameValuePair> parameters = new ArrayList<NameValuePair>();
                parameters.add(new BasicNameValuePair("apidata", body));
                post.setEntity(new UrlEncodedFormEntity(parameters));
                final DefaultHttpClient client = new DefaultHttpClient();
                final HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    final String content = StringUtils.toString(response.getEntity().getContent());
                    if (content.contains("<statusCode>100</statusCode>")) {
                        return true;
                    }
                }
            } catch (final Exception ignored) {
            }
        }
        return false;
    }

    private IncomingPhoneNumber createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        final Sid sid = Sid.generate(Sid.Type.PHONE_NUMBER);
        builder.setSid(sid);
        builder.setAccountSid(accountSid);
        String phoneNumber = data.getFirst("PhoneNumber");
        builder.setPhoneNumber(phoneNumber);
        builder.setFriendlyName(getFriendlyName(phoneNumber, data));
        final String apiVersion = getApiVersion(data);
        builder.setApiVersion(apiVersion);
        builder.setVoiceUrl(getUrl("VoiceUrl", data));
        builder.setVoiceMethod(getMethod("VoiceMethod", data));
        builder.setVoiceFallbackUrl(getUrl("VoiceFallbackUrl", data));
        builder.setVoiceFallbackMethod(getMethod("VoiceFallbackMethod", data));
        builder.setStatusCallback(getUrl("StatusCallback", data));
        builder.setStatusCallbackMethod(getMethod("StatusCallbackMethod", data));
        builder.setHasVoiceCallerIdLookup(getHasVoiceCallerIdLookup(data));
        builder.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
        builder.setSmsUrl(getUrl("SmsUrl", data));
        builder.setSmsMethod(getMethod("SmsMethod", data));
        builder.setSmsFallbackUrl(getUrl("SmsFallbackUrl", data));
        builder.setSmsFallbackMethod(getMethod("SmsFallbackMethod", data));
        builder.setSmsApplicationSid(getSid("SmsApplicationSid", data));
        final Configuration configuration = this.configuration.subset("runtime-settings");
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(apiVersion).append("/Accounts/").append(accountSid.toString())
                .append("/IncomingPhoneNumbers/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private String e164(final String number) {
        final PhoneNumberUtil numbersUtil = PhoneNumberUtil.getInstance();
        try {
            final PhoneNumber result = numbersUtil.parse(number, "US");
            return numbersUtil.format(result, PhoneNumberFormat.E164);
        } catch (final NumberParseException ignored) {
            return number;
        }
    }

    private String getFriendlyName(final String phoneNumber, final MultivaluedMap<String, String> data) {
        String friendlyName = phoneNumber;
        if (data.containsKey("FriendlyName")) {
            friendlyName = data.getFirst("FriendlyName");
        }
        return friendlyName;
    }

    protected Response getIncomingPhoneNumber(final String accountSid, final String sid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        if (incomingPhoneNumber == null) {
            return status(NOT_FOUND).build();
        } else {
            if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
            } else if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else {
                return null;
            }
        }
    }

    protected Response getIncomingPhoneNumbers(final String accountSid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final List<IncomingPhoneNumber> incomingPhoneNumbers = dao.getIncomingPhoneNumbers(new Sid(accountSid));
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(incomingPhoneNumbers), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new IncomingPhoneNumberList(incomingPhoneNumbers));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    protected Response putIncomingPhoneNumber(final String accountSid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        String number = data.getFirst("PhoneNumber");
        IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(e164(number));
        if (incomingPhoneNumber == null) {
            incomingPhoneNumber = createFrom(new Sid(accountSid), data);
            dao.addIncomingPhoneNumber(incomingPhoneNumber);
            number = number.substring(2);
            // Provision the number from VoIP Innovations if they own it.
            if (isValidDid(number)) {
                assignDid(number);
            }
        }
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    public Response updateIncomingPhoneNumber(final String accountSid, final String sid,
            final MultivaluedMap<String, String> data, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Modify:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        dao.updateIncomingPhoneNumber(update(incomingPhoneNumber, data));
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
        if (!data.containsKey("PhoneNumber")) {
            throw new NullPointerException("Phone number can not be null.");
        }
    }

    private IncomingPhoneNumber update(final IncomingPhoneNumber incomingPhoneNumber, final MultivaluedMap<String, String> data) {
        IncomingPhoneNumber result = incomingPhoneNumber;
        if (data.containsKey("ApiVersion")) {
            result = result.setApiVersion(getApiVersion(data));
        }
        if (data.containsKey("FriendlyName")) {
            result = result.setFriendlyName(data.getFirst("FriendlyName"));
        }
        if (data.containsKey("VoiceUrl")) {
            result = result.setVoiceUrl(getUrl("VoiceUrl", data));
        }
        if (data.containsKey("VoiceMethod")) {
            result = result.setVoiceMethod(getMethod("VoiceMethod", data));
        }
        if (data.containsKey("VoiceFallbackUrl")) {
            result = result.setVoiceFallbackUrl(getUrl("VoiceFallbackUrl", data));
        }
        if (data.containsKey("VoiceFallbackMethod")) {
            result = result.setVoiceFallbackMethod(getMethod("VoiceFallbackMethod", data));
        }
        if (data.containsKey("StatusCallback")) {
            result = result.setStatusCallback(getUrl("StatusCallback", data));
        }
        if (data.containsKey("StatusCallbackMethod")) {
            result = result.setStatusCallbackMethod(getMethod("StatusCallbackMethod", data));
        }
        if (data.containsKey("VoiceCallerIdLookup")) {
            result = result.setVoiceCallerIdLookup(getHasVoiceCallerIdLookup(data));
        }
        if (data.containsKey("VoiceApplicationSid")) {
            result = result.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
        }
        if (data.containsKey("SmsUrl")) {
            result = result.setSmsUrl(getUrl("SmsUrl", data));
        }
        if (data.containsKey("SmsMethod")) {
            result = result.setSmsMethod(getMethod("SmsMethod", data));
        }
        if (data.containsKey("SmsFallbackUrl")) {
            result = result.setSmsFallbackUrl(getUrl("SmsFallbackUrl", data));
        }
        if (data.containsKey("SmsFallbackMethod")) {
            result = result.setSmsFallbackMethod(getMethod("SmsFallbackMethod", data));
        }
        if (data.containsKey("SmsApplicationSid")) {
            result = result.setSmsApplicationSid(getSid("SmsApplicationSid", data));
        }

        if (data.containsKey("VoiceCapable")) {
            result = result.setVoiceCapable(Boolean.parseBoolean(data.getFirst("VoiceCapable")));
        } else {
            result = result.setVoiceCapable(Boolean.TRUE);
        }

        if (data.containsKey("SmsCapable")) {
            result = result.setSmsCapable(Boolean.parseBoolean(data.getFirst("SmsCapable")));
        } else {
            result = result.setSmsCapable(Boolean.FALSE);
        }

        if (data.containsKey("MmsCapable")) {
            result = result.setMmsCapable(Boolean.parseBoolean(data.getFirst("MmsCapable")));
        } else {
            result = result.setMmsCapable(Boolean.FALSE);
        }

        if (data.containsKey("FaxCapable")) {
            result = result.setFaxCapable(Boolean.parseBoolean(data.getFirst("FaxCapable")));
        } else {
            result = result.setFaxCapable(Boolean.FALSE);
        }

        return result;
    }
}
