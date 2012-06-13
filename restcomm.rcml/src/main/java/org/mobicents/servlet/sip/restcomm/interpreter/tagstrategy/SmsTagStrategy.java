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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.entities.SmsMessage;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregatorObserver;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.From;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.To;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class SmsTagStrategy extends RcmlTagStrategy implements SmsAggregatorObserver {
  private static final Logger logger = Logger.getLogger(SmsTagStrategy.class);
  private final PhoneNumberUtil phoneNumberUtil;
  private final SmsAggregator smsAggregator;
  
  private PhoneNumber from;
  private PhoneNumber to;
  private String body;
  private URI action;
  private String method;
  private URI statusCallback;
  
  private volatile SmsMessage sms;
  private RcmlInterpreterContext context;
	  
  public SmsTagStrategy() {
    super();
    phoneNumberUtil = PhoneNumberUtil.getInstance();
    final ServiceLocator services = ServiceLocator.getInstance();
    smsAggregator = services.get(SmsAggregator.class);
  }

  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    this.context = context;
	// Send the text message.
	try {
	  if(Call.Status.IN_PROGRESS == context.getCall().getStatus()) {
	    final SmsMessagesDao dao = daos.getSmsMessagesDao();
	    sms = sms(interpreter, context, from, to, body, SmsMessage.Status.QUEUED, SmsMessage.Direction.INCOMING);
	    dao.addSmsMessage(sms);
	    smsAggregator.send(phoneNumberUtil.format(from, PhoneNumberFormat.E164),
	        phoneNumberUtil.format(to, PhoneNumberFormat.E164), body, this);
	    sms = sms.setStatus(SmsMessage.Status.SENDING);
	    dao.updateSmsMessage(sms);
	    if(action != null) {
	      final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
	      parameters.add(new BasicNameValuePair("SmsSid", sms.getSid().toString()));
	      parameters.add(new BasicNameValuePair("SmsStatus", sms.getStatus().toString()));
	      interpreter.load(action, method, parameters);
	      interpreter.redirect();
        }
	  }
	} catch(final Exception exception) {
	  interpreter.failed();
	  interpreter.notify(context, Notification.ERROR, 12400);
	  logger.error(exception);
	  throw new TagStrategyException(exception);
	}
  }
  
  @Override public void failed() {
    handleSmsMessage(false);
  }
  
  private PhoneNumber getFrom(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final PhoneNumber phoneNumber = getPhoneNumber(interpreter, context, tag, From.NAME);
    if(phoneNumber != null) {
      return phoneNumber;
    } else {
      interpreter.notify(context, Notification.WARNING, 14102);
      return null;
    }
  }
  
  private PhoneNumber getTo(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final PhoneNumber phoneNumber = getPhoneNumber(interpreter, context, tag, To.NAME);
    if(phoneNumber != null) {
      return phoneNumber;
    } else {
      interpreter.notify(context, Notification.WARNING, 14101);
      return null;
    }
  }
  
  private void handleSmsMessage(final boolean success) {
    if(success) {
      sms = sms.setDateSent(DateTime.now());
      sms = sms.setStatus(SmsMessage.Status.SENT);
    } else {
      sms = sms.setStatus(SmsMessage.Status.FAILED);
    }
    final SmsMessagesDao dao = daos.getSmsMessagesDao();
    dao.updateSmsMessage(sms);
    if(statusCallback != null) {
      final List<NameValuePair> variables = context.getRcmlRequestParameters();
	  variables.add(new BasicNameValuePair("SmsSid", sms.getSid().toString()));
	  variables.add(new BasicNameValuePair("SmsStatus", sms.getStatus().toString()));
	  final HttpPost post = new HttpPost(statusCallback);
	  try { post.setEntity(new UrlEncodedFormEntity(variables)); }
	  catch(final UnsupportedEncodingException ignored) { }
	  final HttpClient client = new DefaultHttpClient();
	  try {
	    client.execute(post);
	  } catch(final Exception exception) {
	    logger.error(exception);
	  }
    }
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
	      final RcmlTag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    from = getFrom(interpreter, context, tag);
    to = getTo(interpreter, context, tag);
    initBody(interpreter, context, tag);
    action = getAction(interpreter, context, tag);
    initMethod(interpreter, context, tag);
    statusCallback = getStatusCallback(interpreter, context, tag);
  }
  
  private void initBody(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    body = tag.getText();
    if(body == null || body.isEmpty() || body.length() > SmsMessage.MAX_SIZE) {
      interpreter.notify(context, Notification.WARNING, 14103);
      throw new TagStrategyException("Invalid SMS body length.");
    }
  }
  
  private void initMethod(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    method = getMethod(interpreter, context, tag);
    if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
      interpreter.notify(context, Notification.WARNING, 14104);
      method = "POST";
    }
  }
  
  private SmsMessage sms(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final PhoneNumber sender, final PhoneNumber recipient, final String body, final SmsMessage.Status status,
      final SmsMessage.Direction direction) {
    final SmsMessage.Builder builder = SmsMessage.builder();
    final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
    builder.setSid(sid);
    builder.setAccountSid(context.getAccountSid());
    builder.setSender(phoneNumberUtil.format(sender, PhoneNumberFormat.E164));
    builder.setRecipient(phoneNumberUtil.format(recipient, PhoneNumberFormat.E164));
    builder.setBody(body);
    builder.setStatus(status);
    builder.setDirection(direction);
    builder.setPrice(new BigDecimal(0.00));
    builder.setApiVersion(context.getApiVersion());
    final StringBuilder buffer = new StringBuilder();
    buffer.append(context.getApiVersion()).append("/Accounts/");
    buffer.append(context.getAccountSid().toString()).append("/SMS/Messages/");
    buffer.append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    builder.setUri(uri);
    return builder.build();
  }
  
  @Override public void succeeded() {
    handleSmsMessage(true);
  }
}
