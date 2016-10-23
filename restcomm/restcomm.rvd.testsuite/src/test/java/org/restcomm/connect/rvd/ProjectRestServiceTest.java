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

package org.restcomm.connect.rvd;

import com.google.gson.*;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.*;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
@RunWith(Arquillian.class)
public class ProjectRestServiceTest extends RestServiceTest {

    private final static Logger logger = Logger.getLogger(ProjectRestServiceTest.class);
    private static final String version = Version.getVersion();

    static final String username = "administrator@company.com";
    static final String password = "adminpass";
    private String accountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String accountAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @Test
    public void canRetrieveProjects() {
        Client jersey = getClient(username, password);
        WebResource resource = jersey.resource( getResourceUrl("/services/projects") );
        ClientResponse response = resource.get(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());

        String json = response.getEntity(String.class);
        JsonParser parser = new JsonParser();
        JsonArray array = parser.parse(json).getAsJsonArray();
        Assert.assertTrue("Invalid number of project returned", array.size() >= 3);
    }

    @Test
    public void createProject() {
        // create application stub
        stubFor(post(urlMatching("/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Applications.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sid\":\"AP03d28db981ee4aa0888ebebd35b4dd4f\",\"friendly_name\":\"newapplication\"}")));
        // retrieve project created
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Applications/AP03d28db981ee4aa0888ebebd35b4dd4f.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sid\":\"AP03d28db981ee4aa0888ebebd35b4dd4f\",\"friendly_name\":\"newapplication\"}")));
        // update project
        stubFor(post(urlMatching("/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Applications/AP03d28db981ee4aa0888ebebd35b4dd4f.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"sid\":\"AP03d28db981ee4aa0888ebebd35b4dd4f\",\"friendly_name\":\"newapplication\"}")));

        Client jersey = getClient(username, password);
        WebResource resource = jersey.resource( getResourceUrl("/services/projects/newapplication?kind=voice") );
        ClientResponse response = resource.put(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());

        String json = response.getEntity(String.class);
        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(json).getAsJsonObject();
        Assert.assertEquals("Invalid project friendly name", "newapplication", object.get("name").getAsString());
        Assert.assertEquals("Invalid project sid", "AP03d28db981ee4aa0888ebebd35b4dd4f", object.get("sid").getAsString());
        Assert.assertEquals("Invalid project kind", "voice", object.get("kind").getAsString());
    }

    /*
    @Test
    public void canDeleteProjects() {
        Client jersey = getClient(username, password);
        WebResource resource = jersey.resource( getResourceUrl("/services/projects/newapplication?kind=voice") );
        ClientResponse response = resource.put(ClientResponse.class);
        Assert.assertEquals(200, response.getStatus());

        DELETE /restcomm-rvd/services/projects/AP9c1464152be74baeb20c964ef5844dcc

    }
    */

    @Deployment(name = "ProjectRestServiceTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm-rvd.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect-rvd:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);

        archive.addAsWebInfResource("restcomm.xml", "restcomm.xml");
        archive.delete("/WEB-INF/rvd.xml");
        archive.addAsWebInfResource("rvd.xml", "rvd.xml");
        //StringAsset rvdxml = "<rvd><workspaceLocation>workspace</workspaceLocation><workspaceBackupLocation></workspaceBackupLocation><restcommBaseUrl>" +  </restcommBaseUrl></rvd>";


        logger.info("Packaged Test App");
        return archive;
    }
}
