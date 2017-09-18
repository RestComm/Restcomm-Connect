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
import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.OrganizationsDao;
import org.restcomm.connect.dao.entities.Organization;

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
@ThreadSafe
public final class MybatisOrganizationDao implements OrganizationsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.OrganizationsDao.";
    private final SqlSessionFactory sessions;

    public MybatisOrganizationDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addOrganization(final Organization organization) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addOrganization", toMap(organization));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Organization getOrganization(Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getOrganization", sid.toString());
            if (result != null) {
                return toOrganization(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public Organization getOrganizationByDomainName(String domainName) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getOrganizationByDomainName", domainName);
            if (result != null) {
                return toOrganization(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public Organization getOrganizationByStatus(Organization.Status status) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getOrganizationByStatus", status.toString());
            if (result != null) {
                return toOrganization(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Organization> getAllOrganizations() {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getAllOrganizations");
            final List<Organization> organization = new ArrayList<Organization>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    organization.add(toOrganization(result));
                }
            }
            return organization;
        } finally {
            session.close();
        }
    }

    @Override
    public void updateOrganization(Organization organization) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateOrganization", toMap(organization));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Organization toOrganization(final Map<String, Object> map) {
        final Sid sid = DaoUtils.readSid(map.get("sid"));
        final String domainName = DaoUtils.readString(map.get("domain_name"));
        final DateTime dateCreated = DaoUtils.readDateTime(map.get("date_created"));
        final DateTime dateUpdated = DaoUtils.readDateTime(map.get("date_updated"));
        final Organization.Status status = DaoUtils.readOrganizationStatus(map.get("status"));
        return new Organization(sid, domainName, dateCreated, dateUpdated, status);
    }

    private Map<String, Object> toMap(final Organization org) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", DaoUtils.writeSid(org.getSid()));
        map.put("domain_name", org.getDomainName());
        map.put("date_created", DaoUtils.writeDateTime(org.getDateCreated()));
        map.put("date_updated", DaoUtils.writeDateTime(org.getDateUpdated()));
        map.put("status", DaoUtils.writeOrganizationStatus(org.getStatus()));
        return map;
    }
}
