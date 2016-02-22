/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
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

import static org.mobicents.servlet.restcomm.dao.DaoUtils.readDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readSid;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeSid;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.dao.InstanceIdDao;
import org.mobicents.servlet.restcomm.entities.InstanceId;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@ThreadSafe
public class MybatisInstanceIdDao implements InstanceIdDao{
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.InstanceIdDao.";
    private final SqlSessionFactory sessions;

    public MybatisInstanceIdDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public InstanceId getInstanceId() {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace+"getInstanceId");
            if (result != null) {
                return toInstanceId(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void addInstancecId(InstanceId instanceId) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addInstanceId", toMap(instanceId));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateInstanceId(InstanceId instanceId) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateInstanceId", toMap(instanceId));
            session.commit();
        } finally {
            session.close();
        }
    }

    private InstanceId toInstanceId(Map<String, Object> map) {
        final Sid sid = readSid(map.get("instance_id"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        return new InstanceId(sid, dateCreated, dateUpdated);
    }

   private Map<String, Object> toMap(final InstanceId instanceId) {
       final Map<String, Object> map = new HashMap<String, Object>();
       map.put("instance_id", writeSid(instanceId.getId()));
       map.put("date_created", writeDateTime(instanceId.getDateCreated()));
       map.put("date_updated", writeDateTime(instanceId.getDateUpdated()));
       return map;
   }
}
