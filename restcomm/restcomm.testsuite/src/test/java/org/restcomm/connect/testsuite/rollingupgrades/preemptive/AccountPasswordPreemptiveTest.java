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

package org.restcomm.connect.testsuite.rollingupgrades.preemptive;

import com.google.gson.JsonObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.EndpointTest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Preemptive tests make sure that old instances in a Restcomm cluster won't break the moment the schema is upgraded.
 * Primarily, they test whether a proposed schema is 'roughly' backwards compatible.
 *
 * To test a schema:
 *
 * 1. Include the hsql restcomm.script that introduces the changes in the schema in the deployment configuration
 * 2. Replace 'version' with the OLD Restcomm binary version at the time of the upgrade. This will be the offended version of restcomm and the one we want to test.
 * 3. Review before() so that the test runs only when needed and not in future restcomm releases. We don't really need it then.
 *
 * In preemptive testing one needs to provide two kinds of tests. Tests for upgraded schema but old data and tests for upgraded
 * schema but new data.
 *
 *
 */
@RunWith(Arquillian.class)
public class AccountPasswordPreemptiveTest extends EndpointTest {
    private static Logger logger = Logger.getLogger(AccountPasswordPreemptiveTest.class);
    // Use the last version before the db schema upgrade is applied. It should not break with the schema upgrade though some operation may not work properly.
    private static final String version = "8.0.0.1102";

    String adminSid = "ACae6e420f425248d6a26948c17a9e2acf";

    @Before
    public void before() {
        // TODO investigate WHEN this test should run
        // only run the test for for old releases. After the upgrade is over and there are no
        // upgrades in the old binary, it makes so sense to loose time running it
        String version = Version.getVersion();
        Assume.assumeTrue(version.startsWith("8.0.0"));
    }

    @Test
    public void workingWithOldData() {
        // Check administrator login. Admin is will have both password and password_algorithm set nul (data is not upgraded yet)
        Client jerseyPassword = this.getClient(adminSid, "RestComm");
        WebResource accountResource = jerseyPassword.resource( getResourceUrl("/2012-04-24/Accounts.json/" + adminSid) );
        ClientResponse response = accountResource.get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
        JsonObject accountJson = parseResponse(response).getAsJsonObject();
        // assert AuthToken is correctly returned
        Assert.assertEquals("77f8c12cc7b8f8423e5c38b035249166",accountJson.get("auth_token").getAsString());
        // typical use of the API - try to retrieve some calls
        WebResource callsResource = jerseyPassword.resource( getResourceUrl("/2012-04-24/Accounts/"+adminSid+"/Calls.json"));
        response = callsResource.get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
        // create a sub-account
        WebResource subAccountsResource = jerseyPassword.resource( getResourceUrl("/2012-04-24/Accounts.json"));
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("EmailAddress", "sub1@company.com");
        params.add("Password", "RestComm12");
        params.add("Role", "Administrator");
        response = subAccountsResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, params);
        Assert.assertEquals(200, response.getStatus());
        // check sub-accounts AuthToken
        JsonObject subAccountJson = parseResponse(response).getAsJsonObject();
        Assert.assertEquals("28f96b0fea1f9e33646f42026abdf305", subAccountJson.get("auth_token").getAsString());
        String subAccountSid = subAccountJson.get("sid").getAsString();
        // login sub-account
        Client jerseySub = this.getClient(subAccountSid, "RestComm12");
        WebResource subAccountResource = jerseySub.resource( getResourceUrl("/2012-04-24/Accounts.json/" + subAccountSid));
        response = subAccountResource.get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void workingWithNewData() {
        // try to login to an upgraded account
        String account1Sid = "AC00000000000000000000000000000000";
        Client jerseyPassword = this.getClient(account1Sid, "RestComm");
        WebResource accountResource = jerseyPassword.resource( getResourceUrl("/2012-04-24/Accounts.json/" + account1Sid) );
        ClientResponse response = accountResource.get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
        JsonObject accountJson = parseResponse(response).getAsJsonObject();
        // change AuthToken
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("EmailAddress", "account1@company.com");
        params.add("Password", "RestComm12");
        response = accountResource.accept(MediaType.APPLICATION_JSON).put(ClientResponse.class, params);
        Assert.assertEquals(200, response.getStatus());
        // try to login again. At this point, password/password_algorithm will be out of sync (outdated) but logins from old instances should work
        jerseyPassword = this.getClient(account1Sid, "RestComm12");
        accountResource = jerseyPassword.resource( getResourceUrl("/2012-04-24/Accounts.json/" + account1Sid) );
        response = accountResource.get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());
    }

    @Deployment(name = "AccountPasswordPreemptiveTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        // We only change the DB file. Preemptive tests need to be as close as possible to the production version of the code.
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("preemptive/restcomm.script_account_password", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }
}

