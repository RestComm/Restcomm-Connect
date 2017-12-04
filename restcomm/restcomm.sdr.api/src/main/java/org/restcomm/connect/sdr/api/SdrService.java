/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2017, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it andor modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but OUT ANY WARRANTY; out even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 *  along  this program.  If not, see <http:www.gnu.orglicenses>
 */

package org.restcomm.connect.sdr.api;

import akka.actor.ActorRef;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.commons.patterns.StopObserving;
import org.restcomm.connect.telephony.api.CallStateChanged;

import javax.servlet.sip.SipServletMessage;

/**
 * @author oleg.agafonov@telestax.com (Oleg Agafonov)
 */
public abstract class SdrService extends RestcommUntypedActor {

    @Override
    public void onReceive(Object message) throws Exception {
        ActorRef self = self();
        ActorRef sender = sender();
        if (message instanceof StartSdrService) {
            onStartSdrService((StartSdrService) message, self, sender);
        } else if (message instanceof Observing) {
            onStartObserving((Observing) message, self, sender);
        } else if (message instanceof CallStateChanged) {
            onCallStateChanged((CallStateChanged) message, self, sender);
        } else if (message instanceof StopObserving) {
            onStopObserving((StopObserving) message, self, sender);
        } else if (message instanceof SipServletMessage) {
            onSipServletMessage((SipServletMessage) message, self, sender);
        }
    }

    protected abstract void onStartSdrService(StartSdrService message, ActorRef self, ActorRef sender);

    protected abstract void onStartObserving(Observing message, ActorRef self, ActorRef sender);

    protected abstract void onCallStateChanged(CallStateChanged message, ActorRef self, ActorRef sender);

    protected abstract void onStopObserving(StopObserving message, ActorRef self, ActorRef sender);

    protected abstract void onSipServletMessage(SipServletMessage message, ActorRef self, ActorRef sender);
}
