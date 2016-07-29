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

import static org.mobicents.servlet.restcomm.dao.DaoUtils.readString;

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
        final String msId = readString(map.get("ms_id"));
        final String msIpAddress = readString(map.get("ms_ip_address"));
        final String msPort = readString(map.get("ms_port"));
        final String compatibility = readString(map.get("compatibility"));
        final String timeOut = readString(map.get("timeout"));

        return new MediaResourceBrokerEntity(msId, msIpAddress, msPort, compatibility, timeOut);
    }

    private Map<String, Object> toMap(final MediaResourceBrokerEntity ms) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("ms_id", ms.getMsId());
        map.put("ms_ip_address", ms.getMsIpAddress());
        map.put("ms_port", ms.getMsPort());
        map.put("compatibility", ms.getCompatibility());
        map.put("timeout", ms.getTimeOut());
        return map;
    }
}
