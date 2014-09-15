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

import static org.mobicents.servlet.restcomm.dao.DaoUtils.*;
import org.mobicents.servlet.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.restcomm.entities.Recording;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisRecordingsDao implements RecordingsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.RecordingsDao.";
    private final SqlSessionFactory sessions;

    public MybatisRecordingsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addRecording(final Recording recording) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addRecording", toMap(recording));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Recording getRecording(final Sid sid) {
        return getRecording(namespace + "getRecording", sid);
    }

    @Override
    public Recording getRecordingByCall(final Sid callSid) {
        return getRecording(namespace + "getRecordingByCall", callSid);
    }

    private Recording getRecording(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(selector, sid.toString());
            if (result != null) {
                return toRecording(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Recording> getRecordings(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getRecordings", accountSid.toString());
            final List<Recording> recordings = new ArrayList<Recording>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    recordings.add(toRecording(result));
                }
            }
            return recordings;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeRecording(final Sid sid) {
        removeRecording(namespace + "removeRecording", sid);
    }

    @Override
    public void removeRecordings(final Sid accountSid) {
        removeRecording(namespace + "removeRecordings", accountSid);
    }

    private void removeRecording(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(final Recording recording) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(recording.getSid()));
        map.put("date_created", writeDateTime(recording.getDateCreated()));
        map.put("date_updated", writeDateTime(recording.getDateUpdated()));
        map.put("account_sid", writeSid(recording.getAccountSid()));
        map.put("call_sid", writeSid(recording.getCallSid()));
        map.put("duration", recording.getDuration());
        map.put("api_version", recording.getApiVersion());
        map.put("uri", writeUri(recording.getUri()));
        return map;
    }

    private Recording toRecording(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final Sid callSid = readSid(map.get("call_sid"));
        final Double duration = readDouble(map.get("duration"));
        final String apiVersion = readString(map.get("api_version"));
        final URI uri = readUri(map.get("uri"));
        return new Recording(sid, dateCreated, dateUpdated, accountSid, callSid, duration, apiVersion, uri);
    }
}
