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

import java.net.URI;
import java.math.BigInteger;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

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

    protected Integer getInteger(final String name, final MultivaluedMap<String, String> data) {
        Integer integer = null;
        if (data.containsKey(name)) {
            integer = new Integer(data.getFirst(name));
        }
        return integer;
    }

    protected BigInteger getBigInteger(final String name, final MultivaluedMap<String, String> data) {
        BigInteger bigInteger = null;
        if (data.containsKey(name)) {
            bigInteger = new BigInteger(data.getFirst(name));
        }
        return bigInteger;
    }

    protected Long getLong(final String name, final MultivaluedMap<String, String> data) {
        Long l = null;
        if (data.containsKey(name)) {
            l = new Long(data.getFirst(name));
        }
        return l;
    }

    protected DateTime getDateTime(final String name, final MultivaluedMap<String, String> data) {
        DateTime dateTime = null;
        if (data.containsKey(name)) {
            dateTime = new DateTime(data.getFirst(name));
        }
        return dateTime;
    }

    protected Boolean getBoolean(final String name, final MultivaluedMap<String, String> data) {
        Boolean b = null;
        if (data.containsKey(name)) {
            b = new Boolean(data.getFirst(name));
        }
        return b;
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

    // A general purpose method to test incoming parameters for meaningful data
    protected boolean isEmpty(Object value) {
        if (value == null)
            return true;
        if (value.equals(""))
            return true;
        return false;
    }
}
