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

package org.restcomm.connect.mscontrol.mms;

import org.apache.log4j.Logger;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

/**
 * Provides controllers for Mobicents Media Server.
 *
 * @author Maria Farooq (maria.farooq@telestax.com)
 *
 */
public class MockFailingMmsControllerFactory implements MediaServerControllerFactory {

    private static Logger logger = Logger.getLogger(MockFailingMmsControllerFactory.class);
    private final ActorSystem system;
    private final CallControllerFactory callControllerFactory;
    private final ConferenceControllerFactory conferenceControllerFactory;
    private final BridgeControllerFactory bridgeControllerFactory;
    private final ActorRef mrb;

    public MockFailingMmsControllerFactory(ActorSystem system, ActorRef mrb) {
        super();
        this.system = system;
        this.callControllerFactory = new CallControllerFactory();
        this.conferenceControllerFactory = new ConferenceControllerFactory();
        this.bridgeControllerFactory = new BridgeControllerFactory();
        this.mrb = mrb;
    }

    @Override
    public ActorRef provideCallController() {
        final Props props = new Props(this.callControllerFactory);
        return system.actorOf(props);
    }

    @Override
    public ActorRef provideConferenceController() {
        final Props props = new Props(this.conferenceControllerFactory);
        return system.actorOf(props);
    }

    @Override
    public ActorRef provideBridgeController() {
        final Props props = new Props(this.bridgeControllerFactory);
        return system.actorOf(props);
    }

    private final class CallControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -4649683839304615853L;

        @Override
        public Actor create() throws Exception {
            return null;
        }

    }

    private final class ConferenceControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -919317656354678281L;

        @Override
        public Actor create() throws Exception {
            return new MockFailingMmsConferenceController(mrb);
        }

    }

    private final class BridgeControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = 8999152285760508857L;

        @Override
        public Actor create() throws Exception {
        	return null;
        }

    }

}
