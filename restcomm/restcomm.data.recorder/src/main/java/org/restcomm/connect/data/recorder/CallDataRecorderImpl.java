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
package org.restcomm.connect.data.recorder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.commons.patterns.StopObserving;
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.data.recorder.api.RecordCallData;
import org.restcomm.connect.data.recorder.api.interfaces.CallDataRecorder;
import org.restcomm.connect.telephony.api.CallInfo;
import org.restcomm.connect.telephony.api.CallResponse;
import org.restcomm.connect.telephony.api.CallStateChanged;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 *
 */
@Immutable
public final class CallDataRecorderImpl extends CallDataRecorder{

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private final List<ActorRef> observers;
    private final DaoManager daoManager;
    private CallInfo callInfo;
    private Sid sid;
    private CallDetailRecord cdr;

    public CallDataRecorderImpl(final DaoManager daoManager) {
        super();
        this.observers = new ArrayList<ActorRef>();
        this.daoManager = daoManager;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if(logger.isInfoEnabled()) {
            logger.info("********** CallDataRecorder" + self().path() + " Processing Message: \"" + klass.getName() + " sender : "
                + sender.path().toString());
        }

        if (Observe.class.equals(klass)) {
            onObserve((Observe) message, self, sender);
        } else if (StopObserving.class.equals(klass)) {
            onStopObserving((StopObserving) message, self, sender);
        } else if(CallResponse.class.equals(klass)){
        	onCallResponse((CallResponse) message, self, sender);
        } else if(CallStateChanged.class.equals(klass)){
        	onCallStateChanged((CallStateChanged) message, self, sender);
        }
    }

    private void onObserve(Observe message, ActorRef self, ActorRef sender) throws Exception {
        final ActorRef observer = message.observer();
        if (observer != null) {
            this.observers.add(observer);
            observer.tell(new Observing(self), self);
        }
    }

    private void onStopObserving(StopObserving message, ActorRef self, ActorRef sender) throws Exception {
        final ActorRef observer = message.observer();
        if (observer != null) {
            observers.remove(observer);
        }
    }

    private void onCallResponse(CallResponse<CallInfo> message, ActorRef self, ActorRef sender) {
        if(logger.isDebugEnabled()){
            logger.debug("callInfo: "+callInfo.toString());
        }
        CallInfo ci = message.get();
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onCallStateChanged(CallStateChanged message, ActorRef self, ActorRef sender) {
    	CallStateChanged.State callState = message.state();
        if(logger.isDebugEnabled()){
            logger.debug("onCallStateChanged: "+callState.name());
        }
        CallDetailRecordsDao dao = daoManager.getCallDetailRecordsDao();
        cdr = cdr.setStatus(callState.name());
        dao.updateCallDetailRecord(cdr);
    }

    private void onRecordCallData(RecordCallData recordCallData, ActorRef self, ActorRef sender) throws Exception {
    	CallInfo callInfo = recordCallData.callInfo();
    	if(callInfo == null){
    		logger.error("Received null CallInfo");
    	}else{
    		this.sid = callInfo.sid();
    		CallDetailRecordsDao dao = daoManager.getCallDetailRecordsDao();
            cdr = dao.getCallDetailRecord(sid);
            if(cdr == null){
            	// Create a call detail record for the call.
                final CallDetailRecord.Builder builder = CallDetailRecord.builder();
                builder.setSid(callInfo.sid());
                builder.setInstanceId(RestcommConfiguration.getInstance().getMain().getInstanceId());
                builder.setDateCreated(callInfo.dateCreated());
                builder.setAccountSid(accountId);
                builder.setTo(callInfo.to());
                if (callInfo.fromName() != null) {
                    builder.setCallerName(callInfo.fromName());
                } else {
                    builder.setCallerName("Unknown");
                }
                if (callInfo.from() != null) {
                    builder.setFrom(callInfo.from());
                } else {
                    builder.setFrom("Unknown");
                }
                builder.setForwardedFrom(callInfo.forwardedFrom());
                builder.setPhoneNumberSid(phoneId);
                builder.setStatus(callState.toString());
                final DateTime now = DateTime.now();
                builder.setStartTime(now);
                builder.setDirection(callInfo.direction());
                builder.setApiVersion(version);
                builder.setPrice(new BigDecimal("0.00"));
                builder.setMuted(false);
                builder.setOnHold(false);
                // TODO implement currency property to be read from Configuration
                builder.setPriceUnit(Currency.getInstance("USD"));
                final StringBuilder buffer = new StringBuilder();
                buffer.append("/").append(version).append("/Accounts/");
                buffer.append(accountId.toString()).append("/Calls/");
                buffer.append(callInfo.sid().toString());
                final URI uri = URI.create(buffer.toString());
                builder.setUri(uri);

                builder.setCallPath(call.path().toString());

                callRecord = builder.build();
            }else{
            	
            }
    	}
    }

    @Override
    public void postStop() {
        try {
            onStopObserving(new StopObserving(), self(), null);
            getContext().stop(self());
        } catch (Exception exception) {
            if(logger.isInfoEnabled()) {
                logger.info("Exception during CallDataRecorderImpl postStop while trying to remove observers: "+exception);
            }
        }
        super.postStop();
    }
}
