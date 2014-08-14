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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URI;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipURI;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
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
import org.mobicents.servlet.restcomm.loader.ObjectFactory;
import org.mobicents.servlet.restcomm.loader.ObjectInstantiationException;
import org.mobicents.servlet.restcomm.provisioning.number.api.ContainerConfiguration;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberParameters;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberType;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com
 * @author jean.deruelle@telestax.com
 */
@NotThreadSafe
public abstract class IncomingPhoneNumbersEndpoint extends AbstractEndpoint {
    @Context
    protected ServletContext context;
    protected PhoneNumberProvisioningManager phoneNumberProvisioningManager;
    private IncomingPhoneNumbersDao dao;
    protected AccountsDao accountsDao;
    private XStream xstream;
    protected Gson gson;

    public IncomingPhoneNumbersEndpoint() {
        super();
    }

    @PostConstruct
    public void init() throws ObjectInstantiationException {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        super.init(configuration.subset("runtime-settings"));
        dao = storage.getIncomingPhoneNumbersDao();
        accountsDao = storage.getAccountsDao();

        phoneNumberProvisioningManager = (PhoneNumberProvisioningManager) context.getAttribute("PhoneNumberProvisioningManager");
        if(phoneNumberProvisioningManager == null) {
            final String phoneNumberProvisioningManagerClass = configuration.getString("phone-number-provisioning[@class]");
            Configuration phoneNumberProvisioningConfiguration = configuration.subset("phone-number-provisioning");
            Configuration telestaxProxyConfiguration = configuration.subset("runtime-settings").subset("telestax-proxy");

            phoneNumberProvisioningManager = (PhoneNumberProvisioningManager) new ObjectFactory(getClass().getClassLoader())
                    .getObjectInstance(phoneNumberProvisioningManagerClass);
            ContainerConfiguration containerConfiguration = new ContainerConfiguration(getOutboundInterfaces());
            phoneNumberProvisioningManager.init(phoneNumberProvisioningConfiguration, telestaxProxyConfiguration, containerConfiguration);
            context.setAttribute("phoneNumberProvisioningManager", phoneNumberProvisioningManager);
        }

        final IncomingPhoneNumberConverter converter = new IncomingPhoneNumberConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        builder.registerTypeAdapter(IncomingPhoneNumber.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new IncomingPhoneNumberListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
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

    protected Response getIncomingPhoneNumbers(final String accountSid, PhoneNumberType phoneNumberType, final MediaType responseType) {
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
            PhoneNumberType phoneNumberType, final MediaType responseType) {
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
            number = number.substring(2);
            PhoneNumberParameters phoneNumberParameters = getPhoneNumberParameters(data);
            phoneNumberParameters.setPhoneNumberType(phoneNumberType);
            boolean isDidAssigned = phoneNumberProvisioningManager.buyNumber(number, phoneNumberParameters);
            if(isDidAssigned) {
                dao.addIncomingPhoneNumber(incomingPhoneNumber);
                if (APPLICATION_JSON_TYPE == responseType) {
                    return ok(gson.toJson(incomingPhoneNumber), APPLICATION_JSON).build();
                } else if (APPLICATION_XML_TYPE == responseType) {
                    final RestCommResponse response = new RestCommResponse(incomingPhoneNumber);
                    return ok(xstream.toXML(response), APPLICATION_XML).build();
                }
            }
        }
        return status(BAD_REQUEST).entity("21452").build();
    }

    public Response updateIncomingPhoneNumber(final String accountSid, final String sid,
            final MultivaluedMap<String, String> data, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Modify:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        boolean updated = phoneNumberProvisioningManager.updateNumber(sid, getPhoneNumberParameters(data));
        if(updated) {
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
        return status(BAD_REQUEST).entity("21452").build();
    }

    private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
        if (!data.containsKey("PhoneNumber") && !data.containsKey("AreaCode")) {
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

    public Response deleteIncomingPhoneNumber(final String accountSid, final String sid) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Delete:IncomingPhoneNumbers");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        final String number = incomingPhoneNumber.getPhoneNumber();
        String numberToRemoveFromVi = number;
        if(numberToRemoveFromVi.startsWith("+1")){
            numberToRemoveFromVi = numberToRemoveFromVi.replaceFirst("\\+1", "");
        }
        releaseDid(numberToRemoveFromVi);
        dao.removeIncomingPhoneNumber(new Sid(sid));
        return noContent().build();
    }

    protected boolean releaseDid(final String did) {
        return phoneNumberProvisioningManager.cancelNumber(did);
    }

    private PhoneNumberParameters getPhoneNumberParameters(MultivaluedMap<String, String> data) {
        PhoneNumberParameters phoneNumberParameters = new PhoneNumberParameters();
//        phoneNumberParameters.setVoiceUrl(data.getFirst("VoiceUrl"));
//        phoneNumberParameters.setVoiceMethod(data.getFirst("VoiceMethod"));
//        phoneNumberParameters.setSmsUrl(data.getFirst("SmsUrl"));
//        phoneNumberParameters.setSmsMethod(data.getFirst("VoiceUrl"));
        return phoneNumberParameters;
    }

    @SuppressWarnings("unchecked")
    private List<SipURI> getOutboundInterfaces() {
        final List<SipURI> uris = (List<SipURI>) context.getAttribute(SipServlet.OUTBOUND_INTERFACES);
        return uris;
    }
}
