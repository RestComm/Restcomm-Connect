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
import org.mobicents.servlet.restcomm.mappers.ApplicationsMapper;
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
            ApplicationsMapper mapper=session.getMapper(ApplicationsMapper.class);
            mapper.addApplication(toMap(application));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Application getApplication(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
        	ApplicationsMapper mapper=session.getMapper(ApplicationsMapper.class);
            final Map<String, Object> result = mapper.getApplication(sid.toString());
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
    public Application getApplication(final String friendlyName) {
        final SqlSession session = sessions.openSession();
        try {
        	ApplicationsMapper mapper=session.getMapper(ApplicationsMapper.class);
            final Map<String, Object> result = mapper.getApplicationByFriendlyName(friendlyName);
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
            ApplicationsMapper mapper=session.getMapper(ApplicationsMapper.class);
            final List<Map<String, Object>> results = mapper.getApplications(accountSid.toString());
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
        final SqlSession session = sessions.openSession();
        try {
            ApplicationsMapper mapper=session.getMapper(ApplicationsMapper.class);
            mapper.removeApplication(sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void removeApplications(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            ApplicationsMapper mapper=session.getMapper(ApplicationsMapper.class);
            mapper.removeApplications(accountSid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }


    @Override
    public void updateApplication(final Application application) {
        final SqlSession session = sessions.openSession();
        try {
            ApplicationsMapper mapper=session.getMapper(ApplicationsMapper.class);
            mapper.updateApplication(toMap(application));
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
        final Boolean hasVoiceCallerIdLookup = readBoolean(map.get("voice_caller_id_lookup"));
        final URI uri = readUri(map.get("uri"));
        final URI rcmlUrl = readUri(map.get("rcml_url"));
        final Application.Kind kind = readApplicationKind(map.get("kind"));
        return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
                uri, rcmlUrl, kind);
    }

    private Map<String, Object> toMap(final Application application) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(application.getSid()));
        map.put("date_created", writeDateTime(application.getDateCreated()));
        map.put("date_updated", writeDateTime(application.getDateUpdated()));
        map.put("friendly_name", application.getFriendlyName());
        map.put("account_sid", writeSid(application.getAccountSid()));
        map.put("api_version", application.getApiVersion());
        map.put("voice_caller_id_lookup", application.hasVoiceCallerIdLookup());
        map.put("uri", writeUri(application.getUri()));
        map.put("rcml_url", writeUri(application.getRcmlUrl()));
        map.put("kind", writeApplicationKind(application.getKind()));
        return map;
    }
}
