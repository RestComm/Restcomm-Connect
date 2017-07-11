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

package org.restcomm.connect.testsuite;

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
import org.restcomm.connect.commons.Version;

/**
 * @author guilherme.jansen@telestax.com
 */
@RunWith(Arquillian.class)
public class RvdProjectsMigratorWorkspaceMixedTest {

    private final static Logger logger = Logger.getLogger(RvdProjectsMigratorWorkspaceMixedTest.class);
    private static final String version = Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private String adminUsername = "administrator@company.com";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String didSid = "PNc2b81d68a221482ea387b6b4e2cbd9d7";
    private String clientSid = "CLa2b99142e111427fbb489c3de357f60a";
    private static ArrayList<String> applicationNames;
    private static ArrayList<String> didSids;
    private static ArrayList<String> clientSids;
    private static GreenMail mailServer;

    @BeforeClass
    public static void before() {
        applicationNames = new ArrayList<String>();
        applicationNames.add("project-1");
        applicationNames.add("project-2");
        applicationNames.add("project-3");
        applicationNames.add("project-4");
        applicationNames.add("rvdCollectVerbDemo");
        applicationNames.add("rvdESDemo");
        applicationNames.add("AP670c33bf0b6748f09eaec97030af36f3");
        didSids = new ArrayList<String>();
        didSids.add("PN46678e5b01d44973bf184f6527bc33f7");
        didSids.add("PN46678e5b01d44973bf184f6527bc33f1");
        didSids.add("PN46678e5b01d44973bf184f6527bc33f2");
        clientSids = new ArrayList<String>();
        clientSids.add("CL3003328d0de04ba68f38de85b732ed56");
        clientSids.add("CL3003328d0de04ba68f38de85b732ed51");
        clientSids.add("CL3003328d0de04ba68f38de85b732ed52");
    }

    @AfterClass
    public static void after() {
        mailServer.stop();
    }

