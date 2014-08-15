/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
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
package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.readBoolean;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readSid;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readString;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readUri;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeSid;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeUri;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumberFilter;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
@ThreadSafe
public final class MybatisIncomingPhoneNumbersDao implements IncomingPhoneNumbersDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumbersDao.";
    private final SqlSessionFactory sessions;

    public MybatisIncomingPhoneNumbersDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addIncomingPhoneNumber(final IncomingPhoneNumber incomingPhoneNumber) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addIncomingPhoneNumber", toMap(incomingPhoneNumber));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public IncomingPhoneNumber getIncomingPhoneNumber(final Sid sid) {
        return getIncomingPhoneNumber("getIncomingPhoneNumber", sid.toString());
    }

    @Override
    public IncomingPhoneNumber getIncomingPhoneNumber(final String phoneNumber) {
        return getIncomingPhoneNumber("getIncomingPhoneNumberByValue", phoneNumber);
    }

    private IncomingPhoneNumber getIncomingPhoneNumber(final String selector, Object parameter) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + selector, parameter);
            if (result != null) {
                return toIncomingPhoneNumber(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<IncomingPhoneNumber> getIncomingPhoneNumbers(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getIncomingPhoneNumbers",
                    accountSid.toString());
            final List<IncomingPhoneNumber> incomingPhoneNumbers = new ArrayList<IncomingPhoneNumber>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    incomingPhoneNumbers.add(toIncomingPhoneNumber(result));
                }
            }
            return incomingPhoneNumbers;
        } finally {
            session.close();
        }
    }

    @Override
    public List<IncomingPhoneNumber> getIncomingPhoneNumbersByFilter(IncomingPhoneNumberFilter filter) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getIncomingPhoneNumbersByFriendlyName",
                    filter);
            final List<IncomingPhoneNumber> incomingPhoneNumbers = new ArrayList<IncomingPhoneNumber>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    incomingPhoneNumbers.add(toIncomingPhoneNumber(result));
                }
            }
            return incomingPhoneNumbers;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeIncomingPhoneNumber(final Sid sid) {
        removeIncomingPhoneNumbers("removeIncomingPhoneNumber", sid);
    }

    @Override
    public void removeIncomingPhoneNumbers(final Sid accountSid) {
        removeIncomingPhoneNumbers("removeIncomingPhoneNumbers", accountSid);
    }

    private void removeIncomingPhoneNumbers(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateIncomingPhoneNumber(final IncomingPhoneNumber incomingPhoneNumber) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateIncomingPhoneNumber", toMap(incomingPhoneNumber));
            session.commit();
        } finally {
            session.close();
        }
    }

    private IncomingPhoneNumber toIncomingPhoneNumber(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final String friendlyName = readString(map.get("friendly_name"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final String phoneNumber = readString(map.get("phone_number"));
        final String apiVersion = readString(map.get("api_version"));
        final Boolean hasVoiceCallerIdLookup = readBoolean(map.get("voice_caller_id_lookup"));
        final URI voiceUrl = readUri(map.get("voice_url"));
        final String voiceMethod = readString(map.get("voice_method"));
        final URI voiceFallbackUrl = readUri(map.get("voice_fallback_url"));
        final String voiceFallbackMethod = readString(map.get("voice_fallback_method"));
        final URI statusCallback = readUri(map.get("status_callback"));
        final String statusCallbackMethod = readString(map.get("status_callback_method"));
        final Sid voiceApplicationSid = readSid(map.get("voice_application_sid"));
        final URI smsUrl = readUri(map.get("sms_url"));
        final String smsMethod = readString(map.get("sms_method"));
        final URI smsFallbackUrl = readUri(map.get("sms_fallback_url"));
        final String smsFallbackMethod = readString(map.get("sms_fallback_method"));
        final Sid smsApplicationSid = readSid(map.get("sms_application_sid"));
        final URI uri = readUri(map.get("uri"));
        final URI ussdUrl = readUri(map.get("ussd_url"));
        final String ussdMethod = readString(map.get("ussd_method"));
        final URI ussdFallbackUrl = readUri(map.get("ussd_fallback_url"));
        final String ussdFallbackMethod = readString(map.get("ussd_fallback_method"));
        final Sid ussdApplicationSid = readSid(map.get("ussd_application_sid"));
        final Boolean voiceCapable = readBoolean(map.get("voice_capable"));
        final Boolean smsCapable = readBoolean(map.get("sms_capable"));
        final Boolean mmsCapable = readBoolean(map.get("mms_capable"));
        final Boolean faxCapable = readBoolean(map.get("fax_capable"));
        return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, apiVersion,
                hasVoiceCallerIdLookup, voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback,
                statusCallbackMethod, voiceApplicationSid, smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod,
                smsApplicationSid, uri, ussdUrl, ussdMethod, ussdFallbackUrl, ussdFallbackMethod, ussdApplicationSid, voiceCapable, smsCapable, mmsCapable, faxCapable);
    }

    private Map<String, Object> toMap(final IncomingPhoneNumber incomingPhoneNumber) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(incomingPhoneNumber.getSid()));
        map.put("date_created", writeDateTime(incomingPhoneNumber.getDateCreated()));
        map.put("date_updated", writeDateTime(incomingPhoneNumber.getDateUpdated()));
        map.put("friendly_name", incomingPhoneNumber.getFriendlyName());
        map.put("account_sid", writeSid(incomingPhoneNumber.getAccountSid()));
        map.put("phone_number", incomingPhoneNumber.getPhoneNumber());
        map.put("api_version", incomingPhoneNumber.getApiVersion());
        map.put("voice_caller_id_lookup", incomingPhoneNumber.hasVoiceCallerIdLookup());
        map.put("voice_url", writeUri(incomingPhoneNumber.getVoiceUrl()));
        map.put("voice_method", incomingPhoneNumber.getVoiceMethod());
        map.put("voice_fallback_url", writeUri(incomingPhoneNumber.getVoiceFallbackUrl()));
        map.put("voice_fallback_method", incomingPhoneNumber.getVoiceFallbackMethod());
        map.put("status_callback", writeUri(incomingPhoneNumber.getStatusCallback()));
        map.put("status_callback_method", incomingPhoneNumber.getStatusCallbackMethod());
        map.put("voice_application_sid", writeSid(incomingPhoneNumber.getVoiceApplicationSid()));
        map.put("sms_url", writeUri(incomingPhoneNumber.getSmsUrl()));
        map.put("sms_method", incomingPhoneNumber.getSmsMethod());
        map.put("sms_fallback_url", writeUri(incomingPhoneNumber.getSmsFallbackUrl()));
        map.put("sms_fallback_method", incomingPhoneNumber.getSmsFallbackMethod());
        map.put("sms_application_sid", writeSid(incomingPhoneNumber.getSmsApplicationSid()));
        map.put("uri", writeUri(incomingPhoneNumber.getUri()));
        map.put("ussd_url", writeUri(incomingPhoneNumber.getUssdUrl()));
        map.put("ussd_method", incomingPhoneNumber.getUssdMethod());
        map.put("ussd_fallback_url", writeUri(incomingPhoneNumber.getUssdFallbackUrl()));
        map.put("ussd_fallback_method", incomingPhoneNumber.getUssdFallbackMethod());
        map.put("ussd_application_sid", writeSid(incomingPhoneNumber.getUssdApplicationSid()));
        map.put("voice_capable", incomingPhoneNumber.isVoiceCapable());
        map.put("sms_capable", incomingPhoneNumber.isSmsCapable());
        map.put("mms_capable", incomingPhoneNumber.isMmsCapable());
        map.put("fax_capable", incomingPhoneNumber.isFaxCapable());
        return map;
    }
}
