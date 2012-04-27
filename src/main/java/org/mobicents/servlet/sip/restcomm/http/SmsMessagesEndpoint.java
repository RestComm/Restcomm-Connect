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

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.SmsMessage;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.sip.restcomm.http.converter.SmsMessageConverter;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizer;

import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/SMS/Messages")
@NotThreadSafe public final class SmsMessagesEndpoint extends AbstractEndpoint {
  private final SmsMessagesDao dao;
  private final SpeechSynthesizer synthesizer;
  private final XStream xstream;

  public SmsMessagesEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getSmsMessagesDao();
    synthesizer = services.get(SpeechSynthesizer.class);
    xstream = new XStream();
    xstream.alias("SMSMessages", List.class);
    xstream.alias("SMSMessage", SmsMessage.class);
    xstream.registerConverter(new SmsMessageConverter());
  }
  
  @Path("/{sid}")
  @GET public Response getSmsMessage(@PathParam("accountSid") String accountSid, @PathParam("sid") String sid) {
    try { secure(new Sid(accountSid), "RestComm:Read:SmsMessages"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final SmsMessage smsMessage = dao.getSmsMessage(new Sid(sid));
    if(smsMessage == null) {
      return status(NOT_FOUND).build();
    } else {
      return ok(xstream.toXML(smsMessage), APPLICATION_XML).build();
    }
  }
  
  @GET public Response getSmsMessages(@PathParam("accountSid") String accountSid) {
    try { secure(new Sid(accountSid), "RestComm:Read:SmsMessages"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<SmsMessage> smsMessages = dao.getSmsMessages(new Sid(accountSid));
    return ok(xstream.toXML(smsMessages), APPLICATION_XML).build();
  }
  
  @POST public Response putSmsMessage(@PathParam("accountSid") String accountSid, final MultivaluedMap<String, String> data) {
    try { secure(new Sid(accountSid), "RestComm:Create:SmsMessages"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    // Send SMS Message.
    return null;
  }
}
