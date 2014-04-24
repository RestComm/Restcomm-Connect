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

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.mgcp.MediaGateway;
import org.mobicents.servlet.restcomm.ussd.telephony.UssdCallManager;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class CallManagerProxy extends SipServlet implements SipApplicationSessionListener {
    private static final long serialVersionUID = 1L;

    private ActorSystem system;
    private ActorRef manager;
    private ActorRef ussdManager;

    private Configuration configuration;

    public CallManagerProxy() {
        super();
    }

    @Override
    public void destroy() {
        system.shutdown();
        system.awaitTermination();
    }

    @Override
    protected void doRequest(final SipServletRequest request) throws ServletException, IOException {
        if(isUssdMessage(request)) {
            ussdManager.tell(request, null);
        } else {
            manager.tell(request, null);
        }
    }

    @Override
    protected void doResponse(final SipServletResponse response) throws ServletException, IOException {
        if(isUssdMessage(response)){
            ussdManager.tell(response, null);
        } else {
            manager.tell(response, null);
        }
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        final ServletContext context = config.getServletContext();
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        final ActorRef gateway = (ActorRef) context.getAttribute(MediaGateway.class.getName());
        system = (ActorSystem) context.getAttribute(ActorSystem.class.getName());
        // Create the call manager.
        final SipFactory factory = (SipFactory) context.getAttribute(SIP_FACTORY);
        final ActorRef conferences = conferences(gateway);
        final ActorRef sms = (ActorRef) context.getAttribute("org.mobicents.servlet.restcomm.sms.SmsService");
        manager = manager(configuration, context, gateway, conferences, sms, factory, storage);
        ussdManager = ussdManager(configuration, context, gateway, conferences, sms, factory, storage);
        context.setAttribute(CallManager.class.getName(), manager);
        context.setAttribute(UssdCallManager.class.getName(), ussdManager);
    }

    private ActorRef manager(final Configuration configuration, final ServletContext context, final ActorRef gateway, final ActorRef conferences,
            final ActorRef sms, final SipFactory factory, final DaoManager storage) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new CallManager(configuration, context, system, gateway, conferences, sms, factory, storage);
            }
        }));
    }

    private ActorRef ussdManager(final Configuration configuration, final ServletContext context, final ActorRef gateway, final ActorRef conferences,
            final ActorRef sms, final SipFactory factory, final DaoManager storage) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new UssdCallManager(configuration, context, system, gateway, conferences, sms, factory, storage);
            }
        }));
    }

    private ActorRef conferences(final ActorRef gateway) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new ConferenceCenter(gateway);
            }
        }));
    }

    @Override
    public void sessionCreated(final SipApplicationSessionEvent event) {
        // Nothing to do.
    }

    @Override
    public void sessionDestroyed(final SipApplicationSessionEvent event) {
        // Nothing to do.
    }

    @Override
    public void sessionExpired(final SipApplicationSessionEvent event) {
        manager.tell(event, null);
    }

    @Override
    public void sessionReadyToInvalidate(final SipApplicationSessionEvent event) {
        // Nothing to do.
    }

    private boolean isUssdMessage(SipServletMessage message) {
        Boolean isUssd = false;
        String contentType = null;
        try {
            contentType = message.getContentType();
        } catch (Exception e) {
        }
        if (contentType != null) {
            isUssd = contentType.equalsIgnoreCase("application/vnd.3gpp.ussd+xml");
        } else {
            if (message.getApplicationSession().getAttribute("UssdCall") != null) {
                isUssd = true;
            }
        }
        return isUssd;
    }
}
