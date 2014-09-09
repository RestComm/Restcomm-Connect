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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.*;
import org.mobicents.servlet.restcomm.dao.HttpCookiesDao;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MybatisHttpCookiesDao implements HttpCookiesDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.HttpCookiesDao.";

    private final SqlSessionFactory sessions;

    public MybatisHttpCookiesDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addCookie(final Sid sid, final Cookie cookie) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addCookie", toMap(sid, cookie));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public List<Cookie> getCookies(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getCookies", sid.toString());
            final List<Cookie> cookies = new ArrayList<Cookie>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    cookies.add(toCookie(result));
                }
            }
            return cookies;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean hasCookie(final Sid sid, final Cookie cookie) {
        final SqlSession session = sessions.openSession();
        try {
            final Integer result = session.selectOne(namespace + "hasCookie", toMap(sid, cookie));
            if (result > 0) {
                return true;
            } else {
                return false;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public boolean hasExpiredCookies(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Integer result = session.selectOne(namespace + "hasExpiredCookies", sid.toString());
            if (result > 0) {
                return true;
            } else {
                return false;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void removeCookies(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "removeCookies", sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void removeExpiredCookies(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "removeExpiredCookies", sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateCookie(final Sid sid, final Cookie cookie) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateCookie", toMap(sid, cookie));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Cookie toCookie(final Map<String, Object> map) {
        final String comment = readString(map.get("comment"));
        final String domain = readString(map.get("domain"));
        final Date expirationDate = (Date) map.get("expiration_date");
        final String name = readString(map.get("name"));
        final String path = readString(map.get("path"));
        final String value = readString(map.get("value"));
        final int version = readInteger(map.get("version"));
        final BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setComment(comment);
        cookie.setDomain(domain);
        cookie.setExpiryDate(expirationDate);
        cookie.setPath(path);
        cookie.setVersion(version);
        return cookie;
    }

    private Map<String, Object> toMap(final Sid sid, final Cookie cookie) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(sid));
        map.put("comment", cookie.getComment());
        map.put("domain", cookie.getDomain());
        map.put("expiration_date", cookie.getExpiryDate());
        map.put("name", cookie.getName());
        map.put("path", cookie.getPath());
        map.put("value", cookie.getValue());
        map.put("version", cookie.getVersion());
        return map;
    }
}
