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

package org.mobicents.servlet.restcomm.mscontrol.mgcp;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.mscontrol.MediaSessionController;

import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
@Immutable
public final class MgcpConferenceMediaSessionController extends MediaSessionController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;

    // Finite states
    private final State uninitialized;
    private final State pending;
    private final State active;
    private final State inactive;
    private final State failed;

    // Intermediate states
    private final State acquiringMediaSession;
    private final State acquiringConferenceEndpoint;
    private final State acquiringConnection;
    private final State initializingConnection;
    private final State openingConnection;
    private final State closingConnection;
    
    public MgcpConferenceMediaSessionController() {
        // Finite States
        this.uninitialized = new State("", null, null);
        this.pending = new State("", null, null);
        this.active = new State("", null, null);
        this.inactive = new State("", null, null);
        this.failed = new State("", null, null);
        
        // Intermediate states
        this.acquiringMediaSession = new State("", null, null);
        this.acquiringConferenceEndpoint = new State("", null, null);
        this.acquiringConnection = new State("", null, null);
        this.initializingConnection = new State("", null, null);
        this.openingConnection = new State("", null, null);
        this.closingConnection = new State("", null, null);
    }

    /*
     * EVENTS
     */
    @Override
    public void onReceive(Object message) throws Exception {
        // TODO Auto-generated method stub

    }

    /*
     * ACTIONS
     */

}
