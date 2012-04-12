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

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.SmsMessage;
import org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.rcml.From;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.StatusCallback;
import org.mobicents.servlet.sip.restcomm.xml.rcml.To;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SmsTagStrategy extends RcmlTagStrategy {
  private final SmsAggregator smsAggregator;
  
  private PhoneNumber from;
  private PhoneNumber to;
  private String body;
  private URI action;
  private String method;
  private URI statusCallback;
  
  private volatile SmsMessage sms;
	  
  public SmsTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    smsAggregator = services.get(SmsAggregator.class);
  }

  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
	// Send the text message.
	if(body != null) {
	  try {
	    final SmsMessagesDao dao = daos.getSmsMessagesDao();
	    sms = sms(interpreter, context, from, to, body, SmsMessage.Status.QUEUED, SmsMessage.Direction.INCOMING);
	    dao.addSmsMessage(sms);
		smsAggregator.send(from.toString(), to.toString(), body);
		sms = sms.setStatus(SmsMessage.Status.SENDING);
		dao.updateSmsMessage(sms);
		if(action != null) {
		  final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		  parameters.add(new BasicNameValuePair("SmsSid", sms.getSid().toString()));
		  parameters.add(new BasicNameValuePair("SmsStatus", sms.getStatus().toString()));
		  interpreter.loadResource(action, method, parameters);
	      interpreter.redirect();
		}
	  } catch(final Exception exception) {
		interpreter.failed();
		notify(interpreter, context, tag, Notification.ERROR, 12400);
	    throw new TagStrategyException(exception);
	  }
	}
  }
  
  private PhoneNumber getFrom(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    return getPhoneNumber(interpreter, context, tag, From.NAME);
  }
  
  private PhoneNumber getTo(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    return getPhoneNumber(interpreter, context, tag, To.NAME);
  }
  
  private PhoneNumber getPhoneNumber(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag, final String attributeName) {
    final Attribute attribute = tag.getAttribute(attributeName);
    if(attribute != null) {
      final String value = attribute.getValue();
      final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
      try {
        return phoneNumberUtil.parse(value, "US");
      } catch(final NumberParseException exception) {
        notify(interpreter, context, tag, Notification.WARNING, 14102);
      }
    }
    return null;
  }
  
  private URI getStatusCallback(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(StatusCallback.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      return URI.create(value);
    }
    return null;
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
	      final RcmlTag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    from = getFrom(interpreter, context, tag);
    to = getTo(interpreter, context, tag);
    body = tag.getText();
    action = getAction(interpreter, context, tag);
    method = getMethod(interpreter, context, tag);
    statusCallback = getStatusCallback(interpreter, context, tag);
  }
  
  private SmsMessage sms(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final PhoneNumber sender, final PhoneNumber recipient, final String body, final SmsMessage.Status status,
      final SmsMessage.Direction direction) {
    final SmsMessage.Builder builder = SmsMessage.builder();
    final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
    builder.setSid(sid);
    builder.setAccountSid(context.getAccountSid());
    builder.setSender(sender.toString());
    builder.setRecipient(recipient.toString());
    builder.setBody(body);
    builder.setStatus(status);
    builder.setDirection(direction);
    builder.setPrice(new BigDecimal(0.00));
    builder.setApiVersion(context.getApiVersion());
    final StringBuilder buffer = new StringBuilder();
    buffer.append(rootUri).append(context.getApiVersion()).append("/Accounts/");
    buffer.append(context.getAccountSid().toString()).append("/SMS/Messages/");
    buffer.append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    builder.setUri(uri);
    return builder.build();
  }
}
