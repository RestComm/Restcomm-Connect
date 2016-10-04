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
import org.mobicents.servlet.restcomm.rvd.identity.UserIdentityContext;

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

    /*
    @PostConstruct
    public void init() {
        super.init();
        rvdContext = new RvdContext(request, servletContext);
        rvdSettings = rvdContext.getSettings();
        marshaler = rvdContext.getMarshaler();
        workspaceStorage = new WorkspaceStorage(rvdSettings.getWorkspaceBasePath(), marshaler);
        projectService = new ProjectService(rvdContext, workspaceStorage);
    }
    */

    @PostConstruct
    public void init() {
        super.init(); // setup userIdentityContext
    }

    // used for testing
    NotificationsRestService(UserIdentityContext userIdentityContext) {
        super(userIdentityContext);
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response postNotification(final MultivaluedMap<String, String> data) {
        secure();
        logger.info("received notification");
        try {
            NotificationType type = NotificationType.valueOf(data.getFirst("type"));
            String applicationSid = null;
            if (type == NotificationType.applicationRemoved) {
                applicationSid = data.getFirst("applicationSid");
            }
            postNotification(type,applicationSid);
        } catch (IllegalArgumentException e) {
            logger.error(e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        catch (Exception e) {
            logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    void postNotification(NotificationType type, String applicationSid) {
        logger.info("asdf");
        return;
    }

}
