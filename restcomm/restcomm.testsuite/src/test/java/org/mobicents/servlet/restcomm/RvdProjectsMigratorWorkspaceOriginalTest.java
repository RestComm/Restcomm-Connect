/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * @author guilherme.jansen@telestax.com
 */
@RunWith(Arquillian.class)
public class RvdProjectsMigratorWorkspaceOriginalTest {

    private final static Logger logger = Logger.getLogger(RvdProjectsMigratorWorkspaceOriginalTest.class);
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private String adminUsername = "administrator@company.com";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private static ArrayList<String> applicationNames;
    private static GreenMail mailServer;

    @BeforeClass
    public static void before() {
        applicationNames = new ArrayList<String>();
        applicationNames.add("rvdCollectVerbDemo");
        applicationNames.add("rvdESDemo");
        applicationNames.add("rvdSayVerbDemo");
    }

    @AfterClass
    public static void after() {
        mailServer.stop();
    }

    @Test
    public void checkApplications() {
        JsonArray applicationsListJson = RestcommRvdProjectsMigratorTool.getInstance().getEntitiesList(
                deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                RestcommRvdProjectsMigratorTool.Endpoint.APPLICATIONS);
        boolean result = true;
        for (String applicationName : applicationNames) {
            boolean current = false;
            for (int i = 0; i < applicationsListJson.size(); i++) {
                JsonObject applicationJson = applicationsListJson.get(i).getAsJsonObject();
                String applicationNameJson = applicationJson.get("friendly_name").getAsString();
                if (applicationName.equals(applicationNameJson)) {
                    current = true;
                    break;
                }
            }
            if (!current) {
                result = false;
                break;
            }
        }
        assertTrue(result);
    }

    @Test
    public void checkIncomingPhoneNumbers() {
        JsonArray incomingPhoneNumbersListJson = RestcommRvdProjectsMigratorTool.getInstance().getEntitiesList(
                deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                RestcommRvdProjectsMigratorTool.Endpoint.INCOMING_PHONE_NUMBERS);
        for (int i = 0; i < incomingPhoneNumbersListJson.size(); i++) {
            JsonObject incomingPhoneNumberJson = incomingPhoneNumbersListJson.get(i).getAsJsonObject();
            assertTrue(incomingPhoneNumberJson.get("voice_url").isJsonNull());
            String applicationSid = incomingPhoneNumberJson.get("voice_application_sid").getAsString();
            JsonObject applicationJson = RestcommRvdProjectsMigratorTool.getInstance().getEntity(deploymentUrl.toString(),
                    adminUsername, adminAuthToken, adminAccountSid, applicationSid,
                    RestcommRvdProjectsMigratorTool.Endpoint.APPLICATIONS);
            assertTrue(!incomingPhoneNumberJson.get("voice_method").isJsonNull());
            assertTrue(!incomingPhoneNumberJson.get("sms_method").isJsonNull());
            assertTrue(!incomingPhoneNumberJson.get("ussd_method").isJsonNull());
            assertTrue(applicationJson != null);
            assertTrue(!applicationJson.isJsonNull());
            assertTrue(applicationJson.get("sid").getAsString().equals(applicationSid));
        }
    }

    @Test
    public void checkClients() {
        JsonArray clientsListJson = RestcommRvdProjectsMigratorTool.getInstance().getEntitiesList(deploymentUrl.toString(),
                adminUsername, adminAuthToken, adminAccountSid, RestcommRvdProjectsMigratorTool.Endpoint.CLIENTS);
        for (int i = 0; i < clientsListJson.size(); i++) {
            JsonObject clientJson = clientsListJson.get(i).getAsJsonObject();
            assertTrue(clientJson.get("voice_url") == null || clientJson.get("voice_url").isJsonNull());
            assertTrue(!clientJson.get("voice_method").isJsonNull());
            assertTrue(clientJson.get("voice_application_sid") == null || clientJson.get("voice_application_sid").isJsonNull());
        }
    }

    @Test
    public void checkNotifications() {
        JsonArray notificationsListJson = RestcommRvdProjectsMigratorTool.getInstance().getEntitiesList(
                deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                RestcommRvdProjectsMigratorTool.Endpoint.NOTIFICATIONS);
        String message = notificationsListJson.toString();
        assertTrue(message.contains("Workspace migration finished with success"));
        assertTrue(message.contains("3 Projects processed"));
        assertTrue(message.contains("3 with success"));
        assertTrue(message.contains("0 with error"));
        assertTrue(message.contains("3 IncomingPhoneNumbers"));
        assertTrue(message.contains("0 Clients"));
    }

    @Test
    public void checkEmail() throws IOException, MessagingException, InterruptedException {
        Thread.sleep(60000);
        MimeMessage[] messages = mailServer.getReceivedMessages();
        assertNotNull(messages);
        assertEquals(1, messages.length);
        MimeMessage m = messages[0];
        assertTrue(String.valueOf(m.getContent()).contains("Workspace migration finished with success"));
        assertTrue(String.valueOf(m.getContent()).contains("3 Projects processed"));
        assertTrue(String.valueOf(m.getContent()).contains("3 with success"));
        assertTrue(String.valueOf(m.getContent()).contains("0 with error"));
        assertTrue(String.valueOf(m.getContent()).contains("3 IncomingPhoneNumbers"));
        assertTrue(String.valueOf(m.getContent()).contains("0 Clients"));
    }

    @Deployment(name = "RvdProjectsMigratorWorkspaceOriginalTest", managed = true, testable = false)
    public static WebArchive createWebArchiveRestcomm() throws Exception {
        startEmailServer();
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm_workspaceMigration.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_projectMigratorWorkspaceOriginalTest", "data/hsql/restcomm.script");
        String source = "src/test/resources/workspace-migration-scenarios/original";
        String target = "workspace-migration";
        File f = new File(source);
        addFiles(archive, f, source, target);
        return archive;
    }

    private static void addFiles(WebArchive war, File dir, String source, String target) throws Exception {
        if (!dir.isDirectory()) {
            throw new Exception("not a directory");
        }
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                String prefix = target != null && !target.isEmpty() ? target : "";
                war.addAsWebResource(f, prefix + f.getPath().replace("\\", "/").substring(source.length()));
            } else {
                addFiles(war, f, source, target);
            }
        }
    }

    private static void startEmailServer() {
        mailServer = new GreenMail(ServerSetupTest.SMTP);
        mailServer.start();
        mailServer.setUser("hascode@localhost", "hascode", "abcdef123");
    }

}
