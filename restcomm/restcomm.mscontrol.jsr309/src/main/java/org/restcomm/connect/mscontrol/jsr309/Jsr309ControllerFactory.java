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

package org.restcomm.connect.mscontrol.jsr309;

import akka.actor.Actor;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import org.apache.log4j.Logger;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;
import org.restcomm.connect.mscontrol.api.MediaServerInfo;

import javax.media.mscontrol.MsControlFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class Jsr309ControllerFactory implements MediaServerControllerFactory {

    private static Logger logger = Logger.getLogger(Jsr309ControllerFactory.class);

    // JSR-309
    private final MsControlFactory msControlFactory;

    // Factories
    private final CallControllerFactory callControllerFactory;
    private final ConferenceControllerFactory conferenceControllerFactory;
    private final BridgeControllerFactory bridgeControllerFactory;

    // Media Server Info
    private final MediaServerInfo mediaServerInfo;

    public Jsr309ControllerFactory(MediaServerInfo mediaServerInfo, MsControlFactory msControlFactory) {
        // Factories
        this.msControlFactory = msControlFactory;
        this.callControllerFactory = new CallControllerFactory();
        this.conferenceControllerFactory = new ConferenceControllerFactory();
        this.bridgeControllerFactory = new BridgeControllerFactory();

        // Media Server Info
        this.mediaServerInfo = mediaServerInfo;
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

        private static final long serialVersionUID = 8689899689896436910L;

        @Override
        public Actor create() throws Exception {
            if (msControlFactory == null) {
                throw new IllegalStateException("No media server control factory has been set.");
            }
            return new Jsr309CallController(msControlFactory, mediaServerInfo);
        }

    }

    private final class ConferenceControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -4095666710038438897L;

        @Override
        public Actor create() throws Exception {
            if (msControlFactory == null) {
                throw new IllegalStateException("No media server control factory has been set.");
            }
            return new Jsr309ConferenceController(msControlFactory, mediaServerInfo);
        }

    }

    private final class BridgeControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -4095666710038438897L;

        @Override
        public Actor create() throws Exception {
            if (msControlFactory == null) {
                throw new IllegalStateException("No media server control factory has been set.");
            }
            return new Jsr309BridgeController(msControlFactory, mediaServerInfo);
        }

    }

}
