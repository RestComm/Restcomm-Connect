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

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.TranscriptionsDao;
import org.mobicents.servlet.sip.restcomm.entities.Transcription;
import org.mobicents.servlet.sip.restcomm.http.converter.TranscriptionConverter;

import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/Transcriptions")
@ThreadSafe public final class TranscriptionsEndpoint extends AbstractEndpoint {
  private final TranscriptionsDao dao;
  private final XStream xstream;
  
  public TranscriptionsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getTranscriptionsDao();
    xstream = new XStream();
    xstream.alias("Transcriptions", List.class);
    xstream.alias("Transcription", Transcription.class);
    xstream.registerConverter(new TranscriptionConverter());
  }
  
  @Path("/{sid}")
  @DELETE public Response deleteTranscription(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid) {
    try { secure(new Sid(accountSid), "RestComm:Delete:Transcriptions"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    dao.removeTranscription(new Sid(sid));
    return ok().build();
  }
  
  @Path("/{sid}")
  @GET public Response getTranscription(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid) {
    try { secure(new Sid(accountSid), "RestComm:Read:Transcriptions"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final Transcription transcription = dao.getTranscription(new Sid(sid));
    if(transcription == null) {
      return status(NOT_FOUND).build();
    } else {
      return ok(xstream.toXML(transcription), APPLICATION_XML).build();
    }
  }
  
  @GET public Response getTranscriptions(@PathParam("accountSid") String accountSid) {
    try { secure(new Sid(accountSid), "RestComm:Read:Transcriptions"); }
    catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<Transcription> transcriptions = dao.getTranscriptions(new Sid(accountSid));
    return ok(xstream.toXML(transcriptions), APPLICATION_XML).build();
  }
}
