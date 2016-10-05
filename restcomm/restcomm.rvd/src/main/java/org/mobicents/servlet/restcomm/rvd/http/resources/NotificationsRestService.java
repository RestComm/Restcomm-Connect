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

package org.mobicents.servlet.restcomm.rvd.http.resources;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.exceptions.AuthorizationException;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.identity.UserIdentityContext;
import org.mobicents.servlet.restcomm.rvd.model.project.RvdProject;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
@Path("notifications")
public class NotificationsRestService extends SecuredRestService {
    static final Logger logger = Logger.getLogger(NotificationsRestService.class.getName());

    enum NotificationType {
        applicationRemoved
    }

    private ProjectService projectService;

    public NotificationsRestService() {
    }

    @PostConstruct
    public void init() {
        super.init();  // setup userIdentityContext
        RvdContext rvdContext = new RvdContext(request, servletContext,applicationContext.getConfiguration());
        WorkspaceStorage storage = new WorkspaceStorage(applicationContext.getConfiguration().getWorkspaceBasePath(), rvdContext.getMarshaler());
        projectService = new ProjectService(rvdContext, storage);
    }

    // used for testing
    NotificationsRestService(UserIdentityContext userIdentityContext, ProjectService projectService) {
        super(userIdentityContext);
        this.projectService = projectService;
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response notifyApplicationRemoved(final MultivaluedMap<String, String> data) {
        secure();
        logger.info("received notification");
        try {
            NotificationType type = NotificationType.valueOf(data.getFirst("type"));
            String applicationSid = null;
            if (type == NotificationType.applicationRemoved) {
                applicationSid = data.getFirst("applicationSid");
                notifyApplicationRemoved(applicationSid);
            }
        } catch (IllegalArgumentException e) {
            logger.error(e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (ProjectDoesNotExist e) {
            logger.error(e);
            return Response.status(Response.Status.NOT_FOUND).build(); // this catch may be a little too generic and we will need to handle per case
        }
        catch (RvdException e) {
            logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    void notifyApplicationRemoved(String applicationSid) throws RvdException {
        // check if the operating user has the permission to remove the project (i.e. is the project owner)
        RvdProject project = projectService.load(applicationSid);
        if (! getLoggedUsername().equalsIgnoreCase(project.getState().getHeader().getOwner()))
            throw new AuthorizationException();
        projectService.deleteProject(applicationSid);
    }

}
