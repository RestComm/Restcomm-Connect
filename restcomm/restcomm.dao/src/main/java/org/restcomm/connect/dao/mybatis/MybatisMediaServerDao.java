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
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.MediaServersDao;
import org.restcomm.connect.dao.entities.MediaServerEntity;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@ThreadSafe
public final class MybatisMediaServerDao implements MediaServersDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.MediaServersDao.";
    private final SqlSessionFactory sessions;

    public MybatisMediaServerDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addMediaServer(final MediaServerEntity ms) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addMediaServer", toMap(ms));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateMediaServer(final MediaServerEntity ms) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "updateMediaServer", toMap(ms));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public List<MediaServerEntity> getMediaServerEntityByIP(final String msIPAddress) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getMediaServerEntityByIP", msIPAddress);
            final List<MediaServerEntity> msList = new ArrayList<MediaServerEntity>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    msList.add(toMediaServer(result));
                }
            }
            return msList;
        } finally {
            session.close();
        }
    }

    @Override
    public List<MediaServerEntity> getMediaServers() {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getMediaServers");
            final List<MediaServerEntity> msList = new ArrayList<MediaServerEntity>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    msList.add(toMediaServer(result));
                }
            }
            return msList;
        } finally {
            session.close();
        }
    }

    @Override
    public MediaServerEntity getMediaServer(String msId) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getMediaServer", msId);
            if (result != null) {
                return toMediaServer(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void removeMediaServerEntity(String msId) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "removeMediaServerEntity", msId);
            session.commit();
        } finally {
            session.close();
        }
    }

    private MediaServerEntity toMediaServer(final Map<String, Object> map) {
        final int msId = DaoUtils.readInteger(map.get("ms_id"));
        final String compatibility = DaoUtils.readString(map.get("compatibility"));
        final String localIpAddress = DaoUtils.readString(map.get("local_ip"));
        final int localPort = DaoUtils.readInteger(map.get("local_port"));
        final String remoteIpAddress = DaoUtils.readString(map.get("remote_ip"));
        final int remotePort = DaoUtils.readInteger(map.get("remote_port"));
        final String responseTimeout = DaoUtils.readString(map.get("response_timeout"));
        final String externalAddress = DaoUtils.readString(map.get("external_address"));

        return new MediaServerEntity(msId, compatibility, localIpAddress, localPort, remoteIpAddress, remotePort, responseTimeout, externalAddress);
    }

    private Map<String, Object> toMap(final MediaServerEntity ms) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("ms_id", ms.getMsId());
        map.put("compatibility", ms.getCompatibility());
        map.put("local_ip", ms.getLocalIpAddress());
        map.put("local_port", ms.getLocalPort());
        map.put("remote_ip", ms.getRemoteIpAddress());
        map.put("remote_port", ms.getRemotePort());
        map.put("response_timeout", ms.getResponseTimeout());
        map.put("external_address", ms.getExternalAddress());
        return map;
    }
}
