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

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.ProfilesDao;
import org.restcomm.connect.dao.entities.Profile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
@ThreadSafe
public final class MybatisProfilesDao implements ProfilesDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ProfilesDao.";
    private final SqlSessionFactory sessions;

    public MybatisProfilesDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public Profile getProfile(String sid) throws SQLException {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getProfile", sid.toString());
            if (result != null) {
                return toProfile(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Profile> getAllProfiles() throws SQLException {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getAllProfiles");
            final List<Profile> profiles = new ArrayList<Profile>();

            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    profiles.add(toProfile(result));
                }
            }
            return profiles;
        } finally {
            session.close();
        }
    }

    @Override
    public int addProfile(Profile profile) {
        final SqlSession session = sessions.openSession();
        int effectedRows = 0;
        try {
            effectedRows = session.insert(namespace + "addProfile", profile);
            session.commit();
        } finally {
            session.close();
        }
        return effectedRows;
    }

    @Override
    public void updateProfile(Profile profile) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateProfile", profile);
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void deleteProfile(String sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "deleteProfile", sid);
            session.commit();
        } finally {
            session.close();
        }
    }


    private Profile toProfile(final Map<String, Object> map) throws SQLException {
        final String sid = DaoUtils.readString(map.get("sid"));
        final DateTime dateCreated = DaoUtils.readDateTime(map.get("date_created"));
        final DateTime dateUpdated = DaoUtils.readDateTime(map.get("date_updated"));
//        byte[] documentArr = null;
//        if (map.get("document") instanceof Blob) {
//            final Blob document = (Blob) map.get("document");
//            documentArr = document.getBytes(1, (int) document.length());
//        } else {
//            documentArr = (byte[]) map.get("document");
//        }
        final String document = DaoUtils.readString(map.get("document"));
        return new Profile(sid, document, dateCreated.toDate(), dateUpdated.toDate());
    }
}
