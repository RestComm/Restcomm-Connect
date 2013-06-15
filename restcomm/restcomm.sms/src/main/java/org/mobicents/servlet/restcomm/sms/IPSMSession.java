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
import akka.actor.UntypedActor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class IPSMSession extends UntypedActor {
  private final SipFactory factory;
  private final List<ActorRef> observers;
  private final String service;
  private final SipURI transport;

  public IPSMSession(final SipFactory factory, final String service,
      final SipURI transport) {
    super();
    this.factory = factory;
    this.observers = new ArrayList<ActorRef>();
    this.service = service;
    this.transport = transport;
  }
  
  private void inbound(final Object message) throws IOException {
    final SipServletRequest request = (SipServletRequest)message;
    // Handle the SMS.
    SipURI uri = (SipURI)request.getFrom().getURI();
    final String from = uri.getUser();
    uri = (SipURI)request.getTo().getURI();
    final String to = uri.getUser();
    String body = null;
    if(request.getContentLength() > 0) {
      body = new String(request.getRawContent());
    }
    // Notify the observers.
    final ActorRef self = self();
    final SmsEvent event = new SmsEvent(from, to, body);
    for(final ActorRef observer : observers) {
      observer.tell(event, self);
    }
  }
  
  private void observe(final Object message) {
	final ActorRef self = self();
	final Observe request = (Observe)message;
    final ActorRef observer = request.observer();
    if(observer != null) {
      observers.add(observer);
      observer.tell(new Observing(self), self);
    }
  }

  @Override public void onReceive(final Object message) throws Exception {
    final Class<?> klass = message.getClass();
    final ActorRef self = self();
    final ActorRef sender = sender();
    if(Observe.class.equals(klass)) {
      observe(message);
    } else if(StopObserving.class.equals(klass)) {
      stopObserving(message);
    } else if(message instanceof SipServletRequest) {
      final SipServletRequest request = (SipServletRequest)message;
      final String method = request.getMethod();
      if("MESSAGE".equalsIgnoreCase(method)) {
        inbound(message);
      }
    } else if(SendSms.class.equals(klass)) {
      try {
        outbound(message);
        sender.tell(new SmsSessionResponse(), self);
      } catch(final Exception exception) {
        sender.tell(exception, self);
      }
    }
  }
  
  private void outbound(final Object message) throws IOException,
      ServletException {
    final SendSms request = (SendSms)message;
    final String from = request.from();
    final String to = request.to();
    final String body = request.body();
    final SipApplicationSession application = factory.createApplicationSession();
	StringBuilder buffer = new StringBuilder();
	buffer.append("sip:").append(from).append("@").append(transport);
	final String sender = buffer.toString();
	buffer = new StringBuilder();
	buffer.append("sip:").append(to).append("@").append(service);
	final String recipient = buffer.toString();
	final SipServletRequest sms = factory.createRequest(application, "MESSAGE",
        sender, recipient);
	final SipURI uri = (SipURI)factory.createURI(recipient);
	sms.pushRoute(uri);
	sms.setRequestURI(uri);
	sms.setContent(body, "text/plain");
	final SipSession session = sms.getSession();
	session.setHandler("VoipInnovationsSmsService");
    sms.send();
  }
  
  private void stopObserving(final Object message) {
	final StopObserving request = (StopObserving)message;
    final ActorRef observer = request.observer();
    if(observer != null) {
      observers.remove(observer);
    }
  }
}
