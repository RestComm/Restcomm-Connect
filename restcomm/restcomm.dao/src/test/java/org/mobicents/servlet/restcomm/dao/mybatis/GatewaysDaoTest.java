/*
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

import java.io.InputStream;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.mobicents.servlet.restcomm.dao.GatewaysDao;
import org.mobicents.servlet.restcomm.entities.Gateway;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class GatewaysDaoTest {
    private static MybatisDaoManager manager;

    public GatewaysDaoTest() {
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
    public void createReadUpdateDelete() {
        final Sid sid = Sid.generate(Sid.Type.GATEWAY);
        final URI uri = URI.create("hello-world.xml");
        final Gateway.Builder builder = Gateway.builder();
        builder.setSid(sid);
        builder.setFriendlyName("Service Provider");
        builder.setPassword("1234");
        builder.setProxy("sip:127.0.0.1:5080");
        builder.setRegister(true);
        builder.setUserName("alice");
        builder.setTimeToLive(3600);
        builder.setUri(uri);
        Gateway gateway = builder.build();
        final GatewaysDao gateways = manager.getGatewaysDao();
        // Create a new gateway in the data store.
        gateways.addGateway(gateway);
        // Read the gateway from the data store.
        Gateway result = gateways.getGateway(sid);
        // Validate the results.
        assertTrue(result.getSid().equals(gateway.getSid()));
        assertTrue(result.getFriendlyName().equals(gateway.getFriendlyName()));
        assertTrue(result.getPassword().equals(gateway.getPassword()));
        assertTrue(result.getProxy().equals(gateway.getProxy()));
        assertTrue(result.register() == gateway.register());
        assertTrue(result.getUserName().equals(gateway.getUserName()));
        assertTrue(result.getTimeToLive() == gateway.getTimeToLive());
        assertTrue(result.getUri().equals(gateway.getUri()));
        // Update the gateway.
        gateway = gateway.setFriendlyName("Provider Service");
        gateway = gateway.setPassword("4321");
        gateway = gateway.setProxy("sip:127.0.0.1:5070");
        gateway = gateway.setRegister(false);
        gateway = gateway.setTimeToLive(0);
        gateway = gateway.setUserName("bob");
        gateways.updateGateway(gateway);
        // Read the updated gateway from the data store.
        result = gateways.getGateway(sid);
        // Validate the results.
        assertTrue(result.getSid().equals(gateway.getSid()));
        assertTrue(result.getFriendlyName().equals(gateway.getFriendlyName()));
        assertTrue(result.getPassword().equals(gateway.getPassword()));
        assertTrue(result.getProxy().equals(gateway.getProxy()));
        assertTrue(result.register() == gateway.register());
        assertTrue(result.getUserName().equals(gateway.getUserName()));
        assertTrue(result.getTimeToLive() == gateway.getTimeToLive());
        assertTrue(result.getUri().equals(gateway.getUri()));
        // Delete the gateway.
        gateways.removeGateway(sid);
        // Validate that the client was removed.
        assertTrue(gateways.getGateway(sid) == null);
        assertTrue(gateways.getGateways().size() == 0);
    }
}
