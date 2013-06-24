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
package org.mobicents.servlet.restcomm.sms;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import java.io.IOException;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.interpreter.SmsInterpreterBuilder;
import org.mobicents.servlet.restcomm.interpreter.StartInterpreter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SmsService extends UntypedActor {
  private final ActorSystem system;
  private final Configuration configuration;
  private final SipFactory factory;
  private final SipURI transport;
  private final DaoManager storage;
  
  public SmsService(final ActorSystem system, final Configuration configuration,
      final SipFactory factory, final SipURI transport, final DaoManager storage) {
    super();
    this.system = system;
    this.configuration = configuration;
    this.factory = factory;
    this.transport = transport;
    this.storage = storage;
  }
  
  private void message(final Object message) throws IOException {
    final ActorRef self = self();
    final SipServletRequest request = (SipServletRequest)message;
    // Tell the sender we received the message okay.
    final SipServletResponse response = request.createResponse(SipServletResponse.SC_ACCEPTED);
    response.send();
    // Handle the SMS message.
    final SipURI uri = (SipURI)request.getRequestURI();
    final String to = uri.getUser();
    // There is no existing session so create a new one.
    try {
      // Format the destination to an E.164 phone number.
      final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
      final String phone = phoneNumberUtil.format(phoneNumberUtil.parse(to, "US"),
          PhoneNumberFormat.E164);
      // Try to find an application defined for the phone number.
      final IncomingPhoneNumbersDao numbers = storage.getIncomingPhoneNumbersDao();
      final IncomingPhoneNumber number = numbers.getIncomingPhoneNumber(phone);
      if(number != null) {
        final SmsInterpreterBuilder builder = new SmsInterpreterBuilder(system);
        builder.setSmsService(self);
        builder.setConfiguration(configuration);
        builder.setStorage(storage);
        builder.setAccount(number.getAccountSid());
        builder.setVersion(number.getApiVersion());
        final Sid sid = number.getVoiceApplicationSid();
        if(sid != null) {
          final ApplicationsDao applications = storage.getApplicationsDao();
          final Application application = applications.getApplication(sid);
          builder.setUrl(application.getSmsUrl());
          builder.setMethod(application.getSmsMethod());
          builder.setFallbackUrl(application.getSmsFallbackUrl());
          builder.setFallbackMethod(application.getSmsFallbackMethod());
        } else {
          builder.setUrl(number.getSmsUrl());
          builder.setMethod(number.getSmsMethod());
          builder.setFallbackUrl(number.getSmsFallbackUrl());
          builder.setFallbackMethod(number.getSmsFallbackMethod());
        }
        final ActorRef interpreter = builder.build();
        final ActorRef session = session();
        session.tell(request, self);
        final StartInterpreter start = new StartInterpreter(session);
        interpreter.tell(start, self);
      }
    } catch(final NumberParseException ignored) { }
  }
  
  @Override public void onReceive(final Object message) throws Exception {
    final UntypedActorContext context = getContext();
    final Class<?> klass = message.getClass();
    final ActorRef self = self();
    final ActorRef sender = sender();
    if(CreateSmsSession.class.equals(klass)) {
      final ActorRef session = session();
      final SmsServiceResponse<ActorRef> response =
          new SmsServiceResponse<ActorRef>(session);
      sender.tell(response, self);
    } else if(DestroySmsSession.class.equals(klass)) {
      final DestroySmsSession request = (DestroySmsSession)message;
      final ActorRef session = request.session();
      context.stop(session);
    } else if(message instanceof SipServletRequest) {
      final SipServletRequest request = (SipServletRequest)message;
      final String method = request.getMethod();
      if("MESSAGE".equalsIgnoreCase(method)) {
        message(message);
      }
    } else if(message instanceof SipServletResponse) {
      final SipServletResponse response = (SipServletResponse)message;
      final SipServletRequest request = response.getRequest();
      final String method = request.getMethod();
      if("MESSAGE".equalsIgnoreCase(method)) {
        response(message);
      }
    }
  }
  
  private void response(final Object message) {
    final ActorRef self = self();
  	final SipServletResponse response = (SipServletResponse)message;
    final SipApplicationSession application = response.getApplicationSession();
    final ActorRef session = (ActorRef)application.getAttribute(SmsSession.class.getName());
    session.tell(response, self);
  }
  
  private ActorRef session() {
    return system.actorOf(new Props(new UntypedActorFactory() {
		private static final long serialVersionUID = 1L;
		@Override public UntypedActor create() throws Exception {
          return new SmsSession(configuration, factory, transport);
		}
    }));
  }
}
