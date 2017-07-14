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

package org.restcomm.connect.telephony;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;
import org.restcomm.connect.telephony.api.BridgeManagerResponse;
import org.restcomm.connect.telephony.api.BridgeStateChanged;
import org.restcomm.connect.telephony.api.CreateBridge;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class BridgeManager extends RestcommUntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final MediaServerControllerFactory factory;

    public BridgeManager(final MediaServerControllerFactory factory) {
        super();
        this.factory = factory;
    }

    private ActorRef createBridge() {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Bridge(factory);
            }
        });
        return getContext().actorOf(props);
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
