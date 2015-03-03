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

package org.mobicents.servlet.restcomm.mscontrol.xms;

import javax.media.mscontrol.MsControlFactory;

import org.mobicents.servlet.restcomm.mscontrol.MediaServerControllerFactory;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerInfo;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class XmsControllerFactory implements MediaServerControllerFactory {

    // Actor system
    private final ActorSystem system;

    // JSR-309
    private MsControlFactory msControlFactory;

    // Factories
    private final CallControllerFactory callControllerFactory;
    private final ConferenceControllerFactory conferenceControllerFactory;

    // Media Server Info
    private final MediaServerInfo mediaServerInfo;

    public XmsControllerFactory(ActorSystem system, MediaServerInfo mediaServerInfo) {
        // Actor system
        this.system = system;

        // Factories
        this.callControllerFactory = new CallControllerFactory();
        this.conferenceControllerFactory = new ConferenceControllerFactory();

        // Media Server Info
        this.mediaServerInfo = mediaServerInfo;
    }

    public void setMsControlFactory(MsControlFactory factory) {
        this.msControlFactory = factory;
    }

    @Override
    public ActorRef provideCallController() {
        return system.actorOf(new Props(this.callControllerFactory));
    }

    @Override
    public ActorRef provideConferenceController() {
        return system.actorOf(new Props(this.conferenceControllerFactory));
    }

    private final class CallControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = 8689899689896436910L;

        @Override
        public Actor create() throws Exception {
            if (msControlFactory == null) {
                throw new IllegalStateException("No media server control factory has been set.");
            }
            return new XmsCallController(msControlFactory, mediaServerInfo);
        }

    }

    private final class ConferenceControllerFactory implements UntypedActorFactory {

        private static final long serialVersionUID = -4095666710038438897L;

        @Override
        public Actor create() throws Exception {
            if (msControlFactory == null) {
                throw new IllegalStateException("No media server control factory has been set.");
            }
            return new XmsConferenceController(msControlFactory, mediaServerInfo);
        }

    }

}
