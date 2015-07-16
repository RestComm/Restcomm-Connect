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
package com.telestax.servlet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallResponse;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;
import org.mobicents.servlet.restcomm.telephony.GetCallInfo;
import org.mobicents.servlet.restcomm.telephony.GetLiveCalls;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
public class MonitoringService extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private final Map<String, ActorRef> callMap;
    private final Map<String,CallInfo> callDetailsMap;
    private final Map<String, CallStateChanged.State> callStateMap;

    public MonitoringService() {
        this.callMap = new ConcurrentHashMap<String, ActorRef>();
        this.callDetailsMap = new ConcurrentHashMap<String, CallInfo>();
        this.callStateMap = new ConcurrentHashMap<String, CallStateChanged.State>();
        logger.info("Monitoring Service started");
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        logger.info("Processing Message: \"" + klass.getName() + " sender : "+ sender.getClass());

        if (Observing.class.equals(klass)) {
            onStartObserve((Observing) message, self, sender);
        } if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } if (CallResponse.class.equals(klass)) {
            onCallResponse((CallResponse<CallInfo>)message, self, sender);
        } if (CallStateChanged.class.equals(klass)) {
            onCallStateChanged((CallStateChanged)message, self, sender);
        } if (GetLiveCalls.class.equals(klass)) {
            onGetLiveCalls((GetLiveCalls)message, self, sender);
        }
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onStartObserve(Observing message, ActorRef self, ActorRef sender) {
        String senderPath = sender.path().name();
        sender.tell(new GetCallInfo(), self);
        callMap.put(senderPath, sender);
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onStopObserving(StopObserving message, ActorRef self, ActorRef sender) {
        String senderPath = sender.path().name();
        callMap.remove(senderPath);
        callDetailsMap.remove(senderPath);
        callStateMap.remove(senderPath);
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onCallResponse(CallResponse<CallInfo> message, ActorRef self, ActorRef sender) {
        String senderPath = sender.path().name();
        CallInfo callInfo = message.get();
        callDetailsMap.put(senderPath, callInfo);
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onCallStateChanged(CallStateChanged message, ActorRef self, ActorRef sender) {
        String senderPath = sender.path().name();
        CallStateChanged.State callState = message.state();
        callStateMap.put(senderPath, callState);
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onGetLiveCalls(GetLiveCalls message, ActorRef self, ActorRef sender) {
        sender.tell(callDetailsMap, self);
    }
}
