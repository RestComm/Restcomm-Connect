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
package org.mobicents.servlet.restcomm.telephony;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.commons.configuration.Configuration;

import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.mgcp.MediaGateway;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class CallManagerProxy extends SipServlet implements SipApplicationSessionListener {
  private static final long serialVersionUID = 1L;
  
  private ActorSystem system;
  private ActorRef manager;

  public CallManagerProxy() {
    super();
  }
  
  @Override public void destroy() {
    system.shutdown();
    system.awaitTermination();
  }
  
  @Override protected void doRequest(final SipServletRequest request)
      throws ServletException, IOException {
    manager.tell(request, null);
  }

  @Override protected void doResponse(final SipServletResponse response)
      throws ServletException, IOException {
    manager.tell(response, null);
  }
  
  @Override public void init(final ServletConfig config) throws ServletException {
    final ServletContext context = config.getServletContext();
    final Configuration xml = (Configuration)context.getAttribute(Configuration.class.getName());
    final DaoManager storage = (DaoManager)context.getAttribute(DaoManager.class.getName());
    final ActorRef gateway = (ActorRef)context.getAttribute(MediaGateway.class.getName());
    system = (ActorSystem)context.getAttribute(ActorSystem.class.getName());
    // Create the call manager.
    final SipFactory factory = (SipFactory)context.getAttribute(SIP_FACTORY);
    final ActorRef sms = (ActorRef)context.getAttribute("org.mobicents.servlet.restcomm.sms.SmsService");
    manager = manager(xml, gateway, sms, factory, storage);
    context.setAttribute(CallManager.class.getName(), manager);
  }
  
  private ActorRef manager(final Configuration configuration, final ActorRef gateway,
      final ActorRef sms, final SipFactory factory, final DaoManager storage) {
    return system.actorOf(new Props(new UntypedActorFactory() {
		private static final long serialVersionUID = 1L;
		@Override public UntypedActor create() throws Exception {
          return new CallManager(configuration, system, gateway, sms, factory, storage);
		}
    }));
  }

  @Override public void sessionCreated(final SipApplicationSessionEvent event) {
    // Nothing to do.
  }

  @Override public void sessionDestroyed(final SipApplicationSessionEvent event) {
	// Nothing to do.
  }

  @Override public void sessionExpired(final SipApplicationSessionEvent event) {
    manager.tell(event, null);
  }

  @Override public void sessionReadyToInvalidate(final SipApplicationSessionEvent event) {
	// Nothing to do.
  }
}
