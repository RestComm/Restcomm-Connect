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
package org.mobicents.servlet.restcomm.dao.mybatis;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import org.joda.time.DateTime;

import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.*;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisApplicationsDao implements ApplicationsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ApplicationsDao.";
    private final SqlSessionFactory sessions;

    public MybatisApplicationsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addApplication(final Application application) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addApplication", toMap(application));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Application getApplication(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getApplication", sid.toString());
            if (result != null) {
                return toApplication(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Application> getApplications(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getApplications", accountSid.toString());
            final List<Application> applications = new ArrayList<Application>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    applications.add(toApplication(result));
                }
            }
            return applications;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeApplication(final Sid sid) {
        removeApplications("removeApplication", sid);
    }

    @Override
    public void removeApplications(final Sid accountSid) {
        removeApplications("removeApplications", accountSid);
    }

    private void removeApplications(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateApplication(final Application application) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateApplication", toMap(application));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Application toApplication(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final String friendlyName = readString(map.get("friendly_name"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final String apiVersion = readString(map.get("api_version"));
        final URI voiceUrl = readUri(map.get("voice_url"));
        final String voiceMethod = readString(map.get("voice_method"));
        final URI voiceFallbackUrl = readUri(map.get("voice_fallback_url"));
        final String voiceFallbackMethod = readString(map.get("voice_fallback_method"));
        final URI statusCallback = readUri(map.get("status_callback"));
        final String statusCallbackMethod = readString(map.get("status_callback_method"));
        final Boolean hasVoiceCallerIdLookup = readBoolean(map.get("voice_caller_id_lookup"));
        final URI smsUrl = readUri(map.get("sms_url"));
        final String smsMethod = readString(map.get("sms_method"));
        final URI smsFallbackUrl = readUri(map.get("sms_fallback_url"));
        final String smsFallbackMethod = readString(map.get("sms_fallback_method"));
        final URI smsStatusCallback = readUri(map.get("sms_status_callback"));
        final URI uri = readUri(map.get("uri"));
        return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, voiceUrl, voiceMethod,
                voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, hasVoiceCallerIdLookup, smsUrl,
                smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback, uri);
    }

    private Map<String, Object> toMap(final Application application) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(application.getSid()));
        map.put("date_created", writeDateTime(application.getDateCreated()));
        map.put("date_updated", writeDateTime(application.getDateUpdated()));
        map.put("friendly_name", application.getFriendlyName());
        map.put("account_sid", writeSid(application.getAccountSid()));
        map.put("api_version", application.getApiVersion());
        map.put("voice_url", writeUri(application.getVoiceUrl()));
        map.put("voice_method", application.getVoiceMethod());
        map.put("voice_fallback_url", writeUri(application.getVoiceFallbackUrl()));
        map.put("voice_fallback_method", application.getVoiceFallbackMethod());
        map.put("status_callback", writeUri(application.getStatusCallback()));
        map.put("status_callback_method", application.getStatusCallbackMethod());
        map.put("voice_caller_id_lookup", application.hasVoiceCallerIdLookup());
        map.put("sms_url", writeUri(application.getSmsUrl()));
        map.put("sms_method", application.getSmsMethod());
        map.put("sms_fallback_url", writeUri(application.getSmsFallbackUrl()));
        map.put("sms_fallback_method", application.getSmsFallbackMethod());
        map.put("sms_status_callback", writeUri(application.getSmsStatusCallback()));
        map.put("uri", writeUri(application.getUri()));
        return map;
    }
}
