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
package org.mobicents.servlet.restcomm.telephony.proxy;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class ProxyManagerProxy extends SipServlet {
    private static final long serialVersionUID = 1L;

    private ActorSystem system;
    private ActorRef manager;

    public ProxyManagerProxy() {
        super();
    }

    @Override
    public void destroy() {
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

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        final ServletContext context = config.getServletContext();
        final SipFactory factory = (SipFactory) context.getAttribute(SIP_FACTORY);
        Configuration configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        final String address = configuration.getString("external-ip");
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        system = (ActorSystem) context.getAttribute(ActorSystem.class.getName());
        manager = manager(config, factory, storage, address);
        context.setAttribute(ProxyManager.class.getName(), manager);
    }

    private ActorRef manager(final ServletConfig configuration, final SipFactory factory, final DaoManager storage,
            final String address) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new ProxyManager(configuration, factory, storage, address);
            }
        }));
    }
}
