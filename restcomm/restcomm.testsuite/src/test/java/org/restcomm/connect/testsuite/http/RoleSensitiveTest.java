/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.connect.testsuite.http;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.annotations.FeatureExpTests;

import javax.ws.rs.client.Client;import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;

/**
 * Selective testing (relies on accounts endpoint only) that involves accounts, roles and permissions.
 *
 * @author Orestis Tsakiridis
 */
@RunWith(Arquillian.class)
public class RoleSensitiveTest extends EndpointTest {
    protected final static Logger logger = Logger.getLogger(RoleSensitiveTest.class);

    static String developerSid = "AC11111111111111111111111111111111";
    static String developerUsername = "developer@company.com";
    static String developerAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    static String userSid = "AC22222222222222222222222222222222";
    static String userUsername = "user@company.com";
    static String userAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    static String powerUserSid = "AC33333333333333333333333333333333";
    static String powerUserUsername = "poweruser@company.com";
    static String powerUserAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    static String guestSid = "AC44444444444444444444444444444444";
    static String guestUsername = "guest@company.com";
    static String guestAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @Test
    @Category(FeatureExpTests.class)
    public void testSinglePermissionAccess() {
        // user can read his account
        Client jersey = getClient(userUsername, userAuthToken);
        WebTarget resource = jersey.target( getResourceUrl("/2012-04-24/Accounts.json/" + userSid) );
        Response response = resource.request().get();
        Assert.assertEquals("user@company.com cannot read his account", 200, response.getStatus());
        // user is refused access when missing permission
        resource = jersey.target( getResourceUrl("/2012-04-24/Accounts/" + userSid + ".json") );
        MultivaluedMap<String, String> applicationParams = new MultivaluedHashMap();
        applicationParams.add("FriendlyName", "Test User UPDATED");
        response = resource.request(MediaType.APPLICATION_JSON).put(Entity.form(applicationParams));
        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    @Category(FeatureExpTests.class)
    public void testSiblingAccountPermissionAccess() {
        // user cannot read sibling account although he has Accounts:Read permission
        Client jersey = getClient(userUsername, userAuthToken);
        WebTarget resource = jersey.target( getResourceUrl("/2012-04-24/Accounts.json/" + developerSid) );
        Response response = resource.request().get();
        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testStarPermission() {
        // power user can read his account
        Client jersey = getClient(powerUserUsername, powerUserAuthToken);
        WebTarget resource = jersey.target( getResourceUrl("/2012-04-24/Accounts.json/" + powerUserSid) );
        Response response = resource.request().get();
        Assert.assertEquals(200, response.getStatus());
        // power user is refused access when missing permission
        resource = jersey.target( getResourceUrl("/2012-04-24/Accounts.json/" + powerUserSid) );
        MultivaluedMap<String, String> applicationParams = new MultivaluedHashMap();
        applicationParams.add("FriendlyName", "Test User UPDATED");
        response = resource.request(MediaType.APPLICATION_JSON).put(Entity.form(applicationParams));
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    @Category(FeatureExpTests.class)
    public void testNoPermissions() {
        // guest cannot read his account
        Client jersey = getClient(guestUsername, guestAuthToken);
        WebTarget resource = jersey.target( getResourceUrl("/2012-04-24/Accounts.json/" + guestSid) );
        Response response = resource.request().get();
        Assert.assertEquals(403, response.getStatus());
    }

    @Deployment(name = "RoleSensitiveTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm_roles_permissions.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_roles_test", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }
}
