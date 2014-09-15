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
import org.mobicents.servlet.restcomm.dao.OutgoingCallerIdsDao;
import org.mobicents.servlet.restcomm.entities.OutgoingCallerId;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisOutgoingCallerIdsDao implements OutgoingCallerIdsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.OutgoingCallerIdsDao.";
    private final SqlSessionFactory sessions;

    public MybatisOutgoingCallerIdsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addOutgoingCallerId(final OutgoingCallerId outgoingCallerId) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addOutgoingCallerId", toMap(outgoingCallerId));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public OutgoingCallerId getOutgoingCallerId(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getOutgoingCallerId", sid.toString());
            if (result != null) {
                return toOutgoingCallerId(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<OutgoingCallerId> getOutgoingCallerIds(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getOutgoingCallerIds",
                    accountSid.toString());
            final List<OutgoingCallerId> outgoingCallerIds = new ArrayList<OutgoingCallerId>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    outgoingCallerIds.add(toOutgoingCallerId(result));
                }
            }
            return outgoingCallerIds;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeOutgoingCallerId(final Sid sid) {
        removeOutgoingCallerIds(namespace + "removeOutgoingCallerId", sid);
    }

    @Override
    public void removeOutgoingCallerIds(final Sid accountSid) {
        removeOutgoingCallerIds(namespace + "removeOutgoingCallerIds", accountSid);
    }

    private void removeOutgoingCallerIds(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateOutgoingCallerId(final OutgoingCallerId outgoingCallerId) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateOutgoingCallerId", toMap(outgoingCallerId));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(final OutgoingCallerId outgoingCallerId) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(outgoingCallerId.getSid()));
        map.put("date_created", writeDateTime(outgoingCallerId.getDateCreated()));
        map.put("date_updated", writeDateTime(outgoingCallerId.getDateUpdated()));
        map.put("friendly_name", outgoingCallerId.getFriendlyName());
        map.put("account_sid", writeSid(outgoingCallerId.getAccountSid()));
        map.put("phone_number", outgoingCallerId.getPhoneNumber());
        map.put("uri", writeUri(outgoingCallerId.getUri()));
        return map;
    }

    private OutgoingCallerId toOutgoingCallerId(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final String friendlyName = readString(map.get("friendly_name"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final String phoneNumber = readString(map.get("phone_number"));
        final URI uri = readUri(map.get("uri"));
        return new OutgoingCallerId(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, uri);
    }
}
