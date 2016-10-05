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

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

/**
 * Provides controllers for Mobicents Media Server.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MmsControllerFactory implements MediaServerControllerFactory {

    private final ActorSystem system;
    //private final ActorRef mediaGateway;
    // A list of media gateways.
    //private final List<ActorRef> mediaGateways;
    private final CallControllerFactory callControllerFactory;
    private final ConferenceControllerFactory conferenceControllerFactory;
    private final BridgeControllerFactory bridgeControllerFactory;
    // configurations
    //private final Configuration configuration;
    private final ActorRef mrb;

    public MmsControllerFactory(ActorSystem system, ActorRef mrb, Configuration configuration) {
        super();
        this.system = system;
        //this.mediaGateways = mediaGateways;
        this.callControllerFactory = new CallControllerFactory();
        this.conferenceControllerFactory = new ConferenceControllerFactory();
        this.bridgeControllerFactory = new BridgeControllerFactory();
        //this.configuration = configuration;
        this.mrb = mrb;
    }

    @Override
    public ActorRef provideCallController() {
        return system.actorOf(new Props(this.callControllerFactory));
    }

    @Override
    public ActorRef provideConferenceController() {
        return system.actorOf(new Props(this.conferenceControllerFactory));
    }

    @Override
    public ActorRef provideBridgeController() {
        return system.actorOf(new Props(this.bridgeControllerFactory));
    }

    private final class CallControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -4649683839304615853L;

        @Override
        public Actor create() throws Exception {
            return new MmsCallController(mrb);
        }

    }

    private final class ConferenceControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -919317656354678281L;

        @Override
        public Actor create() throws Exception {
            //return new MmsConferenceController(mediaGateways, configuration);
            return new MmsConferenceController(mrb);
        }

    }

    private final class BridgeControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = 8999152285760508857L;

        @Override
        public Actor create() throws Exception {
            return new MmsBridgeController(mrb);
        }

    }

}
