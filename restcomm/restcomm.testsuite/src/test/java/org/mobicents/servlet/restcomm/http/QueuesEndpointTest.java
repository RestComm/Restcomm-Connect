/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
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
package org.mobicents.servlet.restcomm.http;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.restcomm.entities.Sid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */

@RunWith(Arquillian.class)
public class QueuesEndpointTest {
    private final static Logger logger = Logger.getLogger(QueuesEndpointTest.class.getName());

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private String adminUsername = "administrator@company.com";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private static final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

    @After
    public void after() throws InterruptedException {
        Thread.sleep(1000);
    }
    @Test
    public void testCreateAndGetQueue() throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {
        // Define queue attributes
        String friendlyName, maxSize;
        // Test create queue via POST
        MultivaluedMap<String, String> queueParams = new MultivaluedMapImpl();
        queueParams.add("FriendlyName", friendlyName = "EndpointTestQueue");
        queueParams.add("MaxSize", maxSize = "400");
        JsonObject queueJson = RestcommQueuesTool.getInstance().createQueue(deploymentUrl.toString(), adminAccountSid,
                adminUsername, adminAuthToken, queueParams);
        Sid queueSid = new Sid(queueJson.get("sid").getAsString());

        // Test asserts via GET to a single queue
        queueJson = RestcommQueuesTool.getInstance().getQueue(deploymentUrl.toString(), adminUsername, adminAuthToken,
                adminAccountSid, queueSid.toString());
        logger.info(queueJson.toString());
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        assertTrue(df.parse(queueJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(queueJson.get("date_updated").getAsString()) != null);
        assertTrue(queueJson.get("friendly_name").getAsString().equals(friendlyName));
        assertTrue(queueJson.get("max_size").getAsInt() == Integer.valueOf(maxSize));

        // Test asserts via GET to a queue list
        JsonArray queueListJson = RestcommQueuesTool.getInstance().getQueues(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminAccountSid);
        logger.info(queueListJson.toString());
        queueJson = queueListJson.get(0).getAsJsonObject();
        assertTrue(df.parse(queueJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(queueJson.get("date_updated").getAsString()) != null);
        assertTrue(queueJson.get("friendly_name").getAsString().equals(friendlyName));
        assertTrue(queueJson.get("max_size").getAsInt() == Integer.valueOf(maxSize));
    }

    @Test
    public void testUpdateQueue() throws ParseException, IllegalArgumentException, ClientProtocolException, IOException {

        // Test create queue via POST
        MultivaluedMap<String, String> queueParams = new MultivaluedMapImpl();
        queueParams.add("FriendlyName", "QueueUpdateTest");
        queueParams.add("MaxSize", "400");
        JsonObject queueJson = RestcommQueuesTool.getInstance().createQueue(deploymentUrl.toString(), adminAccountSid,
                adminUsername, adminAuthToken, queueParams);
        Sid queueSid = new Sid(queueJson.get("sid").getAsString());

        // Define new values to the queue attributes (POST test)
        String friendlyName, maxSize;

        MultivaluedMap<String, String> queueParamsUpdate = new MultivaluedMapImpl();
        queueParamsUpdate.add("FriendlyName", friendlyName = "QueueUpdateTest2");
        queueParamsUpdate.add("MaxSize", maxSize = "450");

        // Update queue via POST
        RestcommQueuesTool.getInstance().updateQueue(deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                queueSid.toString(), queueParamsUpdate);

        // Test asserts via GET to a single queue
        queueJson = RestcommQueuesTool.getInstance().getQueue(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminAccountSid, queueSid.toString());

        logger.info(queueJson.toString());
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        assertTrue(df.parse(queueJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(queueJson.get("date_updated").getAsString()) != null);
        assertTrue(queueJson.get("friendly_name").getAsString().equals(friendlyName));
        assertTrue(queueJson.get("sid").getAsString().equals(queueSid.toString()));
        assertTrue(queueJson.get("max_size").getAsInt() == Integer.valueOf(maxSize));

    }
  
    @Test
    public void testDeleteQueue() throws IllegalArgumentException, ClientProtocolException, IOException {
        // Create Queue
        MultivaluedMap<String, String> queueParams = new MultivaluedMapImpl();
        queueParams.add("FriendlyName", "QueueUpdateTest");
        queueParams.add("MaxSize", "400");
        JsonObject queueJson = RestcommQueuesTool.getInstance().createQueue(deploymentUrl.toString(), adminAccountSid,
                adminUsername, adminAuthToken, queueParams);
        Sid queueSid = new Sid(queueJson.get("sid").getAsString());

        // Delete queue
        RestcommQueuesTool.getInstance().deleteQueue(deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                queueSid.toString());

        // Check if it was removed
        queueJson = RestcommQueuesTool.getInstance().getQueue(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminAccountSid, queueSid.toString());

        assertTrue(queueJson == null);
    }

    @Deployment(name = "QueuesEndPointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script", "data/hsql/restcomm.script");      logger.info("Packaged Test App");
        return archive;
    }
}
