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

import com.sun.jersey.core.util.MultivaluedMapImpl;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.rvd.ProjectService;
import org.restcomm.connect.rvd.TestUtils;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.model.ModelMarshaler;
import org.restcomm.connect.rvd.storage.WorkspaceStorage;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class AccountClosingNotificationTests extends RestServiceMockedTest {
    ProjectService projectService;
    WorkspaceStorage storage;

    @Before
    public void before() throws IOException {
        addLegitimateAccount("administrator@company.com", "ACae6e420f425248d6a26948c17a9e2acf");
        //addLegitimateAccount("orestis@company.com", "AC1234");
        setupMocks();
        workspaceDir = TestUtils.createTempWorkspace();
        marshaler = new ModelMarshaler();
        storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);
        // create projects in the workspace
        createProject("APA0001","administrator@company.com");
        createProject("APA0002","administrator@company.com");
        createProject("APA0003","administrator@company.com");
        createProject("APB0001","orestis@company.com");
        createProject("APB0002","orestis@company.com");
        createProject("APB0003","orestis@company.com");

        projectService = new ProjectService(configuration,storage,marshaler,"/restcomm-rvd");
    }

    @After
    public void after() {
        TestUtils.removeTempWorkspace(workspaceDir.getPath());
    }

    @Test
    public void processAccountClosingNotificationTest() {
        addLegitimateAccount("administrator@company.com", "ACae6e420f425248d6a26948c17a9e2acf");
        addMissingAccount("missing@company.com", "AC_MISSING");
        addForbiddenAccount("forbidden@company.com", "AC_FORBIDDEN");
        UserIdentityContext userIdentityContext = signIn("administrator@company.com", "RestComm");

        NotificationsRestService endpoint = new NotificationsRestService(appContext,userIdentityContext,projectService);
        MultivaluedMap<String,String> params = new MultivaluedMapImpl();
        // removing project of valid and logged account should work
        params.add("type", NotificationsRestService.NotificationType.accountClosed.toString());
        params.add("accountSid","ACae6e420f425248d6a26948c17a9e2acf");
        Response response = endpoint.postNotification(params);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertFalse(projectExists("APA0001"));
        Assert.assertFalse(projectExists("APA0002"));
        Assert.assertFalse(projectExists("APA0003"));
        Assert.assertTrue(projectExists("APB0001"));
        // notifications about accounts that do not exist in restcomm should return 400 - BAD_REQUEST. Remember, CLOSED accounts are still there and returned
        params.clear();
        params.add("type", NotificationsRestService.NotificationType.accountClosed.toString());
        params.add("accountSid","AC_MISSING");
        response = endpoint.postNotification(params);
        Assert.assertEquals(400, response.getStatus());
        // notifications about accounts that the logged user doesn't have access to should return 403
        params.clear();
        params.add("type", NotificationsRestService.NotificationType.accountClosed.toString());
        params.add("accountSid","AC_FORBIDDEN");
        response = endpoint.postNotification(params);
        Assert.assertEquals(403, response.getStatus());
    }

}
