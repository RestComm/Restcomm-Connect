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
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.Permission;
import org.restcomm.connect.dao.entities.AccountPermission;

public class AccountsPermissionsDaoTest extends DaoTest{
    private static MybatisDaoManager manager;
    private MybatisPermissionsDao permissionsDao;
    private MybatisAccountsDao accountsDao;

    Sid account_sid1 = new Sid("AC00000000000000000000000000000001");
    Sid account_sid2 = new Sid("AC00000000000000000000000000000002");
    Sid permission_sid1 = new Sid("PE00000000000000000000000000000001");
    Sid permission_sid2 = new Sid("PE00000000000000000000000000000002");
    Sid permission_sid3 = new Sid("PE00000000000000000000000000000003");
    Sid permission_sid4 = new Sid("PE00000000000000000000000000000004");
    String permission_name1 = "RestComm:*:MOD1";
    String permission_name2 = "RestComm:*:MOD2";
    String permission_name3 = "RestComm:*:MOD3";
    String permission_name4 = "RestComm:*:MOD4";
    Permission permission1 = null;
    Permission permission2 = null;
    Permission permission3 = null;
    Permission permission4 = null;
    List<Permission> permissions1 = null;
    List<Permission> permissions2 = null;

    @Before
    public void before() throws Exception {
        sandboxRoot = createTempDir("accountPermissionsTest");
        String mybatisFilesPath = getClass().getResource("/accountsPermissionsDao").getFile();
        setupSandbox(mybatisFilesPath, sandboxRoot);

        String mybatisXmlPath = sandboxRoot.getPath() + "/mybatis_updated.xml";
        final InputStream data = new FileInputStream(mybatisXmlPath);
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
        permissionsDao = (MybatisPermissionsDao) manager.getPermissionsDao();
        accountsDao = (MybatisAccountsDao) manager.getAccountsDao();

        permissions1 = new ArrayList<Permission>();
        permissions2 = new ArrayList<Permission>();
    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void testGetAccountPermissions(){
        permissions1 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions1.size()==2);
        assertTrue(((AccountPermission)permissions1.get(0)).getSid().equals(permission_sid1));
        assertTrue(((AccountPermission)permissions1.get(0)).getName().equals(permission_name1));
        assertTrue(((AccountPermission)permissions1.get(0)).getValue());

        assertTrue(((AccountPermission)permissions1.get(1)).getSid().equals(permission_sid2));
        assertTrue(((AccountPermission)permissions1.get(1)).getName().equals(permission_name2));
        assertFalse(((AccountPermission)permissions1.get(1)).getValue());

        permissions2 = accountsDao.getAccountPermissions(account_sid2);
        assertTrue(permissions2.size()==3);
        assertTrue(((AccountPermission)permissions2.get(0)).getSid().equals(permission_sid2));
        assertTrue(((AccountPermission)permissions2.get(0)).getName().equals(permission_name2));
        assertTrue(((AccountPermission)permissions2.get(0)).getValue());

        assertTrue(((AccountPermission)permissions2.get(1)).getSid().equals(permission_sid3));
        assertTrue(((AccountPermission)permissions2.get(1)).getName().equals(permission_name3));
        assertTrue(((AccountPermission)permissions2.get(1)).getValue());

        assertTrue(((AccountPermission)permissions2.get(2)).getSid().equals(permission_sid4));
        assertTrue(((AccountPermission)permissions2.get(2)).getName().equals(permission_name4));
        assertFalse(((AccountPermission)permissions2.get(2)).getValue());
    }

    @Test
    public void testAddAccountPermissions(){
        account_sid1 = Sid.generate(Sid.Type.ACCOUNT);
        permissions1 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions1.size()==0);

        permission_sid1 = Sid.generate(Sid.Type.PERMISSION);
        permission_sid2 = Sid.generate(Sid.Type.PERMISSION);
        permission_sid3 = Sid.generate(Sid.Type.PERMISSION);
        //unique name constraint
        AccountPermission permission1 = new AccountPermission(permission_sid1, permission_name1+permission_sid1, true);
        AccountPermission permission2 = new AccountPermission(permission_sid2, permission_name2+permission_sid2, false);
        AccountPermission permission3 = new AccountPermission(permission_sid3, permission_name3+permission_sid3, true);
        permissionsDao.addPermission(permission1);
        permissionsDao.addPermission(permission2);
        permissionsDao.addPermission(permission3);

