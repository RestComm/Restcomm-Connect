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
import org.mobicents.servlet.restcomm.dao.GatewaysDao;
import org.mobicents.servlet.restcomm.entities.Gateway;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisGatewaysDao implements GatewaysDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.GatewaysDao.";
    private final SqlSessionFactory sessions;

    public MybatisGatewaysDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addGateway(final Gateway gateway) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addGateway", toMap(gateway));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Gateway getGateway(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getGateway", sid.toString());
            if (result != null) {
                return toGateway(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Gateway> getGateways() {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getGateways");
            final List<Gateway> gateways = new ArrayList<Gateway>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    gateways.add(toGateway(result));
                }
            }
            return gateways;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeGateway(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "removeGateway", sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateGateway(final Gateway gateway) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateGateway", toMap(gateway));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Gateway toGateway(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final String friendlName = readString(map.get("friendly_name"));
        final String password = readString(map.get("password"));
        final String proxy = readString(map.get("proxy"));
        final Boolean register = readBoolean(map.get("register"));
        final String userAgent = readString(map.get("user_name"));
        final Integer timeToLive = readInteger(map.get("ttl"));
        final URI uri = readUri(map.get("uri"));
        return new Gateway(sid, dateCreated, dateUpdated, friendlName, password, proxy, register, userAgent, timeToLive, uri);
    }

    private Map<String, Object> toMap(final Gateway gateway) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(gateway.getSid()));
        map.put("date_created", writeDateTime(gateway.getDateCreated()));
        map.put("date_updated", writeDateTime(gateway.getDateUpdated()));
        map.put("friendly_name", gateway.getFriendlyName());
        map.put("password", gateway.getPassword());
        map.put("proxy", gateway.getProxy());
        map.put("register", gateway.register());
        map.put("user_name", gateway.getUserName());
        map.put("ttl", gateway.getTimeToLive());
        map.put("uri", writeUri(gateway.getUri()));
        return map;
    }
}
