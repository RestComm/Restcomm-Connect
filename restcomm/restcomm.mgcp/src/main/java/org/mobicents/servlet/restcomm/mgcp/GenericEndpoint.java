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
package org.mobicents.servlet.restcomm.mgcp;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public abstract class GenericEndpoint extends UntypedActor {
    protected final ActorRef gateway;
    protected final MediaSession session;
    protected final NotifiedEntity entity;
    protected EndpointIdentifier id;

    public GenericEndpoint(final ActorRef gateway, final MediaSession session, final NotifiedEntity entity,
            final EndpointIdentifier id) {
        super();
        this.gateway = gateway;
        this.session = session;
        this.entity = entity;
        this.id = id;
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (InviteEndpoint.class.equals(klass)) {
            final EndpointCredentials credentials = new EndpointCredentials(id);
            sender.tell(credentials, self);
        } else if (UpdateEndpointId.class.equals(klass)) {
            final UpdateEndpointId request = (UpdateEndpointId) message;
            id = request.id();
        }
    }
}
