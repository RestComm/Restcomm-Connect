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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.servlet.restcomm.entities.InstanceId;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.MonitoringServiceResponse;
import org.mobicents.servlet.restcomm.telephony.TextMessage;
import org.mobicents.servlet.restcomm.telephony.UserRegistration;
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
    private final Map<String, String> registeredUsers;
    private final AtomicInteger callsUpToNow;
    private final AtomicInteger incomingCallsUpToNow;
    private final AtomicInteger outgoingCallsUpToNow;
    private final AtomicInteger completedCalls;
    private final AtomicInteger failedCalls;
    private final AtomicInteger busyCalls;
    private final AtomicInteger canceledCalls;
    private final AtomicInteger noAnswerCalls;
    private final AtomicInteger notFoundCalls;
    private final AtomicInteger textInboundToApp;
    private final AtomicInteger textInboundToClient;
    private final AtomicInteger textInboundToProxyOut;
    private final AtomicInteger textOutbound;
    private final AtomicInteger textNotFound;
    private InstanceId instanceId;


    public MonitoringService() {
        this.callMap = new ConcurrentHashMap<String, ActorRef>();
        this.callDetailsMap = new ConcurrentHashMap<String, CallInfo>();
        this.callStateMap = new ConcurrentHashMap<String, CallStateChanged.State>();
        registeredUsers = new ConcurrentHashMap<String, String>();
        callsUpToNow = new AtomicInteger();
        incomingCallsUpToNow = new AtomicInteger();
        outgoingCallsUpToNow = new AtomicInteger();
        completedCalls = new AtomicInteger();
        failedCalls = new AtomicInteger();
        busyCalls = new AtomicInteger();
        canceledCalls = new AtomicInteger();
        noAnswerCalls = new AtomicInteger();
        notFoundCalls = new AtomicInteger();
        textInboundToApp = new AtomicInteger();
        textInboundToClient = new AtomicInteger();
        textInboundToProxyOut = new AtomicInteger();
        textOutbound = new AtomicInteger();
        textNotFound = new AtomicInteger();
        if(logger.isInfoEnabled()){
            logger.info("Monitoring Service started");
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if(logger.isInfoEnabled()){
            logger.info("MonitoringService Processing Message: \"" + klass.getName() + " sender : "+ sender.getClass()+" self is terminated: "+self.isTerminated());
        }

        if (InstanceId.class.equals(klass)) {
            onGotInstanceId((InstanceId) message, self, sender);
        } else if (Observing.class.equals(klass)) {
            onStartObserve((Observing) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            if(logger.isInfoEnabled()){
                logger.info("Received stop observing");
            }
            onStopObserving((StopObserving) message, self, sender);
        } else if (CallResponse.class.equals(klass)) {
            onCallResponse((CallResponse<CallInfo>)message, self, sender);
        } else if (CallStateChanged.class.equals(klass)) {
            onCallStateChanged((CallStateChanged)message, self, sender);
        } else if (GetLiveCalls.class.equals(klass)) {
            onGetLiveCalls((GetLiveCalls)message, self, sender);
        } else if (UserRegistration.class.equals(klass)) {
            onUserRegistration((UserRegistration)message, self, sender);
        } else if (TextMessage.class.equals(klass)) {
            onTextMessage((TextMessage) message, self, sender);
        }
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onTextMessage(TextMessage message, ActorRef self, ActorRef sender) {
        TextMessage.SmsState state = message.getState();
        if (state.equals(TextMessage.SmsState.INBOUND_TO_APP)) {
            textInboundToApp.incrementAndGet();
        } else if (state.equals(TextMessage.SmsState.INBOUND_TO_CLIENT)) {
            textInboundToClient.incrementAndGet();
        } else if (state.equals(TextMessage.SmsState.INBOUND_TO_PROXY_OUT)) {
            textInboundToProxyOut.incrementAndGet();
        } else if (state.equals(TextMessage.SmsState.OUTBOUND)) {
            textOutbound.incrementAndGet();
        } else if (state.equals(TextMessage.SmsState.NOT_FOUND)) {
            textNotFound.incrementAndGet();
        }
    }

    private void onGotInstanceId(InstanceId instanceId, ActorRef self, ActorRef sender) {
        this.instanceId = instanceId;
    }

    /**
     * @param userRegistration
     * @param self
     * @param sender
     */
    private void onUserRegistration(UserRegistration userRegistration, ActorRef self, ActorRef sender) {
        if (userRegistration.getRegistered()) {
            registeredUsers.put(userRegistration.getUser(), userRegistration.getAddress());
        } else {
            if (registeredUsers.containsKey(userRegistration.getUser())) {
                registeredUsers.remove(userRegistration.getUser());
            }
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
        callsUpToNow.incrementAndGet();
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
        if (callInfo.direction().equalsIgnoreCase("inbound")) {
            incomingCallsUpToNow.incrementAndGet();
        } else {
            outgoingCallsUpToNow.incrementAndGet();
        }
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onCallStateChanged(CallStateChanged message, ActorRef self, ActorRef sender) {
        String senderPath = sender.path().name();
        if (senderPath != null && message != null && callStateMap != null && callDetailsMap != null) {
            CallStateChanged.State callState = message.state();
            callStateMap.put(senderPath, callState);
            CallInfo callInfo = callDetailsMap.get(senderPath);
            if (callInfo != null) {
                callInfo.setState(callState);
                if (callState.equals(CallStateChanged.State.FAILED)) {
                    failedCalls.incrementAndGet();
                } else if (callState.equals(CallStateChanged.State.COMPLETED)) {
                    completedCalls.incrementAndGet();
                } else if(callState.equals(CallStateChanged.State.BUSY)) {
                    busyCalls.incrementAndGet();
                } else if (callState.equals(CallStateChanged.State.CANCELED)) {
                    canceledCalls.incrementAndGet();
                } else if (callState.equals(CallStateChanged.State.NO_ANSWER)) {
                    noAnswerCalls.incrementAndGet();
                } else if (callState.equals(CallStateChanged.State.NOT_FOUND)) {
                    notFoundCalls.incrementAndGet();
                }
            } else if(logger.isInfoEnabled()){
                logger.info("CallInfo was not in the store for Call: "+senderPath);
            }
        } else {
            logger.error("MonitoringService, SenderPath or storage is null.");
        }
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onGetLiveCalls(GetLiveCalls message, ActorRef self, ActorRef sender) {
        List<CallInfo> callDetailsList = new ArrayList<CallInfo>(callDetailsMap.values());
        Map<String, Integer> countersMap = new HashMap<String, Integer>();

        final AtomicInteger liveIncomingCalls = new AtomicInteger();
        final AtomicInteger liveOutgoingCalls = new AtomicInteger();

        countersMap.put("TotalCallsSinceUptime",callsUpToNow.get());
        countersMap.put("IncomingCallsSinceUptime", incomingCallsUpToNow.get());
        countersMap.put("OutgoingCallsSinceUptime", outgoingCallsUpToNow.get());
        countersMap.put("RegisteredUsers", registeredUsers.size());
        countersMap.put("LiveCalls", callDetailsList.size());

        for (CallInfo callInfo : callDetailsList) {
            if (callInfo.direction().equalsIgnoreCase("inbound")) {
                liveIncomingCalls.incrementAndGet();
            } else if (callInfo.direction().contains("outbound")) {
                liveOutgoingCalls.incrementAndGet();
            }
        }
        countersMap.put("LiveIncomingCalls", liveIncomingCalls.get());
        countersMap.put("LiveOutgoingCalls", liveOutgoingCalls.get());

        countersMap.put("CompletedCalls", completedCalls.get());
        countersMap.put("NoAnswerCalls", noAnswerCalls.get());
        countersMap.put("BusyCalls", busyCalls.get());
        countersMap.put("FailedCalls", failedCalls.get());
        countersMap.put("NotFoundCalls", notFoundCalls.get());
        countersMap.put("CanceledCalls", canceledCalls.get());
        countersMap.put("TextMessageInboundToApp", textInboundToApp.get());
        countersMap.put("TextMessageInboundToClient", textInboundToClient.get());
        countersMap.put("TextMessageInboundToProxyOut", textInboundToProxyOut.get());
        countersMap.put("TextMessageNotFound", textNotFound.get());
        countersMap.put("TextMessageOutbound", textOutbound.get());

        MonitoringServiceResponse callInfoList = new MonitoringServiceResponse(instanceId, callDetailsList, countersMap);
        sender.tell(callInfoList, self);
    }

    @Override
    public void postStop() {
        if(logger.isInfoEnabled()){
            logger.info("Monitoring Service at postStop()");
        }
        super.postStop();
    }
}
