/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.rvd.restcomm;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.servlet.restcomm.rvd.TestUtils;
import org.mobicents.servlet.restcomm.rvd.model.UserProfile;
import org.mobicents.servlet.restcomm.rvd.model.WorkspaceSettings;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Orestis Tsakiridis
 */
public class RestcommClientTest {

    private static WorkspaceSettings workspaceSettings;
    private static UserProfile userProfile;
    private static URI fallbackUri;

    @BeforeClass
    public static void init() {
        workspaceSettings = new WorkspaceSettings();
        workspaceSettings.setApiServerHost("127.0.0.1");
        workspaceSettings.setApiServerRestPort(8080);

        userProfile = new UserProfile();
        userProfile.setRestcommHost("restcomm.com");
        userProfile.setRestcommPort(9090);
        userProfile.setUsername("admin");
        userProfile.setToken("token");

        fallbackUri = URI.create("http://123.123.123.123:7070");

        TestUtils.initRvdConfiguration();
    }

    @Test
    public void workspaceSettingsUsedIfAllDefined() throws RestcommClient.RestcommClientInitializationException {
        RestcommClient client = new RestcommClient(workspaceSettings, userProfile, fallbackUri);
        // if all sources have been defined, workspaceSettings should be selected
        Assert.assertEquals(workspaceSettings.getApiServerHost(),client.getHost());
        Assert.assertEquals(workspaceSettings.getApiServerRestPort(),client.getPort());
        Assert.assertEquals("admin", client.getUsername());
        Assert.assertEquals("token", client.getPassword());
    }

    @Test
    public void profileUsedIfWorkspaceSettingsAreMissing() throws RestcommClient.RestcommClientInitializationException, URISyntaxException {
        RestcommClient client = new RestcommClient(null, userProfile, fallbackUri);
        // if all sources have been defined, workspaceSettings should be selected
        Assert.assertEquals(userProfile.getRestcommHost(),client.getHost());
        Assert.assertEquals(userProfile.getRestcommPort(),client.getPort());
        Assert.assertEquals("admin", client.getUsername());
        Assert.assertEquals("token", client.getPassword());
        // test for null or empty values in the properties
        WorkspaceSettings workspaceSettings = new WorkspaceSettings();
        workspaceSettings.setApiServerHost("");
        client = new RestcommClient(workspaceSettings, userProfile, fallbackUri);
        Assert.assertEquals(userProfile.getRestcommHost(),client.getHost());
        Assert.assertEquals(userProfile.getRestcommPort(),client.getPort());
    }

    @Test(expected=RestcommClient.RestcommClientInitializationException.class)
    public void exceptionThrownWhenNoCredentialsCanBeDetermined() throws RestcommClient.RestcommClientInitializationException, URISyntaxException {
        UserProfile profile = new UserProfile();
        profile.setRestcommHost("");
        profile.setRestcommPort(null);
        RestcommClient client = new RestcommClient(null, profile, fallbackUri);
    }

    @Test
    public void fallbackUriIsUsedWhenOthersAreMissing() throws RestcommClient.RestcommClientInitializationException {
        UserProfile profile = new UserProfile();
        profile.setUsername("asdf");
        profile.setToken("");
        profile.setRestcommHost("");
        profile.setRestcommPort(null);
        RestcommClient client = new RestcommClient(null, profile, fallbackUri);
        Assert.assertEquals(fallbackUri.getHost(),client.getHost());
        Assert.assertEquals(new Integer(fallbackUri.getPort()),client.getPort());

        // test for null or empty values in the properties
        WorkspaceSettings workspaceSettings = new WorkspaceSettings();
        workspaceSettings.setApiServerHost("");
        client = new RestcommClient(workspaceSettings, profile,fallbackUri);
        Assert.assertEquals(fallbackUri.getHost(),client.getHost());
        Assert.assertEquals(new Integer(fallbackUri.getPort()),client.getPort());
    }

    @Test
    public void credentialsOverrideWorks() throws RestcommClient.RestcommClientInitializationException {
        RestcommClient client = new RestcommClient(workspaceSettings, userProfile, fallbackUri,"override-user","override-pass");
        // if all sources have been defined, workspaceSettings should be selected
        Assert.assertEquals("override-user", client.getUsername());
        Assert.assertEquals("override-pass", client.getPassword());
    }

}
