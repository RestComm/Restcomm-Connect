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
import org.mobicents.servlet.restcomm.dao.ConferenceDetailRecordsDao;
import org.mobicents.servlet.restcomm.entities.ConferenceDetailRecord;
import org.mobicents.servlet.restcomm.entities.ConferenceDetailRecordFilter;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
@ThreadSafe
public final class MybatisConferenceDetailRecordsDao implements ConferenceDetailRecordsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ConferenceDetailRecordsDao.";
    private final SqlSessionFactory sessions;

    public MybatisConferenceDetailRecordsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public int addConferenceDetailRecord(ConferenceDetailRecord cdr) {
        final SqlSession session = sessions.openSession();
        int effectedRows = 0;
        try {
            effectedRows = session.insert(namespace + "addConferenceDetailRecord", toMap(cdr));
            session.commit();
        } finally {
            session.close();
        }
        return effectedRows;
    }

    @Override
    public ConferenceDetailRecord getConferenceDetailRecord(Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getConferenceDetailRecord", sid.toString());
            if (result != null) {
                return toConferenceDetailRecord(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public Integer getTotalConferenceDetailRecords(ConferenceDetailRecordFilter filter) {

        final SqlSession session = sessions.openSession();
        try {
            final Integer total = session.selectOne(namespace + "getTotalConferenceDetailRecordByUsingFilters", filter);
            return total;
        } finally {
            session.close();
        }

    }

    @Override
    public List<ConferenceDetailRecord> getConferenceDetailRecords(ConferenceDetailRecordFilter filter) {

        final SqlSession session = sessions.openSession();

        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getConferenceDetailRecordByUsingFilters",
                    filter);
            final List<ConferenceDetailRecord> cdrs = new ArrayList<ConferenceDetailRecord>();

            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    cdrs.add(toConferenceDetailRecord(result));
                }
            }
            return cdrs;
        } finally {
            session.close();
        }
    }

    @Override
    public List<ConferenceDetailRecord> getConferenceDetailRecords(final Sid accountSid) {
        return getConferenceDetailRecords(namespace + "getConferenceDetailRecords", accountSid.toString());
    }

    @Override
    public List<ConferenceDetailRecord> getConferenceDetailRecordsByStatus(String status) {
        return getConferenceDetailRecords(namespace + "getConferenceDetailRecordsByStatus", status);
    }

    @Override
    public List<ConferenceDetailRecord> getConferenceDetailRecordsByDateCreated(final DateTime dateCreated) {
        return getConferenceDetailRecords(namespace + "getConferenceDetailRecordsByDateCreated", dateCreated.toDate());
    }

    @Override
    public List<ConferenceDetailRecord> getConferenceDetailRecordsByDateUpdated(final DateTime dateUpdated) {
        return getConferenceDetailRecords(namespace + "getConferenceDetailRecordsByDateUpdated", dateUpdated.toDate());
    }

    @Override
    public void updateConferenceDetailRecordStatus(ConferenceDetailRecord cdr) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateConferenceDetailRecordStatus", toMap(cdr));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateConferenceDetailRecordMasterEndpointID(ConferenceDetailRecord cdr) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateConferenceDetailRecordMasterEndpointID", toMap(cdr));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void removeConferenceDetailRecord(Sid sid) {
        // TODO Add support for conference modification after basic API's as twillio's
    }

    @Override
    public void removeConferenceDetailRecords(Sid accountSid) {
        // TODO Add support for conference modification after basic API's as twillio's
    }

    private List<ConferenceDetailRecord> getConferenceDetailRecords(final String selector, Object input) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(selector, input);
            final List<ConferenceDetailRecord> cdrs = new ArrayList<ConferenceDetailRecord>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    cdrs.add(toConferenceDetailRecord(result));
                }
            }
            return cdrs;
        } finally {
            session.close();
        }
    }

    private ConferenceDetailRecord toConferenceDetailRecord(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final String status = readString(map.get("status"));
        final String friendlyName = readString(map.get("friendly_name"));
        final String apiVersion = readString(map.get("api_version"));
        final URI uri = readUri(map.get("uri"));
        final String msId = readString(map.get("master_ms_id"));
        final String masterConferenceEndpointId = readString(map.get("master_conference_endpoint_id"));
        return new ConferenceDetailRecord(sid, dateCreated, dateUpdated, accountSid, status, friendlyName, apiVersion, uri, msId, masterConferenceEndpointId);
    }

    private Map<String, Object> toMap(final ConferenceDetailRecord cdr) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(cdr.getSid()));
        map.put("date_created", writeDateTime(cdr.getDateCreated()));
        map.put("date_updated", writeDateTime(cdr.getDateUpdated()));
        map.put("account_sid", writeSid(cdr.getAccountSid()));
        map.put("status", cdr.getStatus());
        map.put("friendly_name", cdr.getFriendlyName());
        map.put("api_version", cdr.getApiVersion());
        map.put("uri", writeUri(cdr.getUri()));
        map.put("master_ms_id", cdr.getMasterMsId());
        map.put("master_conference_endpoint_id", cdr.getMasterConferenceEndpointId());
        return map;
    }
}
