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

import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.ShortCodesDao;
import org.restcomm.connect.dao.entities.ShortCode;
import org.restcomm.connect.dao.entities.Sid;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisShortCodesDao implements ShortCodesDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ShortCodesDao.";
    private final SqlSessionFactory sessions;

    public MybatisShortCodesDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addShortCode(final ShortCode shortCode) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addShortCode", toMap(shortCode));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public ShortCode getShortCode(final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getShortCode", sid.toString());
            if (result != null) {
                return toShortCode(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<ShortCode> getShortCodes(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getShortCodes", accountSid.toString());
            final List<ShortCode> shortCodes = new ArrayList<ShortCode>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    shortCodes.add(toShortCode(result));
                }
            }
            return shortCodes;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeShortCode(final Sid sid) {
        removeShortCodes(namespace + "removeShortCode", sid);
    }

    @Override
    public void removeShortCodes(final Sid accountSid) {
        removeShortCodes(namespace + "removeShortCodes", accountSid);
    }

    private void removeShortCodes(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateShortCode(final ShortCode shortCode) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateShortCode", toMap(shortCode));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(final ShortCode shortCode) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", DaoUtils.writeSid(shortCode.getSid()));
        map.put("date_created", DaoUtils.writeDateTime(shortCode.getDateCreated()));
        map.put("date_updated", DaoUtils.writeDateTime(shortCode.getDateUpdated()));
        map.put("friendly_name", shortCode.getFriendlyName());
        map.put("account_sid", DaoUtils.writeSid(shortCode.getAccountSid()));
        map.put("short_code", shortCode.getShortCode());
        map.put("api_version", shortCode.getApiVersion());
        map.put("sms_url", DaoUtils.writeUri(shortCode.getSmsUrl()));
        map.put("sms_method", shortCode.getSmsMethod());
        map.put("sms_fallback_url", DaoUtils.writeUri(shortCode.getSmsFallbackUrl()));
        map.put("sms_fallback_method", shortCode.getSmsFallbackMethod());
        map.put("uri", DaoUtils.writeUri(shortCode.getUri()));
        return map;
    }

    private ShortCode toShortCode(final Map<String, Object> map) {
        final Sid sid = DaoUtils.readSid(map.get("sid"));
        final DateTime dateCreated = DaoUtils.readDateTime(map.get("date_created"));
        final DateTime dateUpdated = DaoUtils.readDateTime(map.get("date_updated"));
        final String friendlyName = DaoUtils.readString(map.get("friendly_name"));
        final Sid accountSid = DaoUtils.readSid(map.get("account_sid"));
        final Integer shortCode = DaoUtils.readInteger(map.get("short_code"));
        final String apiVersion = DaoUtils.readString(map.get("api_version"));
        final URI smsUrl = DaoUtils.readUri(map.get("sms_url"));
        final String smsMethod = DaoUtils.readString(map.get("sms_method"));
        final URI smsFallbackUrl = DaoUtils.readUri(map.get("sms_fallback_url"));
        final String smsFallbackMethod = DaoUtils.readString(map.get("sms_fallback_method"));
        final URI uri = DaoUtils.readUri(map.get("uri"));
        return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion, smsUrl, smsMethod,
                smsFallbackUrl, smsFallbackMethod, uri);
    }
}
