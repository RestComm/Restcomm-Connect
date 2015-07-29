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

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import akka.japi.Creator;
import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;

import org.mobicents.servlet.restcomm.util.Pre23Props;
import org.mobicents.servlet.restcomm.util.RevolvingCounter;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 */
public abstract class AbstractMockMediaGateway extends UntypedActor {
    // Call agent.
    protected NotifiedEntity agent;
    // Media gateway domain name.
    protected final String domain;
    // Timeout period to wait for a response from the media gateway.
    protected final long timeout;
    // Runtime stuff.
    protected RevolvingCounter requestIdPool;
    protected RevolvingCounter sessionIdPool;
    protected RevolvingCounter transactionIdPool;

    public AbstractMockMediaGateway() {
        super();
        agent = new NotifiedEntity("restcomm", "192.168.1.1", 2427);
        domain = "192.168.1.1:2427";
        timeout = 500;
        requestIdPool = new RevolvingCounter(1, Integer.MAX_VALUE);
        sessionIdPool = new RevolvingCounter(1, Integer.MAX_VALUE);
        transactionIdPool = new RevolvingCounter(1, Integer.MAX_VALUE);
    }

    private ActorRef getConnection(final Object message) {
        final CreateConnection request = (CreateConnection) message;
        final ActorRef gateway = self();
        final MediaSession session = request.session();
        return getContext().actorOf(Pre23Props.create(new Creator<Actor>(){
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Connection(gateway, session, agent, timeout);
            }
        }));
    }

    private ActorRef getLink(final Object message) {
        final CreateLink request = (CreateLink) message;
        final ActorRef gateway = self();
        final MediaSession session = request.session();
        return getContext().actorOf(Pre23Props.create(new Creator<Actor>(){
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Link(gateway, session, agent, timeout);
            }
        }));
    }

    private ActorRef getBridgeEndpoint(final Object message) {
        final CreateBridgeEndpoint request = (CreateBridgeEndpoint) message;
        final ActorRef gateway = self();
        final MediaSession session = request.session();
        return getContext().actorOf(Pre23Props.create(new Creator<Actor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return new BridgeEndpoint(gateway, session, agent, domain);
            }
        }));
    }

    private ActorRef getConferenceEndpoint(final Object message) {
        final CreateConferenceEndpoint request = (CreateConferenceEndpoint) message;
        final ActorRef gateway = self();
        final MediaSession session = request.session();
        return getContext().actorOf(Pre23Props.create(new Creator<Actor>(){
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new ConferenceEndpoint(gateway, session, agent, domain);
            }
        }));
    }

    private ActorRef getIvrEndpoint(final Object message) {
        final CreateIvrEndpoint request = (CreateIvrEndpoint) message;
        final ActorRef gateway = self();
        final MediaSession session = request.session();
        return getContext().actorOf(Pre23Props.create(new Creator<Actor>(){
            private static final long serialVersionUID = 1L;

            @Override
            public IvrEndpoint create() throws Exception {
                return new IvrEndpoint(gateway, session, agent, domain);
            }
        }));
    }

    private ActorRef getPacketRelayEndpoint(final Object message) {
        final CreatePacketRelayEndpoint request = (CreatePacketRelayEndpoint) message;
        final ActorRef gateway = self();
        final MediaSession session = request.session();
        return getContext().actorOf(Pre23Props.create(new Creator<Actor>(){
            private static final long serialVersionUID = 1L;

            @Override
            public PacketRelayEndpoint create() throws Exception {
                return new PacketRelayEndpoint(gateway, session, agent, domain);
            }
        }));
    }

    private MediaSession getSession() {
        return new MediaSession((int) sessionIdPool.get());
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (CreateConnection.class.equals(klass)) {
            sender.tell(new MediaGatewayResponse<ActorRef>(getConnection(message)), self);
        } else if (CreateLink.class.equals(klass)) {
            sender.tell(new MediaGatewayResponse<ActorRef>(getLink(message)), self);
        } else if (CreateMediaSession.class.equals(klass)) {
            sender.tell(new MediaGatewayResponse<MediaSession>(getSession()), self);
        } else if (CreateBridgeEndpoint.class.equals(klass)) {
            final ActorRef endpoint = getBridgeEndpoint(message);
            sender.tell(new MediaGatewayResponse<ActorRef>(endpoint), self);
        } else if (CreatePacketRelayEndpoint.class.equals(klass)) {
            final ActorRef endpoint = getPacketRelayEndpoint(message);
            sender.tell(new MediaGatewayResponse<ActorRef>(endpoint), self);
        } else if (CreateIvrEndpoint.class.equals(klass)) {
            final ActorRef endpoint = getIvrEndpoint(message);
            sender.tell(new MediaGatewayResponse<ActorRef>(endpoint), self);
        } else if (CreateConferenceEndpoint.class.equals(klass)) {
            final ActorRef endpoint = getConferenceEndpoint(message);
            sender.tell(new MediaGatewayResponse<ActorRef>(endpoint), self);
        } else if (message instanceof JainMgcpCommandEvent) {
            request(message, sender);
        } else if (message instanceof JainMgcpResponseEvent) {
            response(message, sender);
        }
    }

    private void request(final Object message, final ActorRef sender) {
        final JainMgcpCommandEvent command = (JainMgcpCommandEvent) message;
        final int transactionId = (int) transactionIdPool.get();
        command.setTransactionHandle(transactionId);
        if (NotificationRequest.class.equals(command.getClass())) {
            final NotificationRequest request = (NotificationRequest) command;
            final String id = Long.toString(requestIdPool.get());
            request.getRequestIdentifier().setRequestIdentifier(id);
        }
        event(message, sender);
    }

    private void response(final Object message, final ActorRef sender) {
        event(message, sender);
    }

    protected abstract void event(Object message, final ActorRef sender);
}
