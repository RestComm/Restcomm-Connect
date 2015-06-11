/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.telephony.ua;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
public final class UserAgentManagerProxy extends SipServlet implements SipServletListener{
    private static final long serialVersionUID = 1L;

    private ActorSystem system;
    private ActorRef manager;
    private ServletContext servletContext;

    private Configuration configuration;

    public UserAgentManagerProxy() {
        super();
    }

    @Override
    public void destroy() {
        if (system != null)
            system.stop(manager);
    }

    @Override
    protected void doRequest(final SipServletRequest request) throws ServletException, IOException {
        manager.tell(request, null);
    }

    @Override
    protected void doResponse(final SipServletResponse response) throws ServletException, IOException {
        manager.tell(response, null);
    }

//    @Override
//    public void init(final ServletConfig config) throws ServletException {
//        configuration.setProperty(ServletConfig.class.getName(), config);
//    }

    private ActorRef manager(final Configuration configuration, final SipFactory factory, final DaoManager storage) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new UserAgentManager(configuration, factory, storage, servletContext);
            }
        }));
    }

    @Override
    public void servletInitialized(SipServletContextEvent event) {
        if (event.getSipServlet().getClass().equals(UserAgentManagerProxy.class)) {
            servletContext = event.getServletContext();
            configuration = (Configuration) servletContext.getAttribute(Configuration.class.getName());
            final SipFactory factory = (SipFactory) servletContext.getAttribute(SIP_FACTORY);
            final DaoManager storage = (DaoManager) servletContext.getAttribute(DaoManager.class.getName());
            system = (ActorSystem) servletContext.getAttribute(ActorSystem.class.getName());
            manager = manager(configuration, factory, storage);
        }
    }
}
