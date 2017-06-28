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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.loader.ObjectInstantiationException;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.IncomingPhoneNumbersDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.IncomingPhoneNumberFilter;
import org.restcomm.connect.dao.entities.IncomingPhoneNumberList;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.extension.api.ApiRequest;
import org.restcomm.connect.http.converter.AvailableCountriesConverter;
import org.restcomm.connect.http.converter.AvailableCountriesList;
import org.restcomm.connect.http.converter.IncomingPhoneNumberConverter;
import org.restcomm.connect.http.converter.IncomingPhoneNumberListConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.provisioning.number.api.PhoneNumberParameters;
import org.restcomm.connect.provisioning.number.api.PhoneNumberProvisioningManager;
import org.restcomm.connect.provisioning.number.api.PhoneNumberProvisioningManagerProvider;
import org.restcomm.connect.provisioning.number.api.PhoneNumberType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.NumberParseException.ErrorType;
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
public abstract class IncomingPhoneNumbersEndpoint extends SecuredEndpoint {
    @Context
    protected ServletContext context;
    protected PhoneNumberProvisioningManager phoneNumberProvisioningManager;
    protected IncomingPhoneNumberListConverter listConverter;
    PhoneNumberParameters phoneNumberParameters;
    private IncomingPhoneNumbersDao dao;
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

        /*
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
        */
        // get manager from context or create it if it does not exist
        phoneNumberProvisioningManager = new PhoneNumberProvisioningManagerProvider(configuration, context).get();

        Configuration callbackUrlsConfiguration = configuration.subset("phone-number-provisioning").subset("callback-urls");
        phoneNumberParameters = new PhoneNumberParameters(
                callbackUrlsConfiguration.getString("voice[@url]"),
                callbackUrlsConfiguration.getString("voice[@method]"), false,
                callbackUrlsConfiguration.getString("sms[@url]"),
                callbackUrlsConfiguration.getString("sms[@method]"),
                callbackUrlsConfiguration.getString("fax[@url]"),
                callbackUrlsConfiguration.getString("fax[@method]"),
                callbackUrlsConfiguration.getString("ussd[@url]"),
                callbackUrlsConfiguration.getString("ussd[@method]"));

