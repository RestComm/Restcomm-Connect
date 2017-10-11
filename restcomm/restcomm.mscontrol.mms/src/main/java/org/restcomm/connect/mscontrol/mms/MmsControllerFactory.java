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

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import org.apache.log4j.Logger;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;

/**
 * Provides controllers for Mobicents Media Server.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MmsControllerFactory implements MediaServerControllerFactory {

    private static Logger logger = Logger.getLogger(MmsControllerFactory.class);
    private final CallControllerFactory callControllerFactory;
    private final ConferenceControllerFactory conferenceControllerFactory;
    private final BridgeControllerFactory bridgeControllerFactory;
    private final ActorRef mrb;

    public MmsControllerFactory(ActorRef mrb) {
        super();
        this.callControllerFactory = new CallControllerFactory();
        this.conferenceControllerFactory = new ConferenceControllerFactory();
        this.bridgeControllerFactory = new BridgeControllerFactory();
        this.mrb = mrb;
    }

    @Override
    public Props provideCallControllerProps() {
        return new Props(this.callControllerFactory);
    }

    @Override
    public Props provideConferenceControllerProps() {
        return new Props(this.conferenceControllerFactory);
    }

    @Override
    public Props provideBridgeControllerProps() {
        return new Props(this.bridgeControllerFactory);
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
