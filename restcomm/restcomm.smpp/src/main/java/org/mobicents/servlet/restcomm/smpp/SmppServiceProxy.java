/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

package org.mobicents.servlet.restcomm.smpp;

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
import org.mobicents.servlet.restcomm.sms.SmsService;
import org.mobicents.servlet.restcomm.sms.SmsServiceProxy;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

/**
 * @author amit bhayani
 *
 */
public class SmppServiceProxy extends SipServlet implements SipServletListener {
    private static final long serialVersionUID = 1L;

    private ActorSystem system;
    private ActorRef service;

    private ServletContext context;

    public SmppServiceProxy() {
        super();
    }

    @Override
    protected void doRequest(final SipServletRequest request) throws ServletException, IOException {
        service.tell(request, null);
    }

    @Override
    protected void doResponse(final SipServletResponse response) throws ServletException, IOException {
        service.tell(response, null);
    }

    private ActorRef service(final Configuration configuration, final SipFactory factory, final DaoManager storage) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new SmppService(system, configuration, factory, storage, context);
            }
        }));
    }

    @Override
    public void servletInitialized(SipServletContextEvent event) {
        if (event.getSipServlet().getClass().equals(SmsServiceProxy.class)) {
            context = event.getServletContext();
            final SipFactory factory = (SipFactory) context.getAttribute(SIP_FACTORY);
            Configuration configuration = (Configuration) context.getAttribute(Configuration.class.getName());
            // configuration = configuration.subset("sms-aggregator");
            final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
            system = (ActorSystem) context.getAttribute(ActorSystem.class.getName());
            service = service(configuration, factory, storage);
            context.setAttribute(SmsService.class.getName(), service);
        }
    }

}
