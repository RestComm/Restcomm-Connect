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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.util.StringUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
@NotThreadSafe
public abstract class AbstractEndpoint {
    private String defaultApiVersion;
    protected Configuration configuration;
    protected String baseRecordingsPath;

    public AbstractEndpoint() {
        super();
    }

    protected void init(final Configuration configuration) {
        final String path = configuration.getString("recordings-path");
        baseRecordingsPath = StringUtils.addSuffixIfNotPresent(path, "/");
        defaultApiVersion = configuration.getString("api-version");
    }

    protected String getApiVersion(final MultivaluedMap<String, String> data) {
        String apiVersion = defaultApiVersion;
        if (data != null && data.containsKey("ApiVersion")) {
            apiVersion = data.getFirst("ApiVersion");
        }
        return apiVersion;
    }

    protected PhoneNumber getPhoneNumber(final MultivaluedMap<String, String> data) {
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        PhoneNumber phoneNumber = null;
        try {
            phoneNumber = phoneNumberUtil.parse(data.getFirst("PhoneNumber"), "US");
        } catch (final NumberParseException ignored) {
        }
        return phoneNumber;
    }

    protected String getMethod(final String name, final MultivaluedMap<String, String> data) {
        String method = "POST";
        if (data.containsKey(name)) {
            method = data.getFirst(name);
        }
        return method;
    }

    protected Sid getSid(final String name, final MultivaluedMap<String, String> data) {
        Sid sid = null;
        if (data.containsKey(name)) {
            sid = new Sid(data.getFirst(name));
        }
        return sid;
    }

    protected URI getUrl(final String name, final MultivaluedMap<String, String> data) {
        URI uri = null;
        if (data.containsKey(name)) {
            uri = URI.create(data.getFirst(name));
        }
        return uri;
    }

    protected boolean getHasVoiceCallerIdLookup(final MultivaluedMap<String, String> data) {
        boolean hasVoiceCallerIdLookup = false;
        if (data.containsKey("VoiceCallerIdLookup")) {
            final String value = data.getFirst("VoiceCallerIdLookup");
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return hasVoiceCallerIdLookup;
    }

/*
    protected void secure(final Account account, final String permission) throws AuthorizationException {
        final Subject subject = SecurityUtils.getSubject();
        if (account != null && account.getSid() != null) {
            final Sid accountSid = account.getSid();
            if (account.getStatus().equals(Account.Status.ACTIVE)
                    && (subject.hasRole("Administrator") || (subject.getPrincipal().toString().equals(accountSid.toString()) && subject
                            .isPermitted(permission)))) {
                return;
            } else {
                throw new AuthorizationException();
            }
        } else {
            throw new AuthorizationException();
        }
    }
    */

    // A general purpose method to test incoming parameters for meaningful data
    protected boolean isEmpty(Object value) {
        if (value == null)
            return true;
        if ( value.equals("") )
            return true;
        return false;
    }

    // Quick'n'dirty error response building
    String buildErrorResponseBody(String message, MediaType type) {
        if (!type.equals(MediaType.APPLICATION_XML_TYPE)) { // fallback to JSON if not XML
            return "{\"message\":"+message+"}";
        } else {
            return "<RestcommResponse><Message>" + message + "</Message></RestcommResponse>";
        }
    }

    String buildErrorResponseBody(String message, String error, MediaType type) {
        if (!type.equals(MediaType.APPLICATION_XML_TYPE)) { // fallback to JSON if not XML
            return "{\"message\":\""+message+"\",\n\"error\":"+error+"}";
        } else {
            return "<RestcommResponse><Message>" + message + "</Message><Error>"+ error +"</Error></RestcommResponse>";
        }
    }
}