    @Test
    public void checkApplications() {
        JsonArray applicationsListJson = RestcommRvdProjectsMigratorTool.getInstance().getEntitiesList(deploymentUrl.toString(),
                adminUsername, adminAuthToken, adminAccountSid, RestcommRvdProjectsMigratorTool.Endpoint.APPLICATIONS);
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
        // Check those who should be migrated
        JsonArray incomingPhoneNumbersListJson = RestcommRvdProjectsMigratorTool.getInstance().getEntitiesList(
                deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                RestcommRvdProjectsMigratorTool.Endpoint.INCOMING_PHONE_NUMBERS);
        boolean result = true;
        for (String didSid : didSids) {
            boolean current = false;
            for (int i = 0; i < incomingPhoneNumbersListJson.size(); i++) {
                JsonObject incomingPhoneNumberJson = incomingPhoneNumbersListJson.get(i).getAsJsonObject();
                if (incomingPhoneNumberJson.get("sid").getAsString().equals(didSid)) {
                    assertTrue(incomingPhoneNumberJson.get("voice_url").isJsonNull());
                    assertTrue(!incomingPhoneNumberJson.get("voice_method").isJsonNull());
                    assertTrue(!incomingPhoneNumberJson.get("sms_method").isJsonNull());
                    assertTrue(!incomingPhoneNumberJson.get("ussd_method").isJsonNull());
                    String applicationSid = incomingPhoneNumberJson.get("voice_application_sid").getAsString();
                    JsonObject applicationJson = RestcommRvdProjectsMigratorTool.getInstance().getEntity(
                            deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid, applicationSid,
                            RestcommRvdProjectsMigratorTool.Endpoint.APPLICATIONS);
                    assertTrue(applicationJson != null);
                    assertTrue(!applicationJson.isJsonNull());
                    assertTrue(applicationJson.get("sid").getAsString().equals(applicationSid));
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

        // Check the one who should not be touched
        JsonObject incomingPhoneNumberJson = RestcommRvdProjectsMigratorTool.getInstance().getEntity(deploymentUrl.toString(),
                adminUsername, adminAuthToken, adminAccountSid, didSid,
                RestcommRvdProjectsMigratorTool.Endpoint.INCOMING_PHONE_NUMBERS);
        assertTrue(incomingPhoneNumberJson.get("voice_application_sid").isJsonNull());
        assertTrue(incomingPhoneNumberJson.get("voice_url").isJsonNull());
    }

    @Test
    public void checkClients() {
        // Check those who should be migrated
        JsonArray clientsListJson = RestcommRvdProjectsMigratorTool.getInstance().getEntitiesList(deploymentUrl.toString(),
                adminUsername, adminAuthToken, adminAccountSid, RestcommRvdProjectsMigratorTool.Endpoint.CLIENTS);
        boolean result = true;
        for (String clientSid : clientSids) {
            boolean current = false;
            for (int i = 0; i < clientsListJson.size(); i++) {
                JsonObject clientJson = clientsListJson.get(i).getAsJsonObject();
                if (clientJson.get("sid").getAsString().equals(clientSid)) {
                    assertTrue(clientJson.get("voice_url") == null || clientJson.get("voice_url").isJsonNull());
                    assertTrue(!clientJson.get("voice_method").isJsonNull());
                    String applicationSid = clientJson.get("voice_application_sid").getAsString();
                    JsonObject applicationJson = RestcommRvdProjectsMigratorTool.getInstance().getEntity(
                            deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid, applicationSid,
                            RestcommRvdProjectsMigratorTool.Endpoint.APPLICATIONS);
                    assertTrue(applicationJson != null);
                    assertTrue(!applicationJson.isJsonNull());
                    assertTrue(applicationJson.get("sid").getAsString().equals(applicationSid));
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

        // Check the one who should not be touched
        JsonObject clientJson = RestcommRvdProjectsMigratorTool.getInstance().getEntity(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminAccountSid, clientSid, RestcommRvdProjectsMigratorTool.Endpoint.CLIENTS);
        assertTrue(clientJson.get("voice_url") == null || clientJson.get("voice_url").isJsonNull());
        assertTrue(clientJson.get("voice_application_sid") == null || clientJson.get("voice_application_sid").isJsonNull());
    }

    @Test
    public void checkNotifications() {
        JsonArray notificationsListJson = RestcommRvdProjectsMigratorTool.getInstance().getEntitiesList(
                deploymentUrl.toString(), adminUsername, adminAuthToken, adminAccountSid,
                RestcommRvdProjectsMigratorTool.Endpoint.NOTIFICATIONS, "notifications");
        String message = notificationsListJson.toString();
        assertTrue(message.contains("Workspace migration finished with success"));
        assertTrue(message.contains("7 Projects processed"));
        assertTrue(message.contains("7 with success"));
        assertTrue(message.contains("0 with error"));
        assertTrue(message.contains("3 IncomingPhoneNumbers"));
        assertTrue(message.contains("3 Clients"));
    }

    @Test
    public void checkEmail() throws IOException, MessagingException, InterruptedException {
        Thread.sleep(60000);
        MimeMessage[] messages = mailServer.getReceivedMessages();
        assertNotNull(messages);
        assertEquals(1, messages.length);
        MimeMessage m = messages[0];
        assertTrue(String.valueOf(m.getContent()).contains("Workspace migration finished with success"));
        assertTrue(String.valueOf(m.getContent()).contains("7 Projects processed"));
        assertTrue(String.valueOf(m.getContent()).contains("7 with success"));
        assertTrue(String.valueOf(m.getContent()).contains("0 with error"));
        assertTrue(String.valueOf(m.getContent()).contains("3 IncomingPhoneNumbers"));
        assertTrue(String.valueOf(m.getContent()).contains("3 Clients"));
    }

    @Deployment(name = "RvdProjectsMigratorWorkspaceMixedTest", managed = true, testable = false)
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
        archive.addAsWebInfResource("restcomm.script_projectMigratorWorkspaceMixedTest", "data/hsql/restcomm.script");
        String source = "src/test/resources/workspace-migration-scenarios/mixed";
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
