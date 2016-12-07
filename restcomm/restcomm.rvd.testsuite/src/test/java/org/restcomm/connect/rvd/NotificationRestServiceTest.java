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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;

import javax.ws.rs.core.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
@RunWith(Arquillian.class)
public class NotificationRestServiceTest extends RestServiceTest {
    private final static Logger logger = Logger.getLogger(NotificationRestServiceTest.class);
    private static final String version = Version.getVersion();

    @Test
    public void notifyApplicationRemovalWorks() {
        // stup application REST API
        stubFor(get(urlMatching("/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Applications.json"))
            // this returns applications AP0a9168dbd9a7402cbb9c99ac434ed817, AP1745d5278c8543c28e29762ea982653d
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{\"sid\": \"AP0a9168dbd9a7402cbb9c99ac434ed817\",\"date_created\": \"Mon, 21 Mar 2016 15:21:04 +0000\",\"date_updated\": \"Sat, 30 Apr 2016 13:43:43 +0000\",\"friendly_name\": \"sms1\",\"account_sid\": \"AC54b41ed43f543e9ca3c8da489b0c1631\",\"api_version\": \"2012-04-24\",\"voice_caller_id_lookup\": false,\"uri\": \"/restcomm/2012-04-24/Accounts/AC54b41ed43f543e9ca3c8da489b0c1631/Applications/AP0a9168dbd9a7402cbb9c99ac434ed817.json\",\"rcml_url\": \"/restcomm-rvd/services/apps/AP0a9168dbd9a7402cbb9c99ac434ed817/controller\",\"kind\": \"sms\"},{\"sid\": \"AP1745d5278c8543c28e29762ea982653d\",\"date_created\": \"Mon, 14 Nov 2016 08:06:53 +0000\",\"date_updated\": \"Mon, 14 Nov 2016 08:06:53 +0000\",\"friendly_name\": \"orestis_TEST\",\"account_sid\": \"AC54b41ed43f543e9ca3c8da489b0c1631\",\"api_version\": \"2012-04-24\",\"voice_caller_id_lookup\": false,\"uri\": \"/restcomm/2012-04-24/Accounts/AC54b41ed43f543e9ca3c8da489b0c1631/Applications/AP1745d5278c8543c28e29762ea982653d.json\",\"rcml_url\": \"/restcomm-rvd/services/apps/AP1745d5278c8543c28e29762ea982653d/controller\",\"kind\": \"voice\"}]")));

        Client jersey = getClient("administrator@company.com", "RestComm");
        WebResource resource = jersey.resource( getResourceUrl("/services/notifications") );
        String body = "[{\"type\":\"accountClosed\",\"accountSid\":\"ACae6e420f425248d6a26948c17a9e2acf\"}]";
        ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class,body);
        // first time we shoud get a 200
        Assert.assertEquals(200, response.getStatus());
        verify(1, getRequestedFor(urlEqualTo("/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Applications.json")));
        // TODO verify that the project directories have been removed from the filesystem
        // ...
    }

    @Deployment(name = "NotificationsRestServiceTest", managed = true, testable = false)
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
