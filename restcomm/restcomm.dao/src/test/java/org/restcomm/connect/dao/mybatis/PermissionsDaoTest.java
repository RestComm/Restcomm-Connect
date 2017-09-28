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

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.Permission;

public class PermissionsDaoTest extends DaoTest{
    private static MybatisDaoManager manager;
    private MybatisPermissionsDao permissionsDao;

    Sid sid1 = new Sid("PE00000000000000000000000000000001");
    Sid sid2 = new Sid("PE00000000000000000000000000000002");
    Sid sid3 = new Sid("PE00000000000000000000000000000003");
    Sid sid4 = new Sid("PE00000000000000000000000000000004");
    String name1 = "RestComm:*:MOD1";
    String name2 = "RestComm:*:MOD2";
    String name3 = "RestComm:*:MOD3";
    String name4 = "RestComm:*:MOD4";
    Permission permission1 = null;
    Permission permission2 = null;
    Permission permission3 = null;
    Permission permission4 = null;

    @Before
    public void before() throws Exception {
        sandboxRoot = createTempDir("permissionsTest");
        String mybatisFilesPath = getClass().getResource("/permissionsDao").getFile();
        setupSandbox(mybatisFilesPath, sandboxRoot);

        String mybatisXmlPath = sandboxRoot.getPath() + "/mybatis_updated.xml";
        final InputStream data = new FileInputStream(mybatisXmlPath);

        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
        permissionsDao = (MybatisPermissionsDao) manager.getPermissionsDao();

        permission1 = null;
        permission2 = null;
        permission3 = null;
        permission4 = null;
    }

    @After
    public void after() {
        manager.shutdown();
        removeTempDir(sandboxRoot.getAbsolutePath());
    }

    @Test
    public void testGetPermissions() {
        assertNull(permission1);
        assertNull(permission2);
        assertNull(permission3);
        assertNull(permission4);

        //TODO: all the validations can be abstracted
        List<Permission> permissions1 = permissionsDao.getPermissions();
        List<Permission> permissions2 =  new ArrayList<Permission>();
        permissions2.add(new Permission(sid1, name1));
        permissions2.add(new Permission(sid2, name2));
        permissions2.add(new Permission(sid3, name3));

        assertTrue(permissions1.size()==3);
//        for(Permission p:permissions1){
//            System.out.println(p.getSid().toString()+" "+p.getName());
//        }
        assertTrue(permissions1.equals(permissions2));
    }
    @Test
    public void testGetPermission() {
        assertNull(permission1);
        assertNull(permission2);
        assertNull(permission3);
        assertNull(permission4);

        //TODO: all the validations can be abstracted
        permission1 = permissionsDao.getPermission(sid1);
        assertNotNull(permission1);
        assertTrue(permission1.getSid().equals(sid1));
        assertTrue(permission1.getName().equals(name1));

        permission2 = permissionsDao.getPermission(sid2);
        assertNotNull(permission2);
        assertTrue(permission2.getSid().equals(sid2));
        assertTrue(permission2.getName().equals(name2));

        permission3 = permissionsDao.getPermission(sid3);
        assertNotNull(permission3);
        assertTrue(permission3.getSid().equals(sid3));
        assertTrue(permission3.getName().equals(name3));

        //failure case
        permission4 = permissionsDao.getPermission(sid4);
        assertNull(permission4);
    }

    @Test
    public void testAddPermission() {
        assertNull(permission4);

        permission4 = permissionsDao.getPermission(sid4);
        assertNull(permission4);

        permission4 = new Permission(sid4, name4);
        permissionsDao.addPermission(permission4);

        permission4 = permissionsDao.getPermission(sid4);
        assertNotNull(permission4);
        assertTrue(permission4.getSid().equals(sid4));
        assertTrue(permission4.getName().equals(name4));
    }

    @Test(expected = PersistenceException.class) 
    public void testAddPermissionFail() throws Exception {
        assertNull(permission1);

        //failure case
        permission1 = new Permission(sid1, name1);
        permissionsDao.addPermission(permission1);
    }

    @Test
    public void testUpdatePermission() {
        assertNull(permission1);

        permission1 = permissionsDao.getPermission(sid1);
        assertNotNull(permission1);
        assertTrue(permission1.getSid().equals(sid1));
        assertTrue(permission1.getName().equals(name1));

        permission1.setName(name2);
        permissionsDao.updatePermission(sid1, permission1);

        permission1 = permissionsDao.getPermission(sid1);
        assertNotNull(permission1);
        assertTrue(permission1.getSid().equals(sid1));
        assertTrue(permission1.getName().equals(name2));
    }

    @Test(expected = PersistenceException.class)
    public void testUpdatePermissionFail() throws Exception {
        assertNull(permission1);

        //update nonexistent
        permission1 = permissionsDao.getPermission(sid1);
        permission1.setName(name2);
        permissionsDao.updatePermission(sid4, permission1);//Exception
    }

    @Test
    public void testDeletePermission() {
        assertNull(permission1);

        permission1 = permissionsDao.getPermission(sid1);
        assertNotNull(permission1);
        assertTrue(permission1.getSid().equals(sid1));
        assertTrue(permission1.getName().equals(name1));

        permissionsDao.deletePermission(sid1);
        permission1 = permissionsDao.getPermission(sid1);
        assertNull(permission1);
    }

    @Ignore
    @Test(expected = PersistenceException.class)
    public void testDeletePermissionFail() {
        //we dont enforce fk constraints, so we cant expect failures here
    }
}
