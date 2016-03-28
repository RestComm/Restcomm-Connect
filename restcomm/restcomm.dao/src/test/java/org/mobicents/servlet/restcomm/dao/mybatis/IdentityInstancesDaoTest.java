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

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.dao.IdentityInstancesDao;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.IdentityInstance.Status;

import java.io.InputStream;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Orestis Tsakiridis
 */
public class IdentityInstancesDaoTest {
    private static MybatisDaoManager manager;

    String organizationSidForIdentityInstance = "ORxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    public IdentityInstancesDaoTest() {
        super();
    }

    @Before
    public void before() {
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void createAndRetrieveIdentityInstance() {
        final IdentityInstancesDao identityInstances = manager.getIdentityInstancesDao();

        String name = "test-instance";
        Sid organizationSid = Sid.generate(Sid.Type.ORGANIZATION);
        DateTime dateCreated = DateTime.now();
        DateTime dateUpdated = DateTime.now();
        String restcommRestRAT = "rat1";
        DateTime restcommRestLastRegistrationDate = DateTime.now();
        Status restcommRestStatus = Status.success;
        String restcommRestClientSecret = "client-top-secret";
        String restcommUiRAT = "rat2";;
        DateTime restcommUiLastRegistrationDate = DateTime.now();
        Status restcommUiStatus = Status.fail;
        String rvdRestRAT = "rat3";
        DateTime rvdRestLastRegistrationDate = DateTime.now();
        Status rvdRestStatus = Status.success;
        String rvdUiRAT = "rat4";
        DateTime rvdUiLastRegistrationDate = DateTime.now();
        Status rvdUiStatus = Status.success;

        // create and retrieve a blank (only sid is filled) entity
        IdentityInstance iinstance = new IdentityInstance();
        identityInstances.addIdentityInstance(iinstance);
        IdentityInstance iinstance2 = identityInstances.getIdentityInstance(iinstance.getSid());
        assertEquals("identity instance SIDs (for empty records) differ",iinstance.getSid().toString(), iinstance2.getSid().toString());

        // remove and make sure it's not there
        identityInstances.removeIdentityInstance(iinstance.getSid());
        iinstance2 = identityInstances.getIdentityInstance(iinstance.getSid());
        assertNull("identity instance was not removed", iinstance2);

        // create an identity instance in the store
        iinstance = new IdentityInstance();
        iinstance.setName(name);
        iinstance.setOrganizationSid(organizationSid);
        iinstance.setDateCreated(dateCreated);
        iinstance.setDateUpdated(dateUpdated);
        iinstance.setRestcommRestRAT(restcommRestRAT);
        iinstance.setRestcommRestLastRegistrationDate(restcommRestLastRegistrationDate);
        iinstance.setRestcommRestStatus(restcommRestStatus);
        iinstance.setRestcommRestClientSecret(restcommRestClientSecret);

        iinstance.setRestcommUiRAT(restcommUiRAT);
        iinstance.setRestcommUiLastRegistrationDate(restcommUiLastRegistrationDate);
        iinstance.setRestcommUiStatus(restcommUiStatus);

        iinstance.setRvdRestRAT(rvdRestRAT);
        iinstance.setRvdRestLastRegistrationDate(rvdRestLastRegistrationDate);
        iinstance.setRvdRestStatus(rvdRestStatus);

        iinstance.setRvdUiRAT(rvdUiRAT);
        iinstance.setRvdUiLastRegistrationDate(rvdUiLastRegistrationDate);
        iinstance.setRvdUiStatus(rvdUiStatus);

        identityInstances.addIdentityInstance(iinstance);

        // retrieve back the stored identity instance
        iinstance2 = identityInstances.getIdentityInstance(iinstance.getSid());
        assertEquals("identity instance SIDs differ",iinstance.getSid().toString(), iinstance2.getSid().toString());
        assertEquals("identity instance names differ", name, iinstance2.getName());
        assertEquals("organization SIDs differ", organizationSid.toString(), iinstance2.getOrganizationSid().toString());
        assertEquals(dateCreated, iinstance2.getDateCreated());
        assertEquals(dateUpdated, iinstance2.getDateUpdated());
        assertEquals("restcomm-rest RAT differ", restcommRestRAT, iinstance2.getRestcommRestRAT());
        assertEquals("restcomm-rest last registration dates differ", restcommRestLastRegistrationDate, iinstance2.getRestcommRestLastRegistrationDate());
        assertEquals("restcomm-rest statuses differ", restcommRestStatus, iinstance2.getRestcommRestStatus());
        assertEquals("restcomm-rest client secrets differ",restcommRestClientSecret, iinstance2.getRestcommRestClientSecret());

        assertEquals("restcomm-ui RAT differ", restcommUiRAT, iinstance2.getRestcommUiRAT());
        assertEquals("restcomm-ui last registration dates differ", restcommUiLastRegistrationDate, iinstance2.getRestcommUiLastRegistrationDate());
        assertEquals("restcomm-ui statuses differ", restcommUiStatus, iinstance2.getRestcommUiStatus());

        assertEquals("rvd-rest RAT differ", rvdRestRAT, iinstance2.getRvdRestRAT());
        assertEquals("rvd-rest last registration dates differ", rvdRestLastRegistrationDate, iinstance2.getRvdRestLastRegistrationDate());
        assertEquals("rvd-rest statuses differ", rvdRestStatus, iinstance2.getRvdRestStatus());

        assertEquals("rvd-ui RAT differ", rvdUiRAT, iinstance2.getRvdUiRAT());
        assertEquals("rvd-ui last registration dates differ", rvdUiLastRegistrationDate, iinstance2.getRvdUiLastRegistrationDate());
        assertEquals("rvd-ui statuses differ", rvdUiStatus, iinstance2.getRvdUiStatus());
    }

    @Test
    public void retrieveInstanceByOrganizationSid() {
        final IdentityInstancesDao dao = manager.getIdentityInstancesDao();
        IdentityInstance instance = dao.getIdentityInstanceByOrganizationSid(new Sid(organizationSidForIdentityInstance) );
        assertNotNull(instance);
        assertEquals("bound-instance",instance.getName());
    }
}
