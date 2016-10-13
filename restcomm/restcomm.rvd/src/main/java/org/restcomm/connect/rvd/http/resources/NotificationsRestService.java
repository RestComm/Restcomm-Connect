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

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.ApplicationContext;
import org.restcomm.connect.rvd.ProjectService;
import org.restcomm.connect.rvd.RvdContext;
import org.restcomm.connect.rvd.commons.GenericResponse;
import org.restcomm.connect.rvd.exceptions.AuthorizationException;
import org.restcomm.connect.rvd.exceptions.NotificationProcessingError;
import org.restcomm.connect.rvd.exceptions.ProjectDoesNotExist;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.model.client.ProjectItem;
import org.restcomm.connect.rvd.model.project.RvdProject;
import org.restcomm.connect.rvd.restcomm.RestcommAccountInfo;
import org.restcomm.connect.rvd.storage.WorkspaceStorage;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
@Path("notifications")
public class NotificationsRestService extends SecuredRestService {
    static final Logger logger = Logger.getLogger(NotificationsRestService.class.getName());

    enum NotificationType {
        applicationRemoved,
        accountClosed
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

    // used for testing
    public NotificationsRestService(ApplicationContext applicationContext, UserIdentityContext userIdentityContext, ProjectService projectService) {
        super(applicationContext, userIdentityContext);
        this.projectService = projectService;
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response postNotification(final MultivaluedMap<String, String> data) {
        secure(SecureBehavior.AllowNonActive);
        logger.info("received notification");
        try {
            NotificationType type = NotificationType.valueOf(data.getFirst("type"));
            String applicationSid = null;
            if (type == NotificationType.applicationRemoved) {
                applicationSid = data.getFirst("applicationSid");
                processApplicationRemovalNotification(applicationSid);
            } else
            if (type == NotificationType.accountClosed) {
                String accountSid = data.getFirst("accountSid");
                try {
                    processAccountRemovalNotification(accountSid);
                } catch (NotificationProcessingError e) {
                    logger.error(e);
                    if (e.getType() == NotificationProcessingError.Type.AccountIsMissing)
                        return Response.status(Response.Status.BAD_REQUEST).build(); // the removed account was not found when trying to authorize against restcomm
                    else
                    if (e.getType() == NotificationProcessingError.Type.AccountNotAccessible)
                        return Response.status(Response.Status.FORBIDDEN).build();
                    else {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                }
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

    void processApplicationRemovalNotification(String applicationSid) throws RvdException {
        // check if the operating user has the permission to remove the project (i.e. is the project owner)
        RvdProject project = projectService.load(applicationSid);
        if (! getLoggedUsername().equalsIgnoreCase(project.getState().getHeader().getOwner()))
            throw new AuthorizationException();
        projectService.deleteProject(applicationSid);
    }

    void processAccountRemovalNotification(String removedAccountSid) throws RvdException {
        // check if the operating/logged user has read access to the removed account
        RestcommAccountInfo loggedAccountInfo = getUserIdentityContext().getAccountInfo();
        if ( loggedAccountInfo.getSid().equals(removedAccountSid) ) {
            List<ProjectItem> projects = projectService.getAvailableProjectsByOwner(loggedAccountInfo.getEmail_address());
            for (ProjectItem project : projects) {
                projectService.deleteProject(project.getName());
            }
        } else {
            // logged  account is different than the one that is being removed. Let's check if logged user has
            // read access to removed account. If that's the case, we will remove the apps. Even a closed account will do.
            GenericResponse<RestcommAccountInfo> response = applicationContext.getAccountProvider().getAccount(removedAccountSid, getUserIdentityContext().getEffectiveAuthorizationHeader());
            // we don't care whether this account is closed or not here. We will proceed with application removal
            if (response.succeeded()) {
                String closedAccountEmail = response.get().getEmail_address();
                List<ProjectItem> projects = projectService.getAvailableProjectsByOwner(closedAccountEmail);
                for (ProjectItem project : projects) {
                    projectService.deleteProject(project.getName());
                }
            } else {
                // error retrieving the removed account. Something seems wrong here
                if ( response.getHttpFailureStatus() != null ) {
                    if (404 == response.getHttpFailureStatus()) {
                        throw new NotificationProcessingError("Cannot find removed account '" + removedAccountSid + "'" + ". No projects will be removed", NotificationProcessingError.Type.AccountIsMissing);
                    } else
                    if (403 == response.getHttpFailureStatus()) {
                        throw new NotificationProcessingError("User " + getLoggedUsername() + " can't access account " + removedAccountSid + " and remove its projects", NotificationProcessingError.Type.AccountNotAccessible);
                    }
                }
                throw new NotificationProcessingError("User " + getLoggedUsername() + " failed removing account " + removedAccountSid);
            }
        }
    }
}
