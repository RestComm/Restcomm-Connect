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

import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.dao.InstanceIdDao;
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
    public Sid getInstanceId() {
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
    public void addInstancecId(Sid instanceId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateInstanceId(Sid instanceId) {
        // TODO Auto-generated method stub
        
    }


    /**
     * @param result
     * @return
     */
    private Sid toInstanceId(Map<String, Object> result) {
        // TODO Auto-generated method stub
        return null;
    }

}