        final IncomingPhoneNumberConverter converter = new IncomingPhoneNumberConverter(configuration);
        listConverter = new IncomingPhoneNumberListConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        builder.registerTypeAdapter(IncomingPhoneNumber.class, converter);
        builder.registerTypeAdapter(IncomingPhoneNumberList.class, listConverter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(listConverter);
        xstream.registerConverter(new AvailableCountriesConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));

    }

    private IncomingPhoneNumber createFrom(final Sid accountSid, final MultivaluedMap<String, String> data) {
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        final Sid sid = Sid.generate(Sid.Type.PHONE_NUMBER);
        builder.setSid(sid);
        builder.setAccountSid(accountSid);
        String phoneNumber = data.getFirst("PhoneNumber");
        String cost = data.getFirst("Cost");
        builder.setPhoneNumber(phoneNumber);
        builder.setFriendlyName(getFriendlyName(phoneNumber, data));
        if (data.containsKey("VoiceCapable")) {
            builder.setVoiceCapable(Boolean.parseBoolean(data.getFirst("VoiceCapable")));
        }
        if (data.containsKey("SmsCapable")) {
            builder.setSmsCapable(Boolean.parseBoolean(data.getFirst("SmsCapable")));
        }
        if (data.containsKey("MmsCapable")) {
            builder.setMmsCapable(Boolean.parseBoolean(data.getFirst("MmsCapable")));
        }
        if (data.containsKey("FaxCapable")) {
            builder.setFaxCapable(Boolean.parseBoolean(data.getFirst("FaxCapable")));
        }
        if (data.containsKey("isSIP")) {
            builder.setPureSip(Boolean.parseBoolean(data.getFirst("isSIP")));
        } else {
            builder.setPureSip(false);
        }
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

        builder.setUssdUrl(getUrl("UssdUrl", data));
        builder.setUssdMethod(getMethod("UssdMethod", data));
        builder.setUssdFallbackUrl(getUrl("UssdFallbackUrl", data));
        builder.setUssdFallbackMethod(getMethod("UssdFallbackMethod",data));
        builder.setUssdApplicationSid(getSid("UssdApplicationSid",data));

        builder.setReferUrl(getUrl("ReferUrl", data));
        builder.setReferMethod(getMethod("ReferMethod", data));
        builder.setReferApplicationSid(getSid("ReferApplicationSid",data));

        final Configuration configuration = this.configuration.subset("runtime-settings");
        final StringBuilder buffer = new StringBuilder();
        buffer.append("/").append(apiVersion).append("/Accounts/").append(accountSid.toString())
        .append("/IncomingPhoneNumbers/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private String e164(String number) throws NumberParseException {
        final PhoneNumberUtil numbersUtil = PhoneNumberUtil.getInstance();
        if(!number.startsWith("+")) {
            number = "+" + number;
        }
        final PhoneNumber result = numbersUtil.parse(number, "US");
        if (numbersUtil.isValidNumber(result)) {
            return numbersUtil.format(result, PhoneNumberFormat.E164);
        } else {
            throw new NumberParseException(ErrorType.NOT_A_NUMBER, "This is not a valid number");
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
        Account operatedAccount = accountsDao.getAccount(accountSid);
        secure(operatedAccount, "RestComm:Read:IncomingPhoneNumbers");
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        if (incomingPhoneNumber == null) {
            return status(NOT_FOUND).build();
        } else {
            // if the account that the resource belongs to does not existb while the resource does, we're having BAD parameters
            if (operatedAccount == null) {
                return status(BAD_REQUEST).build();
            }
            secure(operatedAccount, incomingPhoneNumber.getAccountSid(), SecuredType.SECURED_STANDARD);
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

    protected Response getAvailableCountries(final String accountSid, final MediaType responseType) {
        secure(accountsDao.getAccount(accountSid), "RestComm:Read:IncomingPhoneNumbers");
        List<String> countries = phoneNumberProvisioningManager.getAvailableCountries();
        if (countries == null) {
            countries = new ArrayList<String>();
            countries.add("US");
        }
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(countries), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new AvailableCountriesList(countries));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    protected Response getIncomingPhoneNumbers(final String accountSid, final PhoneNumberType phoneNumberType, UriInfo info,
            final MediaType responseType) {

        secure(accountsDao.getAccount(accountSid), "RestComm:Read:IncomingPhoneNumbers");

        String phoneNumberFilter = info.getQueryParameters().getFirst("PhoneNumber");
        String friendlyNameFilter = info.getQueryParameters().getFirst("FriendlyName");
        String page = info.getQueryParameters().getFirst("Page");
        String reverse = info.getQueryParameters().getFirst("Reverse");
        String pageSize = info.getQueryParameters().getFirst("PageSize");
        String sortBy = info.getQueryParameters().getFirst("SortBy");

        pageSize = (pageSize == null) ? "50" : pageSize;
        page = (page == null) ? "0" : page;
        reverse = (reverse != null && "true".equalsIgnoreCase(reverse)) ? "DESC" : "ASC";
        sortBy = (sortBy != null) ? sortBy : "phone_number";

        int limit = Integer.parseInt(pageSize);
        int pageAsInt = Integer.parseInt(page);
        int offset = (page == "0") ? 0 : (((pageAsInt - 1) * limit) + limit);
        IncomingPhoneNumberFilter incomingPhoneNumberFilter = new IncomingPhoneNumberFilter(accountSid, friendlyNameFilter,
                phoneNumberFilter);
        final int total = dao.getTotalIncomingPhoneNumbers(incomingPhoneNumberFilter);

        if (pageAsInt > (total / limit)) {
            return status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
        }

        incomingPhoneNumberFilter = new IncomingPhoneNumberFilter(accountSid, friendlyNameFilter, phoneNumberFilter, sortBy,
                reverse, limit, offset);

        final List<IncomingPhoneNumber> incomingPhoneNumbers = dao.getIncomingPhoneNumbersByFilter(incomingPhoneNumberFilter);

        listConverter.setCount(total);
        listConverter.setPage(pageAsInt);
        listConverter.setPageSize(limit);
        listConverter.setPathUri("/" + getApiVersion(null) + "/" + info.getPath());

        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(new IncomingPhoneNumberList(incomingPhoneNumbers)), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new IncomingPhoneNumberList(incomingPhoneNumbers));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    protected Response putIncomingPhoneNumber(final String accountSid, final MultivaluedMap<String, String> data,
            PhoneNumberType phoneNumberType, final MediaType responseType) {
        secure(accountsDao.getAccount(accountSid), "RestComm:Create:IncomingPhoneNumbers");
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        String number = data.getFirst("PhoneNumber");
        String isSIP = data.getFirst("isSIP");
        // cater to SIP numbers
        if(isSIP == null) {
            try {
                number = e164(number);
            } catch (NumberParseException e) {}
        }
        IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(number);
        if (incomingPhoneNumber == null) {
            incomingPhoneNumber = createFrom(new Sid(accountSid), data);
            phoneNumberParameters.setPhoneNumberType(phoneNumberType);

            org.restcomm.connect.provisioning.number.api.PhoneNumber phoneNumber = convertIncomingPhoneNumbertoPhoneNumber(incomingPhoneNumber);
            boolean hasSuceeded = false;
            if(phoneNumberProvisioningManager != null && isSIP == null) {
                ApiRequest apiRequest = new ApiRequest(accountSid, data, ApiRequest.Type.INCOMINGPHONENUMBER);
                //Before proceed to buy the DID, check with the extensions if the purchase is allowed or not
                if (executePreApiAction(apiRequest)) {
                    hasSuceeded = phoneNumberProvisioningManager.buyNumber(phoneNumber, phoneNumberParameters);
                } else {
                    //Extensions didn't allowed this API action
                    hasSuceeded = false;
                    if (logger.isInfoEnabled()) {
                        logger.info("DID purchase is now allowed for this account");
                    }
                }
                executePostApiAction(apiRequest);
                //If Extension blocked the request, return the proper error response
                if (!hasSuceeded) {
                    String msg = "DID purchase is now allowed for this account";
                    String error = "DID_QUOTA_EXCEEDED";
                    return status(FORBIDDEN).entity(buildErrorResponseBody(msg, error, responseType)).build();
                }
            } else if (isSIP != null) {
                hasSuceeded = true;
            }
            if(hasSuceeded) {
                if(phoneNumber.getFriendlyName() != null) {
                    incomingPhoneNumber.setFriendlyName(phoneNumber.getFriendlyName());
                }
                if(phoneNumber.getPhoneNumber() != null) {
                    incomingPhoneNumber.setPhoneNumber(phoneNumber.getPhoneNumber());
                }
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
        Account operatedAccount = accountsDao.getAccount(accountSid);
        secure(operatedAccount, "RestComm:Modify:IncomingPhoneNumbers");
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        secure(operatedAccount, incomingPhoneNumber.getAccountSid(), SecuredType.SECURED_STANDARD );
        boolean updated = true;
        if(phoneNumberProvisioningManager != null && (incomingPhoneNumber.isPureSip() == null || !incomingPhoneNumber.isPureSip())) {
            updated = phoneNumberProvisioningManager.updateNumber(convertIncomingPhoneNumbertoPhoneNumber(incomingPhoneNumber), phoneNumberParameters);
        }
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
        if (data.containsKey("ApiVersion")) {
            incomingPhoneNumber.setApiVersion(getApiVersion(data));
        }
        if (data.containsKey("FriendlyName")) {
            incomingPhoneNumber.setFriendlyName(data.getFirst("FriendlyName"));
        }
        if (data.containsKey("VoiceUrl")) {
            // for all values that qualify as 'empty' populate property with null
            URI uri = getUrl("VoiceUrl", data);
            incomingPhoneNumber.setVoiceUrl(isEmpty(uri.toString()) ? null : uri);
        }
        if (data.containsKey("VoiceMethod")) {
            incomingPhoneNumber.setVoiceMethod(getMethod("VoiceMethod", data));
        }
        if (data.containsKey("VoiceFallbackUrl")) {
            URI uri = getUrl("VoiceFallbackUrl", data);
            incomingPhoneNumber.setVoiceFallbackUrl( isEmpty(uri.toString()) ? null : uri );
        }
        if (data.containsKey("VoiceFallbackMethod")) {
            incomingPhoneNumber.setVoiceFallbackMethod( getMethod("VoiceFallbackMethod", data) );
        }
        if (data.containsKey("StatusCallback")) {
            URI uri = getUrl("StatusCallback", data);
            incomingPhoneNumber.setStatusCallback( isEmpty(uri.toString()) ? null : uri );
        }
        if (data.containsKey("StatusCallbackMethod")) {
            incomingPhoneNumber.setStatusCallbackMethod(getMethod("StatusCallbackMethod", data));
        }
        if (data.containsKey("VoiceCallerIdLookup")) {
            incomingPhoneNumber.setHasVoiceCallerIdLookup(getHasVoiceCallerIdLookup(data));
        }
        if (data.containsKey("VoiceApplicationSid")) {
            if ( org.apache.commons.lang.StringUtils.isEmpty( data.getFirst("VoiceApplicationSid") ) )
                incomingPhoneNumber.setVoiceApplicationSid(null);
            else
                incomingPhoneNumber.setVoiceApplicationSid(getSid("VoiceApplicationSid", data));
        }
        if (data.containsKey("SmsUrl")) {
            URI uri = getUrl("SmsUrl", data);
            incomingPhoneNumber.setSmsUrl( isEmpty(uri.toString()) ? null : uri);
        }
        if (data.containsKey("SmsMethod")) {
            incomingPhoneNumber.setSmsMethod(getMethod("SmsMethod", data));
        }
        if (data.containsKey("SmsFallbackUrl")) {
            URI uri = getUrl("SmsFallbackUrl", data);
            incomingPhoneNumber.setSmsFallbackUrl( isEmpty(uri.toString()) ? null : uri );
        }
        if (data.containsKey("SmsFallbackMethod")) {
            incomingPhoneNumber.setSmsFallbackMethod(getMethod("SmsFallbackMethod", data));
        }
        if (data.containsKey("SmsApplicationSid")) {
            if ( org.apache.commons.lang.StringUtils.isEmpty( data.getFirst("SmsApplicationSid") ) )
                incomingPhoneNumber.setSmsApplicationSid(null);
            else
                incomingPhoneNumber.setSmsApplicationSid(getSid("SmsApplicationSid", data));

        }

        if (data.containsKey("ReferUrl")) {
            URI uri = getUrl("ReferUrl", data);
            incomingPhoneNumber.setReferUrl( isEmpty(uri.toString()) ? null : uri );
        }

        if (data.containsKey("ReferMethod")) {
            incomingPhoneNumber.setReferMethod(getMethod("ReferMethod", data));
        }

        if (data.containsKey("ReferApplicationSid")) {
            if ( org.apache.commons.lang.StringUtils.isEmpty( data.getFirst("ReferApplicationSid") ) )
                incomingPhoneNumber.setReferApplicationSid(null);
            else
                incomingPhoneNumber.setReferApplicationSid(getSid("ReferApplicationSid", data));
        }

        if (data.containsKey("VoiceCapable")) {
            incomingPhoneNumber.setVoiceCapable(Boolean.parseBoolean(data.getFirst("VoiceCapable")));
        }

        if (data.containsKey("VoiceCapable")) {
            incomingPhoneNumber.setVoiceCapable(Boolean.parseBoolean(data.getFirst("VoiceCapable")));
        }

        if (data.containsKey("SmsCapable")) {
            incomingPhoneNumber.setSmsCapable(Boolean.parseBoolean(data.getFirst("SmsCapable")));
        }

        if (data.containsKey("MmsCapable")) {
            incomingPhoneNumber.setMmsCapable(Boolean.parseBoolean(data.getFirst("MmsCapable")));
        }

        if (data.containsKey("FaxCapable")) {
            incomingPhoneNumber.setFaxCapable(Boolean.parseBoolean(data.getFirst("FaxCapable")));
        }

        if (data.containsKey("UssdUrl")) {
            URI uri = getUrl("UssdUrl", data);
            incomingPhoneNumber.setUssdUrl(isEmpty(uri.toString()) ? null : uri);
        }

        if (data.containsKey("UssdMethod")) {
            incomingPhoneNumber.setUssdMethod(getMethod("UssdMethod", data));
        }

        if (data.containsKey("UssdFallbackUrl")) {
            URI uri = getUrl("UssdFallbackUrl", data);
            incomingPhoneNumber.setUssdFallbackUrl(isEmpty(uri.toString()) ? null : uri);
        }

        if (data.containsKey("UssdFallbackMethod")) {
            incomingPhoneNumber.setUssdFallbackMethod(getMethod("UssdFallbackMethod", data));
        }

        if (data.containsKey("UssdApplicationSid")) {
            if (org.apache.commons.lang.StringUtils.isEmpty(data.getFirst("UssdApplicationSid")))
                incomingPhoneNumber.setUssdApplicationSid(null);
            else
                incomingPhoneNumber.setUssdApplicationSid(getSid("UssdApplicationSid", data));
        }

        incomingPhoneNumber.setDateUpdated(DateTime.now());
        return incomingPhoneNumber;
    }

    public Response deleteIncomingPhoneNumber(final String accountSid, final String sid) {
        Account operatedAccount = accountsDao.getAccount(accountSid);
        secure(operatedAccount, "RestComm:Delete:IncomingPhoneNumbers");
        final IncomingPhoneNumber incomingPhoneNumber = dao.getIncomingPhoneNumber(new Sid(sid));
        secure(operatedAccount, incomingPhoneNumber.getAccountSid(), SecuredType.SECURED_STANDARD);
        if(phoneNumberProvisioningManager != null && (incomingPhoneNumber.isPureSip() == null || !incomingPhoneNumber.isPureSip())) {
            phoneNumberProvisioningManager.cancelNumber(convertIncomingPhoneNumbertoPhoneNumber(incomingPhoneNumber));
        }
        dao.removeIncomingPhoneNumber(new Sid(sid));
        return noContent().build();
    }

    /*
    @SuppressWarnings("unchecked")
    private List<SipURI> getOutboundInterfaces() {
        final List<SipURI> uris = (List<SipURI>) context.getAttribute(SipServlet.OUTBOUND_INTERFACES);
        return uris;
    }
    */

    public static org.restcomm.connect.provisioning.number.api.PhoneNumber convertIncomingPhoneNumbertoPhoneNumber(IncomingPhoneNumber incomingPhoneNumber) {
        return new org.restcomm.connect.provisioning.number.api.PhoneNumber(
                incomingPhoneNumber.getFriendlyName(),
                incomingPhoneNumber.getPhoneNumber(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                incomingPhoneNumber.isVoiceCapable(),
                incomingPhoneNumber.isSmsCapable(),
                incomingPhoneNumber.isMmsCapable(),
                incomingPhoneNumber.isFaxCapable(),
                false);
    }
}
