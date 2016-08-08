/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

import org.apache.commons.lang.NotImplementedException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.DaoUtils;
import org.mobicents.servlet.restcomm.dao.OrgIdentityDao;
import org.mobicents.servlet.restcomm.entities.OrgIdentity;
import org.mobicents.servlet.restcomm.entities.OrgIdentity.Status;
import org.mobicents.servlet.restcomm.entities.Sid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Orestis Tsakiridis
 */
public class MybatisOrgIdentityDao implements OrgIdentityDao {

    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.OrgIdentityDao.";
    private final SqlSessionFactory sessions;

    public MybatisOrgIdentityDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addOrgIdentity(final OrgIdentity instance) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addOrgIdentity", toMap(instance));
            session.commit();
        } finally {
            session.close();
        }

    }

    @Override
    public OrgIdentity getOrgIdentity(Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getOrgIdentity", sid.toString());
            if (result != null) {
                return toOrgIdentity(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public OrgIdentity getOrgIdentityByName(String name) {
        final SqlSession session = sessions.openSession();
        try {
            // TODO see what happens if the query returns more than one results (an exception will be thrown)
            final Map<String, Object> result = session.selectOne(namespace + "getOrgIdentityByName", name);
            if (result != null) {
                return toOrgIdentity(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public OrgIdentity getOrgIdentityByOrganizationSid(Sid organizationSid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getOrgIdentityByOrganizationSid", organizationSid.toString());
            if (result != null) {
                return toOrgIdentity(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<OrgIdentity> getOrgIdentities() {
        throw new NotImplementedException();
    }

    @Override
    public void removeOrgIdentity(Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "removeOrgIdentity", sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateOrgIdentity(OrgIdentity instance) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateOrgIdentity", toMap(instance));
            session.commit();
        } finally {
            session.close();
        }
    }

    private OrgIdentity toOrgIdentity(final Map<String, Object> map) {
        final Sid sid = DaoUtils.readSid(map.get("sid"));
        final Sid organizationSid = DaoUtils.readSid(map.get("organization_sid"));
        final String name = DaoUtils.readString(map.get("name"));
        final DateTime dateCreated = DaoUtils.readDateTime(map.get("date_created"));
        final DateTime dateUpdated = DaoUtils.readDateTime(map.get("date_updated"));

        final String restcommRestRAT = DaoUtils.readString(map.get("restcomm_rat"));
        final DateTime restcommRestLastRegistrationDate = DaoUtils.readDateTime(map.get("restcomm_last_registration_date"));
        final Status restcommRestStatus = Status.getValueOf(DaoUtils.readString(map.get("restcomm_status")));
        final String restcommRestClientSecret = DaoUtils.readString(map.get("restcomm_client_secret"));

        return new OrgIdentity(sid,organizationSid,name, dateCreated, dateUpdated, restcommRestRAT,restcommRestLastRegistrationDate,restcommRestStatus, restcommRestClientSecret);
    }

    private Map<String, Object> toMap(final OrgIdentity instance) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", DaoUtils.writeSid(instance.getSid()));
        map.put("organization_sid",DaoUtils.writeSid(instance.getOrganizationSid()));
        map.put("name", instance.getName());
        map.put("date_created", DaoUtils.writeDateTime(instance.getDateCreated()));
        map.put("date_updated", DaoUtils.writeDateTime(instance.getDateUpdated()));

        map.put("restcomm_rat", instance.getRestcommRAT());
        map.put("restcomm_last_registration_date", DaoUtils.writeDateTime(instance.getRestcommLastRegistrationDate()));
        map.put("restcomm_status", instance.getRestcommStatus());
        map.put("restcomm_client_secret", instance.getRestcommClientSecret());

        return map;
    }
}
