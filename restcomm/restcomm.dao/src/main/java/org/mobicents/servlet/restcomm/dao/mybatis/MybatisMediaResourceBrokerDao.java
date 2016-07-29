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

import static org.mobicents.servlet.restcomm.dao.DaoUtils.readBoolean;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readSid;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readString;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeSid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.dao.MediaResourceBrokerDao;
import org.mobicents.servlet.restcomm.entities.MediaResourceBrokerEntity;
import org.mobicents.servlet.restcomm.entities.MediaResourceBrokerEntityFilter;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@ThreadSafe
public final class MybatisMediaResourceBrokerDao implements MediaResourceBrokerDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.MediaResourceBrokerDao.";
    private final SqlSessionFactory sessions;

    public MybatisMediaResourceBrokerDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addMediaResourceBrokerEntity(MediaResourceBrokerEntity ms) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addMediaResourceBrokerEntity", toMap(ms));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateMediaResource(MediaResourceBrokerEntity ms) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "updateMediaResource", toMap(ms));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public MediaResourceBrokerEntity getMediaResourceBrokerEntity(String msId) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getMediaResourceBrokerEntity", msId);
            if (result != null) {
                return toMRBEntity(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<MediaResourceBrokerEntity> getMediaResourceBrokerEntities() {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getMediaServers");
            final List<MediaResourceBrokerEntity> mList = new ArrayList<MediaResourceBrokerEntity>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    mList.add(toMRBEntity(result));
                }
            }
            return mList;
        } finally {
            session.close();
        }
    }

    @Override
    public List<MediaResourceBrokerEntity> getMediaResourceBrokerEntitiesByFilter(
            MediaResourceBrokerEntityFilter filter) {

        final SqlSession session = sessions.openSession();

        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getMediaResourceBrokerEntitiesByFilter",
                    filter);
            final List<MediaResourceBrokerEntity> entities = new ArrayList<MediaResourceBrokerEntity>();

            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    entities.add(toMRBEntity(result));
                }
            }
            return entities;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeMediaResourceBrokerEntity(String msId) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "removeMediaResourceBrokerEntity", msId);
            session.commit();
        } finally {
            session.close();
        }
    }

    private MediaResourceBrokerEntity toMRBEntity(final Map<String, Object> map) {
        final Sid callSid = readSid(map.get("call_sid"));
        final Sid conferenceSid = readSid(map.get("conference_sid"));
        final String masterMsId = readString(map.get("master_ms_id"));
        final String masterMsBridgeEpId = readString(map.get("master_ms_bridge_ep_id"));
        final String masterMsCnfEpId = readString(map.get("master_ms_cnf_ep_id"));
        final String slaveMsId = readString(map.get("slave_ms_id"));
        final String slaveMsBridgeEpId = readString(map.get("slave_ms_bridge_ep_id"));
        final String slaveMsCnfEpId = readString(map.get("slave_ms_cnf_ep_id"));
        final boolean isBridgedTogether = readBoolean(map.get("is_bridged_together"));
        final String masterMsSDP = readString(map.get("master_ms_sdp"));
        final String slaveMsSDP = readString(map.get("slave_ms_sdp"));

        return new MediaResourceBrokerEntity(callSid, conferenceSid, masterMsId, masterMsBridgeEpId, masterMsCnfEpId, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether, masterMsSDP, slaveMsSDP);
    }

    private Map<String, Object> toMap(final MediaResourceBrokerEntity ms) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("call_sid", writeSid(ms.getCallSid()));
        map.put("conference_sid", writeSid(ms.getConferenceSid()));
        map.put("master_ms_id", ms.getMasterMsId());
        map.put("master_ms_bridge_ep_id", ms.getMasterMsBridgeEpId());
        map.put("master_ms_cnf_ep_id", ms.getMasterMsCnfEpId());
        map.put("slave_ms_id", ms.getSlaveMsId());
        map.put("slave_ms_bridge_ep_id", ms.getSlaveMsBridgeEpId());
        map.put("slave_ms_cnf_ep_id", ms.getSlaveMsCnfEpId());
        map.put("is_bridged_together", ms.isBridgedTogether());
        map.put("master_ms_sdp", ms.getMasterMsSDP());
        map.put("slave_ms_sdp", ms.getSlaveMsSDP());
        return map;
    }
}
