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

    private final static Logger logger = Logger.getLogger(OrganizationsEndpointTest.class);
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private String adminUsername = "administrator@company.com";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String primaryUsername = "primary@company.com";
    private String primaryAccountSid = "AC1ec5d14c34a421c7697e1ce5d4eac782";
    private String primaryAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String defaultOrganization = "ORec3515ebea5243b6bde0444d84b05b80";
    private String foobarOrganization = "ORec3515ebea5243b6bde0444d84b05b81";
    private String organizationAccounts[] = { "primary@company.com", "subaccounta@company.com", "subaccountb@company.com",
            "subaccountc@company.com" };

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
        assertTrue(organizationJson.get("api_version").getAsString().equals("2012-04-24"));

        // Test asserts via GET to a organization list
        JsonArray organizationsListJson = RestcommOrganizationsTool.getInstance().getOrganizations(deploymentUrl.toString(),
                adminUsername, adminAuthToken, adminAccountSid.toString());
        organizationJson = organizationsListJson.get(0).getAsJsonObject();
        assertTrue(df.parse(organizationJson.get("date_created").getAsString()) != null);
        assertTrue(df.parse(organizationJson.get("date_updated").getAsString()) != null);
        assertTrue(organizationJson.get("friendly_name").getAsString().equals(friendlyName));
        assertTrue(organizationJson.get("namespace").getAsString().equals(namespace));
        assertTrue(organizationJson.get("api_version").getAsString().equals("2012-04-24"));
    }

    @Test
    public void testUpdateOrganization() throws ParseException {
        // Create organization
        MultivaluedMap<String, String> organizationParams = new MultivaluedMapImpl();
        organizationParams.add("FriendlyName", "Organization Test Inc");
        organizationParams.add("Namespace", "organization");
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
        assertTrue(organizationJson.get("api_version").getAsString().equals("2012-04-24"));
    }

    @Test
    public void testUpdateMasterAccountOrganization() {
        // Ensure that sub accounts are using Foobar Organization before start the update
        for (int i = 0; i < organizationAccounts.length; i++) {
            final String username = organizationAccounts[i];
            JsonObject accountJson = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), primaryAccountSid,
                    primaryAuthToken, username);
            assertTrue(accountJson.get("organization_sid").getAsString().equals(foobarOrganization));
        }

        // Update master Account's Organization
        RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), primaryAccountSid, primaryAuthToken,
                primaryUsername, primaryAuthToken, primaryAccountSid, null, defaultOrganization);

        // Check if all sub accounts were updated just like the master Account
        for (int i = 0; i < organizationAccounts.length; i++) {
            final String username = organizationAccounts[i];
            JsonObject accountJson = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), primaryAccountSid,
                    primaryAuthToken, username);
            assertTrue(accountJson.get("organization_sid").getAsString().equals(defaultOrganization));
        }

        // Rollback master Account's Organization for next tests
        RestcommAccountsTool.getInstance().updateAccount(deploymentUrl.toString(), primaryAccountSid, primaryAuthToken,
                primaryUsername, primaryAuthToken, primaryAccountSid, null, foobarOrganization);

        // Check if all sub accounts were updated just like the master Account
        for (int i = 0; i < organizationAccounts.length; i++) {
            final String username = organizationAccounts[i];
            JsonObject accountJson = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), primaryAccountSid,
                    primaryAuthToken, username);
            assertTrue(accountJson.get("organization_sid").getAsString().equals(foobarOrganization));
        }
    }

    @Test
    public void testDeleteOrganization() {
        // Create Organization
        MultivaluedMap<String, String> organizationParams = new MultivaluedMapImpl();
        organizationParams.add("FriendlyName", "Organization");
        organizationParams.add("Namespace", "organization");
        JsonObject organizationJson = RestcommOrganizationsTool.getInstance().createOrganization(deploymentUrl.toString(),
                adminAccountSid, adminUsername, adminAuthToken, organizationParams);
        Sid organizationSid = new Sid(organizationJson.get("sid").getAsString());

        // Delete Organization
        RestcommOrganizationsTool.getInstance().deleteOrganization(deploymentUrl.toString(), adminUsername, adminAuthToken,
                adminAccountSid, organizationSid.toString());

        // Check if it was removed
        organizationJson = RestcommOrganizationsTool.getInstance().getOrganization(deploymentUrl.toString(), adminUsername,
                adminAuthToken, adminAccountSid, organizationSid.toString());

        assertTrue(organizationJson == null);
    }

    @Test
    public void testDeleteOrganizationWithAccounts() {
        // Ensure that sub accounts are using Foobar Organization before delete the Organization
        for (int i = 0; i < organizationAccounts.length; i++) {
            final String username = organizationAccounts[i];
            JsonObject accountJson = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), primaryAccountSid,
                    primaryAuthToken, username);
            assertTrue(accountJson.get("organization_sid").getAsString().equals(foobarOrganization));
        }

        // Delete Organization that still have Accounts
        RestcommOrganizationsTool.getInstance().deleteOrganization(deploymentUrl.toString(), adminUsername, adminAuthToken,
                adminAccountSid, foobarOrganization);

        // Check if it was removed
        JsonObject organizationJson = RestcommOrganizationsTool.getInstance().getOrganization(deploymentUrl.toString(),
                adminUsername, adminAuthToken, adminAccountSid, foobarOrganization);

        assertTrue(organizationJson == null);

        // Check if the Accounts of the Organization were migrated to Default Organization
        for(int i = 0; i < organizationAccounts.length; i++){
            final String username = organizationAccounts[i];
            JsonObject accountJson = RestcommAccountsTool.getInstance().getAccount(deploymentUrl.toString(), primaryAccountSid,
                    primaryAuthToken, username);
            assertTrue(accountJson.get("organization_sid").getAsString().equals(defaultOrganization));
        }
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
        archive.addAsWebInfResource("restcomm.script_organizations", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }

}
