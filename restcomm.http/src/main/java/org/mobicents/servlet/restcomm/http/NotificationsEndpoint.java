/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

import java.util.List;

import static javax.ws.rs.core.MediaType.*;

import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.shiro.authz.AuthorizationException;

import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.NotificationList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.NotificationConverter;
import org.mobicents.servlet.restcomm.http.converter.NotificationListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class NotificationsEndpoint extends AbstractEndpoint {
  @javax.ws.rs.core.Context 
  private ServletContext context;
  protected final NotificationsDao dao;
  protected final Gson gson;
  protected final XStream xstream;
  
  public NotificationsEndpoint() {
    super();
    final DaoManager storage = (DaoManager)context.getAttribute(DaoManager.class.getName());
    dao = storage.getNotificationsDao();
    final NotificationConverter converter = new NotificationConverter(configuration);
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Notification.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new NotificationListConverter(configuration));
    xstream.registerConverter(new RestCommResponseConverter(configuration));
  }
  
  protected Response getNotification(final String accountSid, final String sid,
      final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Notifications"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Notification notification = dao.getNotification(new Sid(sid));
    if(notification == null) {
      return status(NOT_FOUND).build();
    } else {
      if(APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(notification), APPLICATION_JSON).build();
      } else if(APPLICATION_XML_TYPE == responseType) {
        final RestCommResponse response = new RestCommResponse(notification);
        return ok(xstream.toXML(response), APPLICATION_XML).build();
      } else {
        return null;
      }
    }
  }
  
  protected Response getNotifications(final String accountSid, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:Notifications"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<Notification> notifications = dao.getNotifications(new Sid(accountSid));
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(notifications), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(new NotificationList(notifications));
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else {
      return null;
    }
  }
}
