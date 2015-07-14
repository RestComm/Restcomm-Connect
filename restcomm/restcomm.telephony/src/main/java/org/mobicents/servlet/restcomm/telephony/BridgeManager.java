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

package org.mobicents.servlet.restcomm.telephony;

import org.mobicents.servlet.restcomm.mscontrol.MediaServerControllerFactory;
import org.mobicents.servlet.restcomm.patterns.Observe;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class BridgeManager extends UntypedActor {

    private final MediaServerControllerFactory factory;

    public BridgeManager(final MediaServerControllerFactory factory) {
        super();
        this.factory = factory;
    }

    private ActorRef createBridge() {
        return getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Bridge(factory.provideBridgeController());
            }
        }));
    }

    /*
     * Events
     */
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();

        if (CreateBridge.class.equals(klass)) {
            onCreateBridge((CreateBridge) message, self, sender);
        } else if (BridgeStateChanged.class.equals(klass)) {
            onBridgeStateChanged((BridgeStateChanged) message, self, sender);
        }

    }

    private void onCreateBridge(CreateBridge message, ActorRef self, ActorRef sender) {
        // Create a new bridge
        ActorRef bridge = createBridge();

        // Observe state changes in the bridge for termination purposes
        bridge.tell(new Observe(self), self);

        // Send bridge to remote actor
        final BridgeManagerResponse response = new BridgeManagerResponse(bridge);
        sender.tell(response, self);
    }

    private void onBridgeStateChanged(BridgeStateChanged message, ActorRef self, ActorRef sender) {
        switch (message.getState()) {
            case INACTIVE:
            case FAILED:
                context().stop(sender);
                break;

            default:
                // Do not care about other states
                break;
        }
    }

}
