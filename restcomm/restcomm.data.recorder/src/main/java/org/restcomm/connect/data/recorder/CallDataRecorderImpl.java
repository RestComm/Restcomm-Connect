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
import org.restcomm.connect.data.recorder.api.interfaces.CallDataRecorder;
import org.restcomm.connect.telephony.api.CallInfo;
import org.restcomm.connect.telephony.api.CallResponse;
import org.restcomm.connect.telephony.api.CallStateChanged;
import org.restcomm.connect.telephony.api.UpdateCallInfo;

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
    private Sid sid;
    private CallDetailRecord cdr;
    private CallInfo callInfo;

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
            onCallResponse((CallResponse<?>) message, self, sender);
        } else if(CallStateChanged.class.equals(klass)){
            onCallStateChanged((CallStateChanged) message, self, sender);
        } else if(UpdateCallInfo.class.equals(klass)){
            onUpdateCallInfo((UpdateCallInfo) message, self, sender);
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

    private void onCallResponse(CallResponse<?> message, ActorRef self, ActorRef sender) {
        try{
            if(!message.get().getClass().equals(CallInfo.class)){
                if(logger.isInfoEnabled()){
                    logger.info("onCallResponse: message class name is: "+message.get().getClass() +" no further action will be taken by this actor..");
                }
            }else{
                callInfo = (CallInfo)message.get();

                if (callInfo == null) {
                        logger.warning("onCallResponse: callInfo is null");
                }else{
                    if(logger.isDebugEnabled()){
                        logger.debug("onCallResponse: callInfo: "+callInfo);
                    }
                    final CallDetailRecordsDao dao = daoManager.getCallDetailRecordsDao();
                    sid = callInfo.sid();
                    CallDetailRecord cdr = dao.getCallDetailRecord(callInfo.sid());
                    if(cdr == null){
                        //insert a new record
                        final CallDetailRecord.Builder builder = CallDetailRecord.builder();
                        builder.setSid(callInfo.sid());
                        builder.setInstanceId(RestcommConfiguration.getInstance().getMain().getInstanceId());
                        builder.setDateCreated(callInfo.dateCreated());
                        builder.setAccountSid(callInfo.accountSid());
                        builder.setTo(callInfo.to());
                        builder.setStartTime(new DateTime());
                        builder.setStatus(callInfo.state().toString());
                        final DateTime now = DateTime.now();
                        builder.setStartTime(now);
                        builder.setDirection(callInfo.direction());
                        builder.setApiVersion(callInfo.version());
                        builder.setPrice(new BigDecimal("0.00"));
                        // TODO implement currency property to be read from Configuration
                        builder.setPriceUnit(Currency.getInstance("USD"));
                        final StringBuilder buffer = new StringBuilder();
                        buffer.append("/").append(callInfo.version()).append("/Accounts/");
                        buffer.append(callInfo.accountSid().toString()).append("/Calls/");
                        buffer.append(callInfo.sid().toString());
                        final URI uri = URI.create(buffer.toString());
                        builder.setUri(uri);
                        builder.setCallPath(self().path().toString());

                        if (callInfo.direction().equals("inbound")) {
                            if (callInfo.from() != null) {
                                builder.setFrom(callInfo.from());
                            } else {
                                builder.setFrom("Unknown");
                            }
                            builder.setForwardedFrom(callInfo.forwardedFrom());
                            builder.setPhoneNumberSid(callInfo.phoneNumberSid());
                        }else{
                            String fromString = (callInfo.from() != null ? callInfo.from() : "CALLS REST API");
                            builder.setFrom(fromString);
                            builder.setParentCallSid(callInfo.parentCallSid());
                        }
                        cdr = builder.build();
                        dao.addCallDetailRecord(cdr);
                    }else{
                        //update existing record
                        if(logger.isDebugEnabled()){
                            logger.debug("onCallResponse: callInfo: CDR already exists, nothing to do.");
                        }
                    }
                }
            }
        }catch(Exception e){
            logger.error("onCallResponse CallInfo: Exception while trying to add CDR: ", e);
        }
    }

    /**
     * @param message
     * @param self
     * @param sender
     */
    private void onCallStateChanged(CallStateChanged message, ActorRef self, ActorRef sender) {
        try{
            CallStateChanged.State callState = message.state();

            //check if call records is already there or not..
            if(sid == null){
                logger.error("CallStateChanged received for an unknown call: sender path is: "+sender.path());
            }else{
                CallDetailRecordsDao dao = daoManager.getCallDetailRecordsDao();
                cdr = dao.getCallDetailRecord(sid);
                cdr = cdr.setStatus(callState.name());

                switch (callState) {
                    case BUSY:
                        cdr = cdr.setDuration(0);
                        cdr = cdr.setRingDuration((int) ((DateTime.now().getMillis() - cdr.getStartTime().getMillis()) / 1000));
                        break;
                    case IN_PROGRESS:
                        cdr = cdr.setAnsweredBy(callInfo.to());
                        break;
                    case COMPLETED:
                        cdr = cdr.setEndTime(DateTime.now());
                        cdr = cdr.setDuration((int) ((DateTime.now().getMillis() - cdr.getStartTime().getMillis()) / 1000));
                        break;

                    case QUEUED:
                    case NO_ANSWER:
                    case NOT_FOUND:
                    case CANCELED:
                    case FAILED:
                    case RINGING:
                        break;
                    default:
                        break;
                }
                dao.updateCallDetailRecord(cdr);
            }
        }catch(Exception e){
            logger.error("onCallStateChanged: Exception while trying to update CDR: ", e);
        }
    }

    private void onUpdateCallInfo(UpdateCallInfo message, ActorRef self, ActorRef sender) {
        if(logger.isDebugEnabled()){
            logger.debug("onUpdateCallInfo: "+message.toString());
            logger.debug("CDR current values: "+cdr);
        }
        try{
            cdr = daoManager.getCallDetailRecordsDao().getCallDetailRecord(message.getSid());

            cdr = message.getStatus() == null ? cdr : cdr.setStatus(message.getStatus().name());
            cdr = message.getStartTime() == null ? cdr : cdr.setStartTime(message.getStartTime());
            cdr = message.getEndTime() == null ? cdr : cdr.setEndTime(message.getEndTime());
            cdr = message.getPrice() == null ? cdr : cdr.setPrice(message.getPrice());
            cdr = message.getAnsweredBy() == null ? cdr : cdr.setAnsweredBy(message.getAnsweredBy());
            cdr = message.getConferenceSid() == null ? cdr : cdr.setConferenceSid(message.getConferenceSid());
            cdr = message.getMuted() == null ? cdr : cdr.setMuted(message.getMuted());
            cdr = message.getStartConferenceOnEnter() == null ? cdr : cdr.setStartConferenceOnEnter(message.getStartConferenceOnEnter());
            cdr = message.getEndConferenceOnExit() == null ? cdr : cdr.setEndConferenceOnExit(message.getEndConferenceOnExit());
            cdr = message.getOnHold() == null ? cdr : cdr.setOnHold(message.getOnHold());
            cdr = message.getMsId() == null ? cdr : cdr.setMsId(message.getMsId());
            cdr = message.upateDuration() ? cdr : cdr.setDuration((int) ((DateTime.now().getMillis() - cdr.getStartTime().getMillis()) / 1000));
            cdr = message.updateRingDuration() ? cdr : cdr.setRingDuration((int) ((DateTime.now().getMillis() - cdr.getStartTime().getMillis()) / 1000));
            // TODO: enable it to make beep configurable in MRB.
            // cdr = message.beep() == null ? cdr : cdr.setBeep(message.beep()); enable it later

            daoManager.getCallDetailRecordsDao().updateCallDetailRecord(cdr);

            if(logger.isDebugEnabled()){
                logger.debug("CDR updated values: "+cdr);
            }
        }catch(Exception e){
            logger.error("Exception while trying to update CDR: ", e);
        }
    }

    @Override
    public void postStop() {
        try {
            onStopObserving(new StopObserving(), self(), null);
            getContext().stop(self());
        } catch (Exception exception) {
            logger.error("Exception during CallDataRecorderImpl postStop while trying to remove observers: "+exception);
        }
        super.postStop();
    }
}
