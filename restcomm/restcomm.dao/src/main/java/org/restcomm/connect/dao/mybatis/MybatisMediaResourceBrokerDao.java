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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.MediaResourceBrokerDao;
import org.restcomm.connect.dao.entities.MediaResourceBrokerEntity;
import org.restcomm.connect.dao.entities.MediaResourceBrokerEntityFilter;

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
    public List<MediaResourceBrokerEntity> getConnectedSlaveEntitiesByConfSid(Sid conferenceSid) {

        final SqlSession session = sessions.openSession();

        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getConnectedSlaveEntitiesByConfSid",
                    conferenceSid.toString());
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
    public void removeMediaResourceBrokerEntity(MediaResourceBrokerEntityFilter filter) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "removeMediaResourceBrokerEntity", filter);
            session.commit();
        } finally {
            session.close();
        }
    }

    private MediaResourceBrokerEntity toMRBEntity(final Map<String, Object> map) {
        final Sid conferenceSid = DaoUtils.readSid(map.get("conference_sid"));
        final String slaveMsId = DaoUtils.readString(map.get("slave_ms_id"));
        final String slaveMsBridgeEpId = DaoUtils.readString(map.get("slave_ms_bridge_ep_id"));
        final String slaveMsCnfEpId = DaoUtils.readString(map.get("slave_ms_cnf_ep_id"));
        final boolean isBridgedTogether = DaoUtils.readBoolean(map.get("is_bridged_together"));

        return new MediaResourceBrokerEntity(conferenceSid, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether);
    }

    private Map<String, Object> toMap(final MediaResourceBrokerEntity ms) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("conference_sid", DaoUtils.writeSid(ms.getConferenceSid()));
        map.put("slave_ms_id", ms.getSlaveMsId());
        map.put("slave_ms_bridge_ep_id", ms.getSlaveMsBridgeEpId());
        map.put("slave_ms_cnf_ep_id", ms.getSlaveMsCnfEpId());
        map.put("is_bridged_together", ms.isBridgedTogether());
        return map;
    }
}
