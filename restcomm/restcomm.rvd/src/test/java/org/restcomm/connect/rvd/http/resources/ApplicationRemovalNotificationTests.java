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
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.restcomm.connect.rvd.ProjectService;
import org.restcomm.connect.rvd.TestUtils;
import org.restcomm.connect.rvd.exceptions.AuthorizationException;
import org.restcomm.connect.rvd.exceptions.ProjectDoesNotExist;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.model.ModelMarshaler;
import org.restcomm.connect.rvd.model.client.ProjectState;
import org.restcomm.connect.rvd.storage.WorkspaceStorage;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class ApplicationRemovalNotificationTests extends RestServiceMockedTest {

    ProjectService projectService;
    WorkspaceStorage storage;
    UserIdentityContext userIdentityContext;

    @Before
    public void before() throws IOException {
        //addLegitimateAccount("orestis@company.com", "AC1234");
        setupMocks();
        workspaceDir = TestUtils.createTempWorkspace();
        marshaler = new ModelMarshaler();
        storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);
        // create projects in the workspace
        createProject("AP1234","administrator@company.com");
        createProject("AP1235","administrator@company.com");
        projectService = new ProjectService(configuration,storage,marshaler,"/restcomm-rvd");

        addLegitimateAccount("administrator@company.com", "ACae6e420f425248d6a26948c17a9e2acf");
        userIdentityContext = signIn("administrator@company.com", "RestComm");
    }

    @After
    public void after() {
        TestUtils.removeTempWorkspace(workspaceDir.getPath());
    }

    @Test
    public void processApplicationRemovalNotificationTest() throws RvdException {
        File applicationDir = new File(workspaceDir.getPath() + "/AP1234");
        Assert.assertTrue(applicationDir.exists());
        NotificationsRestService endpoint = new NotificationsRestService(userIdentityContext,projectService); // user identity context not needed
        endpoint.processApplicationRemovalNotification("AP1234");
        Assert.assertFalse(applicationDir.exists());
    }


    @Test
    public void applicationRemovalNotificationRest() throws StorageException, ProjectDoesNotExist {
        NotificationsRestService endpoint = new NotificationsRestService(userIdentityContext,projectService);
        MultivaluedMap<String,String> params = new MultivaluedMapImpl();
        params.add("type","applicationRemoved");
        params.add("applicationSid","AP1235");
        Response response = endpoint.postNotification(params);
        Assert.assertEquals(200, response.getStatus());
        // if application is missing, we should get a 404
        params = new MultivaluedMapImpl();
        params.add("type","applicationRemoved");
        params.add("applicationSid","AP0000");
        response = endpoint.postNotification(params);
        Assert.assertEquals(404, response.getStatus());
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void missingApplicationThrowsException() throws RvdException {
        NotificationsRestService endpoint = new NotificationsRestService(null,projectService);
        exception.expect(ProjectDoesNotExist.class);
        endpoint.processApplicationRemovalNotification("AP0000");
    }

    @Test
    public void onlyOwnersAreAllowedToRemoveProjectByNotification() {
        // create a new UserIdentityContext that will use orestis@company.com authorization header
        UserIdentityContext uic = new UserIdentityContext("Basic b3Jlc3Rpc0B0ZWxlc3RheC5jb206YW55cGFzc3dvcmQ=",accountProvider);
        NotificationsRestService endpoint = new NotificationsRestService(uic,projectService);
        MultivaluedMap<String,String> params = new MultivaluedMapImpl();
        params.add("type","applicationRemoved");
        params.add("applicationSid","AP1235");
        // an autohrization exception should be thrown
        exception.expect(AuthorizationException.class); //authorization exception is handled at ExceptionMapper level
        Response response = endpoint.postNotification(params);
    }


}
