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
package org.restcomm.connect.dao.mybatis;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.ConferenceDetailRecordsDao;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceDetailRecordFilter;

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
    public void updateConferenceDetailRecordMasterBridgeEndpointID(ConferenceDetailRecord cdr) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateConferenceDetailRecordMasterBridgeEndpointID", toMap(cdr));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateMasterPresent(ConferenceDetailRecord cdr) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateMasterPresent", toMap(cdr));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateModeratorPresent(ConferenceDetailRecord cdr) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateModeratorPresent", toMap(cdr));
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
        final Sid sid = DaoUtils.readSid(map.get("sid"));
        final DateTime dateCreated = DaoUtils.readDateTime(map.get("date_created"));
        final DateTime dateUpdated = DaoUtils.readDateTime(map.get("date_updated"));
        final Sid accountSid = DaoUtils.readSid(map.get("account_sid"));
        final String status = DaoUtils.readString(map.get("status"));
        final String friendlyName = DaoUtils.readString(map.get("friendly_name"));
        final String apiVersion = DaoUtils.readString(map.get("api_version"));
        final URI uri = DaoUtils.readUri(map.get("uri"));
        final String msId = DaoUtils.readString(map.get("master_ms_id"));
        final String masterConferenceEndpointId = DaoUtils.readString(map.get("master_conference_endpoint_id"));
        final String masterIVREndpointId = DaoUtils.readString(map.get("master_ivr_endpoint_id"));
        boolean masterPresent = false;
        String masterIVREndpointSessionId = null;
        String masterBridgeEndpointId = null;
        String masterBridgeEndpointSessionId = null;
        String masterBridgeConnectionIdentifier = null;
        String masterIVRConnectionIdentifier = null;
        boolean moderatorPresent = false;
        try {
            masterPresent = DaoUtils.readBoolean(map.get("master_present"));
            masterIVREndpointSessionId = DaoUtils.readString(map.get("master_ivr_endpoint_session_id"));
            masterBridgeEndpointId = DaoUtils.readString(map.get("master_bridge_endpoint_id"));
            masterBridgeEndpointSessionId = DaoUtils.readString(map.get("master_bridge_endpoint_session_id"));
            masterBridgeConnectionIdentifier = DaoUtils.readString(map.get("master_bridge_conn_id"));
            masterIVRConnectionIdentifier = DaoUtils.readString(map.get("master_ivr_conn_id"));
            moderatorPresent = DaoUtils.readBoolean(map.get("moderator_present"));
        } catch (Exception e) {
        }
        return new ConferenceDetailRecord(sid, dateCreated, dateUpdated, accountSid, status, friendlyName, apiVersion, uri,
                msId, masterConferenceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId,
                masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier,
                masterIVRConnectionIdentifier, moderatorPresent);
    }

    private Map<String, Object> toMap(final ConferenceDetailRecord cdr) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", DaoUtils.writeSid(cdr.getSid()));
        map.put("date_created", DaoUtils.writeDateTime(cdr.getDateCreated()));
        map.put("date_updated", DaoUtils.writeDateTime(cdr.getDateUpdated()));
        map.put("account_sid", DaoUtils.writeSid(cdr.getAccountSid()));
        map.put("status", cdr.getStatus());
        map.put("friendly_name", cdr.getFriendlyName());
        map.put("api_version", cdr.getApiVersion());
        map.put("uri", DaoUtils.writeUri(cdr.getUri()));
        map.put("master_ms_id", cdr.getMasterMsId());
        map.put("master_conference_endpoint_id", cdr.getMasterConferenceEndpointId());
        map.put("master_ivr_endpoint_id", cdr.getMasterIVREndpointId());
        map.put("master_ivr_endpoint_session_id", cdr.getMasterIVREndpointSessionId());
        map.put("master_bridge_endpoint_id", cdr.getMasterBridgeEndpointId());
        map.put("master_bridge_endpoint_session_id", cdr.getMasterBridgeEndpointSessionId());
        map.put("master_present", cdr.isMasterPresent());
        map.put("master_bridge_conn_id", cdr.getMasterBridgeConnectionIdentifier());
        map.put("master_ivr_conn_id", cdr.getMasterIVRConnectionIdentifier());
        map.put("moderator_present", cdr.isModeratorPresent());
        return map;
    }
}
