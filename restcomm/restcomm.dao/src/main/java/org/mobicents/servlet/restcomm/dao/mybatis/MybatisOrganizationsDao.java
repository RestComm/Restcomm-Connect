/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

import static org.mobicents.servlet.restcomm.dao.DaoUtils.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.OrganizationsDao;
import org.mobicents.servlet.restcomm.entities.Organization;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author guilherme.jansen@telestax.com
 */
@NotThreadSafe
public class MybatisOrganizationsDao implements OrganizationsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.OrganizationsDao.";
    private final SqlSessionFactory sessions;

    public MybatisOrganizationsDao(SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addOrganization(Organization organization) {
        final SqlSession session = sessions.openSession();
        try{
            session.insert(namespace + "addOrganization", toMap(organization));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Organization getOrganization(Sid sid) {
        return getOrganization(namespace + "getOrganization", sid.toString());
    }

    @Override
    public Organization getOrganization(String namespace) {
        return getOrganization(this.namespace + "getOrganizationByNamespace", namespace);
    }

    private Organization getOrganization(final String selector, final String parameter) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(selector, parameter);
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
            final List<Organization> organizations = new ArrayList<Organization>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    organizations.add(toOrganization(result));
                }
            }
            return organizations;
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

    @Override
    public void removeOrganization(Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "removeOrganization", sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    private Organization toOrganization(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final String friendlyName = readString(map.get("friendly_name"));
        final String namespace = readString(map.get("namespace"));
        final String apiVersion = readString(map.get("api_version"));
        final URI uri = readUri(map.get("uri"));
        return new Organization(sid, dateCreated, dateUpdated, friendlyName, namespace, apiVersion, uri);
    }

    private Map<String, Object> toMap(final Organization organization) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(organization.getSid()));
        map.put("date_created", writeDateTime(organization.getDateCreated()));
        map.put("date_updated", writeDateTime(organization.getDateUpdated()));
        map.put("friendly_name", organization.getFriendlyName());
        map.put("namespace", organization.getNamespace());
        map.put("api_version", organization.getApiVersion());
        map.put("uri", writeUri(organization.getUri()));
        return map;
    }

}
