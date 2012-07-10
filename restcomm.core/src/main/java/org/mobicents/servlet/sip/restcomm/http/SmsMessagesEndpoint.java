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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import com.thoughtworks.xstream.XStream;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;
import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.shiro.authz.AuthorizationException;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.sip.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.sip.restcomm.entities.SmsMessage;
import org.mobicents.servlet.sip.restcomm.entities.SmsMessageList;
import org.mobicents.servlet.sip.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.SmsMessageConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.SmsMessageListConverter;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregatorException;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregatorObserver;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class SmsMessagesEndpoint extends AbstractEndpoint {
  protected final SmsMessagesDao dao;
  protected final Gson gson;
  protected final SmsAggregator aggregator;
  protected final XStream xstream;

  public SmsMessagesEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getSmsMessagesDao();
    aggregator = services.get(SmsAggregator.class);
    final SmsMessageConverter converter = new SmsMessageConverter();
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(SmsMessage.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new SmsMessageListConverter());
    xstream.registerConverter(new RestCommResponseConverter());
  }
  
  protected Response getSmsMessage(final  String accountSid, final String sid,
      final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:SmsMessages"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final SmsMessage smsMessage = dao.getSmsMessage(new Sid(sid));
    if(smsMessage == null) {
      return status(NOT_FOUND).build();
    } else {
      if(APPLICATION_JSON_TYPE == responseType) {
        return ok(gson.toJson(smsMessage), APPLICATION_JSON).build();
      } else if(APPLICATION_XML_TYPE == responseType) {
        final RestCommResponse response = new RestCommResponse(smsMessage);
        return ok(xstream.toXML(response), APPLICATION_XML).build();
      } else {
        return null;
      }
    }
  }
  
  protected Response getSmsMessages(final String accountSid, final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Read:SmsMessages"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    final List<SmsMessage> smsMessages = dao.getSmsMessages(new Sid(accountSid));
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(smsMessages), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(new SmsMessageList(smsMessages));
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else {
      return null;
    }
  }
  
  private void normalize(final MultivaluedMap<String, String> data) throws IllegalArgumentException {
    final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    final String from = data.getFirst("From");
    data.remove("From");
    try {
      data.putSingle("From", phoneNumberUtil.format(phoneNumberUtil.parse(from, "US"), PhoneNumberFormat.E164));
    } catch(final NumberParseException exception) { throw new IllegalArgumentException(exception); }
    final String to = data.getFirst("To");
    data.remove("To");
    try {
      data.putSingle("To", phoneNumberUtil.format(phoneNumberUtil.parse(to, "US"), PhoneNumberFormat.E164));
    } catch(final NumberParseException exception) { throw new IllegalArgumentException(exception); }
    final String body = data.getFirst("Body");
    if(body.getBytes().length > 160) {
      data.remove("Body");
      data.putSingle("Body", body.substring(0, 159));
    }
  }
  
  protected Response putSmsMessage(final String accountSid, final MultivaluedMap<String, String> data,
      final MediaType responseType) {
    try { secure(new Sid(accountSid), "RestComm:Create:SmsMessages"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    try { validate(data); normalize(data); } catch(final RuntimeException exception) { 
      return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
    final String sender = data.getFirst("From");
    final String recipient = data.getFirst("To");
    final String body = data.getFirst("Body");
    final SmsMessage message = sms(new Sid(accountSid), getApiVersion(data), sender, recipient, body, SmsMessage.Status.SENDING,
        SmsMessage.Direction.OUTBOUND_API);
    dao.addSmsMessage(message);
    try {
      aggregator.send(sender, recipient, body, new SmsAggregatorObserver() {
        @Override public void succeeded() {
          final DateTime now = DateTime.now();
	      dao.updateSmsMessage(message.setDateSent(now).setStatus(SmsMessage.Status.SENT));
	    }

	    @Override public void failed() {
	      dao.updateSmsMessage(message.setStatus(SmsMessage.Status.FAILED));
	    }
      });
    } catch(final SmsAggregatorException exception) {
      return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
    }
    if(APPLICATION_JSON_TYPE == responseType) {
      return ok(gson.toJson(message), APPLICATION_JSON).build();
    } else if(APPLICATION_XML_TYPE == responseType) {
      final RestCommResponse response = new RestCommResponse(message);
      return ok(xstream.toXML(response), APPLICATION_XML).build();
    } else {
      return null;
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
  
  private void validate(final MultivaluedMap<String, String> data) throws NullPointerException {
    if(!data.containsKey("From")) {
      throw new NullPointerException("From can not be null.");
    } else if(!data.containsKey("To")) {
      throw new NullPointerException("To can not be null.");
    } else if(!data.containsKey("Body")) {
      throw new NullPointerException("Body can not be null.");
    }
  }
}
