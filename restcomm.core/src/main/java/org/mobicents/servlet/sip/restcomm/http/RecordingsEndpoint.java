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
package org.mobicents.servlet.sip.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.io.File;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.sip.restcomm.entities.Recording;
import org.mobicents.servlet.sip.restcomm.http.converter.RecordingConverter;

import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/Recordings")
@ThreadSafe public final class RecordingsEndpoint extends AbstractEndpoint {
  private final RecordingsDao dao;
  private final XStream xstream;

  public RecordingsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getRecordingsDao();
    xstream = new XStream();
    xstream.alias("Recordings", List.class);
    xstream.alias("Recording", Recording.class);
    xstream.registerConverter(new RecordingConverter());
  }
  
  @Path("/{sid}")
  @GET public Response getRecording(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid) {
    try { secure(new Sid(accountSid), "RestComm:Read:Recordings"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    if(sid.contains(".")) {
      final int indexOfPeriod = sid.indexOf(".");
      final String formattedSid = sid.substring(0, indexOfPeriod);
      final StringBuilder buffer = new StringBuilder();
      buffer.append(baseRecordingsPath).append(formattedSid).append(".wav");
      final File file = new File(buffer.toString());
      if(file.exists()) {
        return ok(file, "audio/wav").build();
      } else {
        return status(NOT_FOUND).build();
      }
    } else {
      final Recording recording = dao.getRecording(new Sid(sid));
      return ok(xstream.toXML(recording), APPLICATION_XML).build();
    }
  }
  
  @GET public Response getRecordings(@PathParam("accountSid") String accountSid) {
    try { secure(new Sid(accountSid), "RestComm:Read:Recordings"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<Recording> recordings = dao.getRecordings(new Sid(accountSid));
    return ok(xstream.toXML(recordings), APPLICATION_XML).build();
  }
}
