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
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;

import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class ClientsDaoTest {
    private static MybatisDaoManager manager;

    public ClientsDaoTest() {
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
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.CLIENT);
        Sid application = Sid.generate(Sid.Type.APPLICATION);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/hello-world.xml");
        String method = "GET";
        final Client.Builder builder = Client.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setFriendlyName("Alice");
        builder.setLogin("alice");
        builder.setPassword("1234");
        builder.setStatus(Client.ENABLED);
        builder.setVoiceUrl(url);
        builder.setVoiceMethod(method);
        builder.setVoiceFallbackUrl(url);
        builder.setVoiceFallbackMethod(method);
        builder.setVoiceApplicationSid(application);
        builder.setUri(url);
        Client client = builder.build();
        final ClientsDao clients = manager.getClientsDao();
        // Create a new client in the data store.
        clients.addClient(client);
        // Read the client from the data store.
        Client result = clients.getClient(sid);
        // Validate the results.
        assertTrue(result.getSid().equals(client.getSid()));
        assertTrue(result.getAccountSid().equals(client.getAccountSid()));
        assertTrue(result.getApiVersion().equals(client.getApiVersion()));
        assertTrue(result.getFriendlyName().equals(client.getFriendlyName()));
        assertTrue(result.getLogin().equals(client.getLogin()));
        assertTrue(result.getPassword().equals(client.getPassword()));
        assertTrue(result.getStatus() == client.getStatus());
        assertTrue(result.getVoiceUrl().equals(client.getVoiceUrl()));
        assertTrue(result.getVoiceMethod().equals(client.getVoiceMethod()));
        assertTrue(result.getVoiceFallbackUrl().equals(client.getVoiceFallbackUrl()));
        assertTrue(result.getVoiceFallbackMethod().equals(client.getVoiceFallbackMethod()));
        assertTrue(result.getVoiceApplicationSid().equals(client.getVoiceApplicationSid()));
        assertTrue(result.getUri().equals(client.getUri()));
        // Update the client.
        application = Sid.generate(Sid.Type.APPLICATION);
        url = URI.create("http://127.0.0.1:8080/restcomm/demos/world-hello.xml");
        method = "POST";
        client = client.setFriendlyName("Bob");
        client = client.setPassword("4321");
        client = client.setStatus(Client.DISABLED);
        client = client.setVoiceApplicationSid(application);
        client = client.setVoiceUrl(url);
        client = client.setVoiceMethod(method);
        client = client.setVoiceFallbackUrl(url);
        client = client.setVoiceFallbackMethod(method);
        clients.updateClient(client);
        // Read the updated client from the data store.
        result = clients.getClient(sid);
        // Validate the results.
        assertTrue(result.getFriendlyName().equals(client.getFriendlyName()));
        assertTrue(result.getLogin().equals(client.getLogin()));
        assertTrue(result.getPassword().equals(client.getPassword()));
        assertTrue(result.getStatus() == client.getStatus());
        assertTrue(result.getVoiceUrl().equals(client.getVoiceUrl()));
        assertTrue(result.getVoiceMethod().equals(client.getVoiceMethod()));
        assertTrue(result.getVoiceFallbackUrl().equals(client.getVoiceFallbackUrl()));
        assertTrue(result.getVoiceFallbackMethod().equals(client.getVoiceFallbackMethod()));
        assertTrue(result.getVoiceApplicationSid().equals(client.getVoiceApplicationSid()));
        // Delete the client.
        clients.removeClient(sid);
        // Validate that the client was removed.
        assertTrue(clients.getClient(sid) == null);
    }

    @Test
    public void readByUser() {
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.CLIENT);
        Sid application = Sid.generate(Sid.Type.APPLICATION);
        URI url = URI.create("hello-world.xml");
        String method = "GET";
        final Client.Builder builder = Client.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setFriendlyName("Tom");
        builder.setLogin("tom");
        builder.setPassword("1234");
        builder.setStatus(Client.ENABLED);
        builder.setVoiceUrl(url);
        builder.setVoiceMethod(method);
        builder.setVoiceFallbackUrl(url);
        builder.setVoiceFallbackMethod(method);
        builder.setVoiceApplicationSid(application);
        builder.setUri(url);
        final Client client = builder.build();
        final ClientsDao clients = manager.getClientsDao();
        // Create a new client in the data store.
        clients.addClient(client);
        // Read the client from the data store using the user name.
        final Client result = clients.getClient("tom");
        // Validate the result.
        assertTrue(result.getSid().equals(client.getSid()));
        assertTrue(result.getAccountSid().equals(client.getAccountSid()));
        assertTrue(result.getApiVersion().equals(client.getApiVersion()));
        assertTrue(result.getFriendlyName().equals(client.getFriendlyName()));
        assertTrue(result.getLogin().equals(client.getLogin()));
        assertTrue(result.getPassword().equals(client.getPassword()));
        assertTrue(result.getStatus() == client.getStatus());
        assertTrue(result.getVoiceUrl().equals(client.getVoiceUrl()));
        assertTrue(result.getVoiceMethod().equals(client.getVoiceMethod()));
        assertTrue(result.getVoiceFallbackUrl().equals(client.getVoiceFallbackUrl()));
        assertTrue(result.getVoiceFallbackMethod().equals(client.getVoiceFallbackMethod()));
        assertTrue(result.getVoiceApplicationSid().equals(client.getVoiceApplicationSid()));
        assertTrue(result.getUri().equals(client.getUri()));
        // Delete the client.
        clients.removeClient(sid);
        // Validate that the client was removed.
        assertTrue(clients.getClient(sid) == null);
    }

    @Test
    public void readDeleteByAccountSid() {
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.CLIENT);
        Sid application = Sid.generate(Sid.Type.APPLICATION);
        URI url = URI.create("hello-world.xml");
        String method = "GET";
        final Client.Builder builder = Client.builder();
        builder.setSid(sid);
        builder.setAccountSid(account);
        builder.setApiVersion("2012-04-24");
        builder.setFriendlyName("Tom");
        builder.setLogin("tom");
        builder.setPassword("1234");
        builder.setStatus(Client.ENABLED);
        builder.setVoiceUrl(url);
        builder.setVoiceMethod(method);
        builder.setVoiceFallbackUrl(url);
        builder.setVoiceFallbackMethod(method);
        builder.setVoiceApplicationSid(application);
        builder.setUri(url);
        final Client client = builder.build();
        final ClientsDao clients = manager.getClientsDao();
        // Create a new client in the data store.
        clients.addClient(client);
        // Get all the clients for a specific account.
        assertTrue(clients.getClients(account).size() == 1);
        // Remove all the clients for a specific account.
        clients.removeClients(account);
        // Validate that the clients were removed.
        assertTrue(clients.getClients(account).size() == 0);
    }
}