        permissions1.add(permission1);
        permissions1.add(permission2);
        permissions1.add(permission3);

        accountsDao.addAccountPermissions(account_sid1, permissions1);
        permissions2 = accountsDao.getAccountPermissions(account_sid1);

        assertTrue(permissions1.size()==3);
        System.out.println(((AccountPermission)permissions1.get(0)).getSid()+", "+permission_sid1);
        System.out.println(((AccountPermission)permissions1.get(0)).getName()+", "+permission_name1+permission_sid1);
        System.out.println(((AccountPermission)permissions1.get(1)).getSid()+", "+permission_sid2);
        System.out.println(((AccountPermission)permissions1.get(1)).getName()+", "+permission_name2+permission_sid2);
        System.out.println(((AccountPermission)permissions1.get(2)).getSid()+", "+permission_sid3);
        System.out.println(((AccountPermission)permissions1.get(2)).getName()+", "+permission_name3+permission_sid3);

        Collections.sort(permissions2);//not really needed

        assertTrue(((AccountPermission)permissions1.get(0)).getSid().equals(permission_sid1));
        assertTrue(((AccountPermission)permissions1.get(0)).getName().equals(permission_name1+permission_sid1));
        assertTrue(((AccountPermission)permissions1.get(0)).getValue());

        assertTrue(((AccountPermission)permissions1.get(1)).getSid().equals(permission_sid2));
        assertTrue(((AccountPermission)permissions1.get(1)).getName().equals(permission_name2+permission_sid2));
        assertFalse(((AccountPermission)permissions1.get(1)).getValue());

        assertTrue(((AccountPermission)permissions1.get(2)).getSid().equals(permission_sid3));
        assertTrue(((AccountPermission)permissions1.get(2)).getName().equals(permission_name3+permission_sid3));
        assertTrue(((AccountPermission)permissions1.get(2)).getValue());

    }

    @Test
    public void testUpdateAccountPermissions(){
        //change value 
        permissions1 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions1.size()==2);

        AccountPermission permission1 = (AccountPermission)permissions1.get(0);
        AccountPermission permission2 = (AccountPermission)permissions1.get(1);
        assertTrue(permission1.getValue());
        assertFalse(permission2.getValue());

        //reverse the values
        permission1.setValue(!permission1.getValue());
        permission2.setValue(!permission2.getValue());
        assertFalse(permission1.getValue());
        assertTrue(permission2.getValue());

        //commit changes
        accountsDao.updateAccountPermissions(account_sid1, permission1);
        //check update
        permissions2 = accountsDao.getAccountPermissions(account_sid1);
        AccountPermission permission3 = (AccountPermission)permissions2.get(0);
        AccountPermission permission4 = (AccountPermission)permissions2.get(1);
        assertFalse(permission3.getValue());
        assertFalse(permission4.getValue());
    }

    @Test
    public void testDeleteAccountPermissions(){
        permissions1 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions1.size()==2);

        accountsDao.deleteAccountPermission(account_sid1, permission_sid1);
        accountsDao.deleteAccountPermission(account_sid1, permission_sid3);

        permissions2 = accountsDao.getAccountPermissions(account_sid1);
        permission1 = permissions2.get(0);
        assertTrue(permissions2.size()==1);
        assertTrue(permission1.getSid().equals(permission_sid2));
    }

    @Test
    public void testDeleteAccountPermissionsByName(){
        permissions1 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions1.size()==2);
        accountsDao.deleteAccountPermissionByName(account_sid1, permission_name1);

        permissions2 = accountsDao.getAccountPermissions(account_sid1);
        permission1 = permissions2.get(0);
        assertTrue(permissions2.size()==1);
        assertTrue(permission1.getSid().equals(permission_sid2));
    }

    @Test
    public void testClearAccountPermissions(){
        permissions1 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions1.size()==2);
        accountsDao.clearAccountPermissions(account_sid1);
        permissions2 = accountsDao.getAccountPermissions(account_sid1);
        assertTrue(permissions2.size()==0);
    }
}
