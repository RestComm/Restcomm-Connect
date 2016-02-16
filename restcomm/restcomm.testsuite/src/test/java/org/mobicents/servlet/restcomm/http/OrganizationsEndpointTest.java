/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.http;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.restcomm.entities.Sid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author guilherme.jansen@telestax.com
 */
@RunWith(Arquillian.class)
public class OrganizationsEndpointTest {

    private final static Logger logger = Logger.getLogger(ApplicationsEndpointTest.class);
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private String adminUsername = "administrator@company.com";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @Test
    public void testCreateAndGetOrganization() throws ParseException {
        // Define organization attributes
        String friendlyName, namespace;

        // Test create organization via POST
        MultivaluedMap<String, String> organizationParams = new MultivaluedMapImpl();
        organizationParams.add("FriendlyName", friendlyName = "Organization Test Inc");
        organizationParams.add("Namespace", namespace = "test");
        JsonObject organizationJson = RestcommOrganizationsTool.getInstance().createOrganization(deploymentUrl.toString(),
                adminAccountSid, adminUsername, adminAuthToken, organizationParams);
        Sid organizationSid = new Sid(organizationJson.get("sid").getAsString());

        // Test asserts via GET to a single organization
        organizationJson = RestcommOrganizationsTool.getInstance().getOrganization(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminAccountSid, organizationSid.toString());

        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        assertTrue(df.parse(organizationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(organizationJson.get("date_updated").getAsString()) != null);
        assertTrue(organizationJson.get("friendly_name").getAsString().equals(friendlyName));
        assertTrue(organizationJson.get("namespace").getAsString().equals(namespace));
        assertTrue(organizationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(organizationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Test asserts via GET to a organization list
        JsonArray organizationsListJson = RestcommOrganizationsTool.getInstance().getOrganizations(deploymentUrl.toString(),
                adminUsername, adminAuthToken, adminAccountSid.toString());
        organizationJson = organizationsListJson.get(0).getAsJsonObject();
        assertTrue(df.parse(organizationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(organizationJson.get("date_updated").getAsString()) != null);
        assertTrue(organizationJson.get("friendly_name").getAsString().equals(friendlyName));
        assertTrue(organizationJson.get("namespace").getAsString().equals(namespace));
        assertTrue(organizationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(organizationJson.get("api_version").getAsString().equals("2012-04-24"));
    }

    @Test
    public void testUpdateOrganization() throws ParseException {
        // Create organization
        MultivaluedMap<String, String> organizationParams = new MultivaluedMapImpl();
        organizationParams.add("FriendlyName", "Organization Test Inc");
        organizationParams.add("Namespace", "test");
        JsonObject organizationJson = RestcommOrganizationsTool.getInstance().createOrganization(deploymentUrl.toString(),
                adminAccountSid, adminUsername, adminAuthToken, organizationParams);
        Sid organizationSid = new Sid(organizationJson.get("sid").getAsString());

        // Define new values to the organization attributes (POST test)
        String friendlyName, namespace;

        MultivaluedMap<String, String> organizationParamsUpdate = new MultivaluedMapImpl();
        organizationParamsUpdate.add("FriendlyName", friendlyName = "Organization Test 2");
        organizationParamsUpdate.add("Namespace", namespace = "test2");

        // Update organization via POST
        RestcommOrganizationsTool.getInstance().updateOrganization(deploymentUrl.toString(), adminUsername, adminAuthToken,
                adminAccountSid, organizationSid.toString(), organizationParamsUpdate, false);

        // Test asserts via GET to a single organization
        organizationJson = RestcommOrganizationsTool.getInstance().getOrganization(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminAccountSid, organizationSid.toString());

        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
        assertTrue(df.parse(organizationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(organizationJson.get("date_updated").getAsString()) != null);
        assertTrue(organizationJson.get("friendly_name").getAsString().equals(friendlyName));
        assertTrue(organizationJson.get("namespace").getAsString().equals(namespace));
        assertTrue(organizationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(organizationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Define new values to the organization attributes (PUT test)
        organizationParamsUpdate = new MultivaluedMapImpl();
        organizationParamsUpdate.add("FriendlyName", friendlyName = "Organization Test 3");
        organizationParamsUpdate.add("Namespace", namespace = "test3");

        // Update application via PUT
        RestcommOrganizationsTool.getInstance().updateOrganization(deploymentUrl.toString(), adminUsername, adminAuthToken,
                adminAccountSid, organizationSid.toString(), organizationParamsUpdate, true);

        // Test asserts via GET to a single organization
        organizationJson = RestcommOrganizationsTool.getInstance().getOrganization(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminAccountSid, organizationSid.toString());

        assertTrue(df.parse(organizationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(organizationJson.get("date_updated").getAsString()) != null);
        assertTrue(organizationJson.get("friendly_name").getAsString().equals(friendlyName));
        assertTrue(organizationJson.get("namespace").getAsString().equals(namespace));
        assertTrue(organizationJson.get("account_sid").getAsString().equals(adminAccountSid));
        assertTrue(organizationJson.get("api_version").getAsString().equals("2012-04-24"));
    }

    @Test
    public void testDeleteOrganization() {
        // Create aplication
        MultivaluedMap<String, String> organizationParams = new MultivaluedMapImpl();
        organizationParams.add("FriendlyName", "Organization");
        organizationParams.add("Namespace", "organization");
        JsonObject organizationJson = RestcommOrganizationsTool.getInstance().createOrganization(deploymentUrl.toString(),
                adminAccountSid, adminUsername, adminAuthToken, organizationParams);
        Sid organizationSid = new Sid(organizationJson.get("sid").getAsString());

        // Delete organization
        RestcommOrganizationsTool.getInstance().deleteOrganization(deploymentUrl.toString(), adminUsername, adminAuthToken,
                adminAccountSid, organizationSid.toString());

        // Check if it was removed
        organizationJson = RestcommOrganizationsTool.getInstance().getOrganization(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminAccountSid, organizationSid.toString());

        assertTrue(organizationJson == null);
    }


    @Deployment(name = "OrganizationsEndpointTest", managed = true, testable = false)
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
        archive.addAsWebInfResource("restcomm.script", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }

}
