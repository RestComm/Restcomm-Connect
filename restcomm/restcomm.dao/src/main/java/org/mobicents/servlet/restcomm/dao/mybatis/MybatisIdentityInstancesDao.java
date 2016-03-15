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

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.DaoUtils;
import org.mobicents.servlet.restcomm.dao.IdentityInstancesDao;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.exceptions.ConstraintViolationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Orestis Tsakiridis
 */
public class MybatisIdentityInstancesDao implements IdentityInstancesDao {

    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.IdentityInstancesDao.";
    private final SqlSessionFactory sessions;

    public MybatisIdentityInstancesDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addIdentityInstance(final IdentityInstance instance) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addIdentityInstance", toMap(instance));
            session.commit();
        } finally {
            session.close();
        }

    }

    @Override
    public IdentityInstance getIdentityInstance(Sid sid) {
        IdentityInstance instance = null;

        instance = getIdentityInstance(namespace + "getIdentityInstance", name);
        if (account == null){
            account = getAccount(namespace + "getAccountByEmail", name);
        }
        if (account == null) {
            account = getAccount(namespace + "getAccount", name);
        }

        return account;
    }


    private IdentityInstance getIdentityInstance(final String selector, final Object parameters) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(selector, parameters);
            if (result != null) {
                return toIdentityInstance(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public IdentityInstance getIdentityInstanceByName(String name) {
        return null;
    }

    @Override
    public List<IdentityInstance> getIdentityInstances() {
        return null;
    }

    @Override
    public void removeIdentityInstance(Sid sid) {

    }

    @Override
    public void updateIdentityInstance(IdentityInstance instance) {

    }

    private IdentityInstance toIdentityInstance(final Map<String, Object> map) {
        final Sid sid = DaoUtils.readSid(map.get("sid"));
        final Sid organizationSid = DaoUtils.readSid(map.get("organizationSid"));
        final String name = DaoUtils.readString(map.get("name"));

        final String restcommRestRAT = DaoUtils.readString(map.get("restcomm_rest_rat"));
        final DateTime restcommRestLastRegistrationDate = DaoUtils.readDateTime(map.get("restcomm_rest_last_registration_date"));
        final String restcommRestStatus = DaoUtils.readString(map.get("restcomm_rest_status"));

        final String restcommUiRAT = DaoUtils.readString(map.get("restcomm_ui_rat"));
        final DateTime restcommUiLastRegistrationDate = DaoUtils.readDateTime(map.get("restcomm_ui_last_registration_date"));
        final String restcommUiStatus = DaoUtils.readString(map.get("restcomm_ui_status"));

        final String rvdRestRAT = DaoUtils.readString(map.get("rvd_rest_rat"));
        final DateTime rvdRestLastRegistrationDate = DaoUtils.readDateTime(map.get("rvd_rest_last_registration_date"));
        final String rvdRestStatus = DaoUtils.readString(map.get("rvd_rest_status"));

        final String rvdUiRAT = DaoUtils.readString(map.get("rvd_ui_rat"));
        final DateTime rvdUiLastRegistrationDate = DaoUtils.readDateTime(map.get("rvd_ui_last_registration_date"));
        final String rvdUiStatus = DaoUtils.readString(map.get("rvd_ui_status"));

        return new IdentityInstance(sid,organizationSid,name,restcommRestRAT,restcommRestLastRegistrationDate,restcommRestStatus,restcommUiRAT,restcommUiLastRegistrationDate,restcommUiStatus,rvdRestRAT,rvdRestLastRegistrationDate,rvdRestStatus,rvdUiRAT,rvdUiLastRegistrationDate,rvdUiStatus);
    }

    private Map<String, Object> toMap(final IdentityInstance instance) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", DaoUtils.writeSid(instance.getSid()));
        map.put("organization_sid",DaoUtils.writeSid(instance.getOrganizationSid()));
        map.put("name", instance.getOrganizationSid());

        map.put("restcomm_rest_rat", instance.getRestcommRestRAT());
        map.put("restcomm_rest_last_registration_date", DaoUtils.writeDateTime(instance.getRestcommRestLastRegistrationDate()));
        map.put("restcomm_rest_status", instance.getRestcommRestStatus());

        map.put("restcomm_ui_rat", instance.getRestcommUiRAT());
        map.put("restcomm_ui_last_registration_date", DaoUtils.writeDateTime(instance.getRestcommUiLastRegistrationDate()));
        map.put("restcomm_ui_status", instance.getRestcommUiStatus());

        map.put("rvd_rest_rat", instance.getRvdRestRAT());
        map.put("rvd_rest_last_registration_date", DaoUtils.writeDateTime(instance.getRvdRestLastRegistrationDate()));
        map.put("rvd_rest_status", instance.getRvdRestStatus());

        map.put("rvd_rat", instance.getRvdUiRAT());
        map.put("rvd_ui_last_registration_date", DaoUtils.writeDateTime(instance.getRvdUiLastRegistrationDate()));
        map.put("rvd_ui_status", instance.getRvdUiStatus());

        return map;
    }
}
