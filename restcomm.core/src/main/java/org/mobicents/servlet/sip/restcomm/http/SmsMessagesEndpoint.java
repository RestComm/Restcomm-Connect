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
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.math.BigDecimal;
import java.net.URI;
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
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.sip.restcomm.entities.SmsMessage;
import org.mobicents.servlet.sip.restcomm.http.converter.SmsMessageConverter;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/SMS/Messages")
@NotThreadSafe public final class SmsMessagesEndpoint extends AbstractEndpoint {
  private final SmsMessagesDao dao;
  private final SmsAggregator aggregator;
  private final XStream xstream;

  public SmsMessagesEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getSmsMessagesDao();
    aggregator = services.get(SmsAggregator.class);
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
    try {
      validate(data);
      final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
      final String sender = phoneNumberUtil.format(phoneNumberUtil.parse(data.getFirst("From"), "US"), PhoneNumberFormat.E164);
      final String recipient = phoneNumberUtil.format(phoneNumberUtil.parse(data.getFirst("To"), "US"), PhoneNumberFormat.E164);
      final String body = data.getFirst("Body");
      aggregator.send(sender, recipient, body, null);
      dao.addSmsMessage(sms(new Sid(accountSid), getApiVersion(data), sender, recipient, body, SmsMessage.Status.SENT,
          SmsMessage.Direction.OUTBOUND_API));
      return ok().build();
    } catch(final Exception exception) { 
      return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
  }
  
  private SmsMessage sms(final Sid accountSid, final String apiVersion, final String sender, final String recipient, final String body,
      final SmsMessage.Status status, final SmsMessage.Direction direction) {
    final SmsMessage.Builder builder = SmsMessage.builder();
    final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
    builder.setSid(sid);
    builder.setAccountSid(accountSid);
    builder.setSender(sender);
    builder.setRecipient(recipient);
    builder.setBody(body);
    builder.setStatus(status);
    builder.setDirection(direction);
    builder.setPrice(new BigDecimal(0.00));
    builder.setApiVersion(apiVersion);
    final StringBuilder buffer = new StringBuilder();
    buffer.append(apiVersion).append("/Accounts/");
    buffer.append(accountSid.toString()).append("/SMS/Messages/");
    buffer.append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    builder.setUri(uri);
    return builder.build();
  }
  
  private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
    if(!data.containsKey("From")) {
      throw new NullPointerException("From can not be null.");
    } else if(!data.containsKey("To")) {
      throw new NullPointerException("To can not be null.");
    } else if(!data.containsKey("Body")) {
      throw new NullPointerException("Body can not be null.");
    }
  }
}
