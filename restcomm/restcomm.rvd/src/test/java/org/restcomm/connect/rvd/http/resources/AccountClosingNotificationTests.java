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

package org.restcomm.connect.rvd.http.resources;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.restcomm.connect.rvd.ProjectService;
import org.restcomm.connect.rvd.TestUtils;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.model.ModelMarshaler;
import org.restcomm.connect.rvd.storage.WorkspaceStorage;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.Mockito.when;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class AccountClosingNotificationTests extends RestServiceMockedTest {
    ProjectService projectService;
    WorkspaceStorage storage;

    @Before
    public void before() throws IOException {
        addLegitimateAccount("administrator@company.com", "ACA1000000000000000000000000000000");
        setupMocks();
        workspaceDir = TestUtils.createTempWorkspace();
        marshaler = new ModelMarshaler();
        storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);
        // create projects in the workspace
        createProject("APA0000","administrator@company.com");
        createProject("APB0001","sub1@company.com");
        createProject("APB0002","sub2@company.com");
        createProject("APB0003","orestis@company.com");

        projectService = new ProjectService(configuration,storage,marshaler,"/restcomm-rvd");
    }

    @After
    public void after() {
        TestUtils.removeTempWorkspace(workspaceDir.getPath());
    }

    @Test
    public void processAccountClosingNotificationsTest() throws IOException {
        addLegitimateAccount("sub1@company.com","ACA1000000000000000000000000000001");
        addLegitimateAccount("sub2@company.com","ACA1000000000000000000000000000002");
        UserIdentityContext userIdentityContext = signIn("administrator@company.com", "RestComm");
        NotificationsRestService endpoint = new NotificationsRestService(appContext,userIdentityContext,projectService);

        // mock input stream
        final ByteArrayInputStream is = new ByteArrayInputStream("[{\"type\":\"accountClosed\",\"accountSid\":\"ACA1000000000000000000000000000005\"},{\"type\":\"accountClosed\",\"accountSid\":\"ACA1000000000000000000000000000004\"},{\"type\":\"accountClosed\",\"accountSid\":\"ACA1000000000000000000000000000003\"},{\"type\":\"accountClosed\",\"accountSid\":\"ACA1000000000000000000000000000002\"},{\"type\":\"accountClosed\",\"accountSid\":\"ACA1000000000000000000000000000001\"},{\"type\":\"accountClosed\",\"accountSid\":\"ACA1000000000000000000000000000000\"}]".getBytes());
        ServletInputStream sis = new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return is.read();
            }
        };
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getInputStream()).thenReturn(sis);

        Response response = endpoint.postNotifications(request);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertFalse(projectExists("APA0000"));
        Assert.assertFalse(projectExists("APB0001"));
        Assert.assertFalse(projectExists("APB0002"));
        Assert.assertTrue(projectExists("APB0003"));
    }

}
