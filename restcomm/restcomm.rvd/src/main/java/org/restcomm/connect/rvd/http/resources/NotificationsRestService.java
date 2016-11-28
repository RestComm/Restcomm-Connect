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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.ApplicationContext;
import org.restcomm.connect.rvd.ProjectService;
import org.restcomm.connect.rvd.RvdContext;
import org.restcomm.connect.rvd.exceptions.AccessApiException;
import org.restcomm.connect.rvd.exceptions.AuthorizationException;
import org.restcomm.connect.rvd.exceptions.NotificationProcessingError;
import org.restcomm.connect.rvd.exceptions.ProjectDoesNotExist;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.model.project.RvdProject;
import org.restcomm.connect.rvd.restcomm.RestcommApplicationResponse;
import org.restcomm.connect.rvd.restcomm.RestcommApplicationsResponse;
import org.restcomm.connect.rvd.restcomm.RestcommClient;
import org.restcomm.connect.rvd.storage.WorkspaceStorage;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
@Path("notifications")
public class NotificationsRestService extends SecuredRestService {
    static final Logger logger = Logger.getLogger(NotificationsRestService.class.getName());

    public enum NotificationType {
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
    @Consumes(APPLICATION_JSON)
    public Response postNotifications(@Context HttpServletRequest req) {
        secure();
        logger.info("received notifications");
        // Note that most know errors respond with 200 OK in case a notification is syntactically correct and. An exception
        // is logged though.
        try {
            JsonParser parse = new JsonParser();
            JsonArray notifications;
            try {
                notifications = parse.parse(new InputStreamReader(req.getInputStream(), Charset.forName("UTF-8"))).getAsJsonArray();
            } catch (IOException e) {
                logger.error(e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            for (int i = 0; i< notifications.size(); i++) {
                JsonObject notif = notifications.get(i).getAsJsonObject();
                String type = notif.get("type").getAsString();
                if (NotificationType.accountClosed.toString().equals(type)) {
                    String accountSid = notif.get("accountSid").getAsString();
                    try {
                        processAccountRemovalNotification(accountSid);
                    } catch (NotificationProcessingError e) {
                        // ignore most errors. Technically, the notification was properly received.
                        logger.error(e);
                        if (e.getType() == NotificationProcessingError.Type.AccountIsMissing) {
                            //return Response.status(Response.Status.OK).build(); // the removed account was not found when trying to authorize against restcomm
                            continue;
                        }
                        else
                        if (e.getType() == NotificationProcessingError.Type.AccountNotAccessible) {
                            //return Response.status(Response.Status.OK).build();
                            continue;
                        }
                        else {
                            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                        }
                    }
                } else
                if (NotificationType.applicationRemoved.equals(type)) {
                    String applicationSid = notif.get("applicationSid").getAsString();
                    processApplicationRemovalNotification(applicationSid);
                }
            }

            // TODO refine error handling here
        } catch (ProjectDoesNotExist e) {
            logger.error(e);
            return Response.status(Response.Status.OK).build();
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
        // retrieve the applications belonging to the removed account
        RestcommClient client = new RestcommClient(applicationContext.getConfiguration().getRestcommBaseUri(), getUserIdentityContext().getEffectiveAuthorizationHeader(), applicationContext.getHttpClientBuilder());
        RestcommApplicationsResponse applications = null;
        try {
            applications = client.get("/restcomm/2012-04-24/Accounts/" + removedAccountSid + "/Applications.json").done(new Gson(), RestcommApplicationsResponse.class);
        } catch (AccessApiException e) {
            if (404 == e.getStatusCode()) {
                throw new NotificationProcessingError("Cannot find removed account '" + removedAccountSid + "'" + ". No projects will be removed", NotificationProcessingError.Type.AccountIsMissing);
            } else
            if (403 == e.getStatusCode()) {
                throw new NotificationProcessingError("User " + getLoggedUsername() + " can't access account " + removedAccountSid + " and remove its projects", NotificationProcessingError.Type.AccountNotAccessible);
            } else {
                throw new NotificationProcessingError("User " + getLoggedUsername() + " failed project for account " + removedAccountSid + ". Couldn't fetch application list. " + (e.getStatusCode() != null ? "status: " + e.getStatusCode() : ""  ));
            }
        }
        if (applications != null) {
            for (RestcommApplicationResponse app: applications) {
                try {
                    projectService.deleteProject(app.getSid());
                } catch (ProjectDoesNotExist e) {
                    logger.warn("Project " + app.getSid() + " wasn't removed because it wasn't found.");
                }
            }
        }
    }
}
