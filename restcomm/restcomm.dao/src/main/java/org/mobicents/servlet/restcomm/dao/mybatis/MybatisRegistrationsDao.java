/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.readDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readInteger;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readSid;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readString;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeSid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.entities.Registration;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@gmail.com (Jean Deruelle)
 */
@ThreadSafe
public final class MybatisRegistrationsDao implements RegistrationsDao {
    private static final Logger logger = Logger.getLogger(MybatisRegistrationsDao.class);

    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.RegistrationsDao.";
    private final SqlSessionFactory sessions;

    public MybatisRegistrationsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addRegistration(final Registration registration) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addRegistration", toMap(registration));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Registration getRegistration(String user) {
        final SqlSession session = sessions.openSession();
        try {
            // https://bitbucket.org/telestax/telscale-restcomm/issue/107/dial-fails-to-call-a-client-registered
            // we get all registrations and sort them by latest updated date so that we target the device where the user last
            // updated the registration
            final List<Map<String, Object>> results = session.selectList(namespace + "getRegistration", user);
            final List<Registration> records = new ArrayList<Registration>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    records.add(toPresenceRecord(result));
                }
                if (records.isEmpty()) {
                    return null;
                } else {
                    Collections.sort(records);
                    return records.get(0);
                }
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Registration> getRegistrations() {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getRegistrations");
            final List<Registration> records = new ArrayList<Registration>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    records.add(toPresenceRecord(result));
                }
            }
            return records;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean hasRegistration(final Registration registration) {
        final SqlSession session = sessions.openSession();
        try {
            final Integer result = (Integer) session.selectOne(namespace + "hasRegistration", toMap(registration));
            return result != null && result > 0;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeRegistration(final Registration registration) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "removeRegistration", toMap(registration));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateRegistration(final Registration registration) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateRegistration", toMap(registration));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(final Registration registration) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(registration.getSid()));
        map.put("date_created", writeDateTime(registration.getDateCreated()));
        map.put("date_updated", writeDateTime(registration.getDateUpdated()));
        map.put("date_expires", writeDateTime(registration.getDateExpires()));
        map.put("address_of_record", registration.getAddressOfRecord());
        map.put("display_name", registration.getDisplayName());
        map.put("user_name", registration.getUserName());
        map.put("location", registration.getLocation());
        map.put("user_agent", registration.getUserAgent());
        map.put("ttl", registration.getTimeToLive());
        return map;
    }

    private Registration toPresenceRecord(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final DateTime dateExpires = readDateTime(map.get("date_expires"));
        final String addressOfRecord = readString(map.get("address_of_record"));
        final String dislplayName = readString(map.get("display_name"));
        final String userName = readString(map.get("user_name"));
        final String location = readString(map.get("location"));
        final String userAgent = readString(map.get("user_agent"));
        final Integer timeToLive = readInteger(map.get("ttl"));
        return new Registration(sid, dateCreated, dateUpdated, dateExpires, addressOfRecord, dislplayName, userName, userAgent,
                timeToLive, location);
    }
}
