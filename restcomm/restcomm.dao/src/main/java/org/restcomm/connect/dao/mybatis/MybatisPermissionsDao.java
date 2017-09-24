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
package org.restcomm.connect.dao.mybatis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.PermissionsDao;
import org.restcomm.connect.dao.entities.Permission;

public class MybatisPermissionsDao implements PermissionsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.PermissionsDao.";
    private final SqlSessionFactory sessions;

    public MybatisPermissionsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public Permission getPermission(Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getPermission", sid.toString());
            if (result != null) {
                return toPermission(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    public Permission getPermissionById(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Permission getPermissionByName(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addPermission(final Permission permission) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addPermission", toMap(permission));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void deletePermission(final Sid sid) {
        deletePermission(namespace + "deletePermission", sid);
    }

    private void deletePermission(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updatePermission(Sid sid, Permission permission) {
        final SqlSession session = sessions.openSession();
        HashedMap map = (HashedMap)toMap(permission);
        map.put("sid", sid.toString());
        try {
            session.insert(namespace + "updatePermission", map);
            session.commit();
        } finally {
            session.close();
        }
    }

    private Permission toPermission(Map<String, Object> result) {
        Sid sid = new Sid((String)result.get("sid"));
        String name = (String)result.get("name");
        return new Permission(sid, name);
    }

    private Map toMap(Permission permission) {
        HashedMap map = new HashedMap();
        map.put("sid", permission.getSid().toString());
        map.put("name", permission.getName());
        return map;
    }

    @Override
    public List<Permission> getPermissions() {
        final SqlSession session = sessions.openSession();
        List<Permission> permissions = new ArrayList<>();
        Sid sid1 = new Sid("PE00000000000000000000000000000001");
        Sid sid2 = new Sid("PE00000000000000000000000000000002");
        String name1 = "RestComm:*:USSD";
        String name2 = "RestComm:*:ASR";
        permissions.add(new Permission(sid1, name1));
        permissions.add(new Permission(sid2, name2));
//        try {
//            session.selectList(namespace + "getPermissions");
//
//        } finally {
//            session.close();
//        }
        return permissions;
    }
}
