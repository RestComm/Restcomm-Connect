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
package org.mobicents.servlet.restcomm.mgcp.mrb;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.ConferenceDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.MediaServersDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.ConferenceDetailRecord;
import org.mobicents.servlet.restcomm.entities.MediaServerEntity;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.CreateBridgeEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateLink;
import org.mobicents.servlet.restcomm.mgcp.InitializeLink;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaResourceBrokerResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaServerRouter;
import org.mobicents.servlet.restcomm.mgcp.OpenLink;
import org.mobicents.servlet.restcomm.mgcp.UpdateLink;
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.GetMediaGateway;
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.JoinComplete;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerController.AbstractAction;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerStateChanged.MediaServerControllerState;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.AcquiringBridge;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.AcquiringInternalLink;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.AcquiringMediaGatewayInfo;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.AcquiringMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.AcquiringRemoteConnection;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.Active;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.ClosingRemoteConnection;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.CreatingMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.EnterClosingInternalLink;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.ExitClosingInternalLink;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.Failed;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.FinalState;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.GetMediaGatewayFromMRB;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.Inactive;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.InitializingInternalLink;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.InitializingRemoteConnection;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.Muting;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.OpeningInternalLink;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.OpeningRemoteConnection;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.Pending;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.Stopping;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.Unmuting;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.UpdatingInternalLink;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsCallController.UpdatingRemoteConnection;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.telephony.ConferenceInfo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

public class MediaResourceBroker extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // Finite State Machine
    private final FiniteStateMachine fsm;
    private final State uninitialized;
    private final State providingMediaGateway;
    private final State active;
    private final State inactive;
    private final State failed;
    //Get SDP
    private final State creatingBridge;
    private final State acquiringInternalLink;
    private final State initializingInternalLink;
    private final State openingInternalLink;
    private final State updatingInternalLink;
    //Pass SDP
    private final State initializingConnectingBridges;
    //update SDP
    private final State updatingHomeBridge;
    private final State stopping;
    private Boolean fail;

    private final ActorSystem system;
    private final Map<String, ActorRef> mediaGatewayMap;
    private final Configuration configuration;
    private final MediaServerRouter msRouter;

    private final DaoManager storage;

    public MediaResourceBroker(ActorSystem system, Map<String, ActorRef> gateways, Configuration configuration, DaoManager storage){
        this.system = system;
        this.mediaGatewayMap = gateways;
        this.configuration = configuration;
        this.msRouter = new MediaServerRouter(gateways, configuration);
        this.storage = storage;

        saveMediaServersInDB();
        
     // Initialize the states for the FSM.
        this.uninitialized = new State("uninitialized", null, null);
        this.providingMediaGateway = new State("providing Media Gateway", new ProvidingMediaGateway(source), null);
        this.creatingBridge = new State("creating bridge", new CreatingBridge(source), null);
        this.acquiringInternalLink = new State("acquiring internal link", new AcquiringInternalLink(source), null);
        this.initializingInternalLink = new State("acquiring media bridge", new InitializingInternalLink(source), null);
        this.openingInternalLink = new State("creating media group", new OpeningInternalLink(source), null);
        this.updatingInternalLink = new State("acquiring connection", new UpdatingInternalLink(source), null);
        this.initializingConnectingBridges = new State("initializing connection", new InitializingConnectingBridges(source), null);
        this.updatingHomeBridge = new State("opening connection", new UpdatingHomeBridge(source), null);
        this.active = new State("active", new Active(source), null);
        this.stopping = new State("stopping", new Stopping(source));
        this.inactive = new State("inactive", new Inactive(source));
        this.failed = new State("failed", new Failed(source));

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(this.uninitialized, this.providingMediaGateway));


        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** MediaResourceBroker " + self().path() + " Processing Message: " + klass.getName());
        }
        if (GetMediaGateway.class.equals(klass)) {
            onGetMediaGateway((GetMediaGateway) message, self, sender);
        } else if (JoinComplete.class.equals(klass)){
            onJoinComplete((JoinComplete) message, self, sender);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            onMediaGatewayResponse((MediaGatewayResponse<?>) message, self, sender);
        }
    }

    private void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) {
		// TODO Auto-generated method stub
		
	}

	private void onJoinComplete(JoinComplete message, ActorRef self, ActorRef sender) {
        logger.info("conferenceName: "+message.conferenceName()+" callSid: "+message.callSid()+" conferenceSid: "+message.conferenceSid()+" cnfEndpoint: "+message.cnfEndpoint());
        String msId = getMSIdinCallDetailRecord(message.callSid());
        if(msId == null){
            logger.info("invalid callsid");
            return;
        }
        ActorRef mediaGateway = mediaGatewayMap.get(msId);
        mediaGateway.tell(new CreateBridgeEndpoint(message.mediaSession()), sender);
    }

    private void onGetMediaGateway(GetMediaGateway message, ActorRef self, ActorRef sender) {
        final ConferenceInfo conferenceInfo = message.conferenceInfo();
        final Sid callSid = message.callSid();
        String msId = null;
        ActorRef mediaGateway = null;

        // if its not request for conference return media-gateway according to algo.
        if(conferenceInfo == null){
            msId = msRouter.getNextMediaServerKey();
            logger.info("msId: "+msId);
            mediaGateway = mediaGatewayMap.get(msId);
            updateMSIdinCallDetailRecord(msId, callSid);
        }else{
            // get the call and see where it is connected and return same msId so call and its conferenceEndpoint are on same mediaserver
            msId = getMSIdinCallDetailRecord(callSid);
            if(msId == null){
                //TODO handle it more gracefully
                logger.info("invalid callsid");
                return;
            }
            mediaGateway = mediaGatewayMap.get(msId);
            addConferenceDetailRecord(conferenceInfo, msId, callSid);
        }

        sender.tell(new MediaResourceBrokerResponse<ActorRef>(mediaGateway), self);
    }

    private void updateMSIdinCallDetailRecord(final String msId, final Sid callSid){
        if(callSid == null){
            logger.info("Call Id is not specisfied");
        }else{
            logger.info("msId: "+msId+" callSid: "+ callSid.toString());

            CallDetailRecordsDao dao = storage.getCallDetailRecordsDao();
            CallDetailRecord cdr = dao.getCallDetailRecord(callSid);
            if(cdr != null){
                cdr = cdr.setMsId(msId);
                dao.updateCallDetailRecord(cdr);
            }else{
                logger.info("provided call id did not found");
            }
        }

    }

    private void addConferenceDetailRecord(final ConferenceInfo conferenceInfo, final String msId, final Sid callSid){
        if(conferenceInfo == null || conferenceInfo.name() == null){
            logger.info("provided conference info/sid is null, this can lead to problems in future of this call");
        }else{
            CallDetailRecordsDao callDao = storage.getCallDetailRecordsDao();
            CallDetailRecord callRecord = callDao.getCallDetailRecord(callSid);
            if(callRecord != null){
                logger.info("updateMSIdinConferenceDetailRecord: SID: "+conferenceInfo.sid()+" NAME: "+conferenceInfo.name()+" STATE: "+conferenceInfo.state());
                ConferenceDetailRecordsDao dao = storage.getConferenceDetailRecordsDao();
                ConferenceDetailRecord cdr = dao.getConferenceDetailRecord(conferenceInfo.sid());
                if(cdr == null){
                    final ConferenceDetailRecord.Builder conferenceBuilder = ConferenceDetailRecord.builder();
                    conferenceBuilder.setSid(conferenceInfo.sid());
                    conferenceBuilder.setDateCreated(DateTime.now());

                    String[] cnfNameAndAccount = conferenceInfo.name().split(":");
                    final Sid accountId = new Sid(cnfNameAndAccount[0]);
                    conferenceBuilder.setAccountSid(accountId);
                    conferenceBuilder.setStatus("CONNECTING");
                    conferenceBuilder.setApiVersion(callRecord.getApiVersion());
                    final StringBuilder UriBuffer = new StringBuilder();
                    UriBuffer.append("/").append(callRecord.getApiVersion()).append("/Accounts/").append(accountId.toString()).append("/Conferences/");
                    UriBuffer.append(conferenceInfo.sid());
                    final URI uri = URI.create(UriBuffer.toString());
                    conferenceBuilder.setUri(uri);
                    conferenceBuilder.setFriendlyName(cnfNameAndAccount[1]);
                    conferenceBuilder.setMsId(msId);

                    cdr = conferenceBuilder.build();
                    dao.addConferenceDetailRecord(cdr);
                }
            }else{
                logger.info("call record is null");
            }
        }
    }

    private String getMSIdinCallDetailRecord(Sid callSid){
        CallDetailRecordsDao dao = storage.getCallDetailRecordsDao();
        CallDetailRecord cdr = dao.getCallDetailRecord(callSid);

        return cdr.getMsId();
    }

    private void saveMediaServersInDB() {
        MediaServersDao dao = storage.getMediaServersDao();

        List<Object> mgcpMediaServers = configuration.getList("mgcp-servers.mgcp-server.local-address");
        int mgcpMediaServerListSize = mgcpMediaServers.size();

        //TODO remove this log line after completion
        logger.info("Available Media gateways are: "+mgcpMediaServerListSize);

        for (int count = 0; count < mgcpMediaServerListSize; count++) {

            final MediaServerEntity.Builder builder = MediaServerEntity.builder();

            final String msId = configuration.getString("mgcp-servers.mgcp-server(" + count + ").ms-id");
            final String msIpAddress = configuration.getString("mgcp-servers.mgcp-server(" + count + ").remote-address");
            final String msPort = configuration.getString("mgcp-servers.mgcp-server(" + count + ").remote-port");
            final String timeOut = configuration.getString("mgcp-servers.mgcp-server(" + count + ").response-timeout");

            builder.setMsId(msId);
            builder.setMsIpAddress(msIpAddress);
            builder.setMsPort(msPort);
            builder.setTimeOut(timeOut);

            final MediaServerEntity freshMediaServerEntity = builder.build();
            final MediaServerEntity existingMediaServerEntity = dao.getMediaServerEntity(msId);

            if(existingMediaServerEntity == null){
                dao.addMediaServer(freshMediaServerEntity);
            }else{
                dao.updateMediaServer(freshMediaServerEntity);
            }
        }
    }
    
    private final class AcquiringInternalLink extends AbstractAction {

        public AcquiringInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            // YES here made a sesssion with master RMS and probably switch everything to that
            // otherwise if nothing is done
            // this will send message to
            mediaGateway.tell(new CreateLink(session), source);
        }

    }

    private final class InitializingInternalLink extends AbstractAction {

        public InitializingInternalLink(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;

            if (self().path().toString().equalsIgnoreCase("akka://RestComm/user/$j")) {
                if(logger.isInfoEnabled()) {
                    logger.info("Initializing Internal Link for the Outbound call");
                }
            }

            if (bridgeEndpoint != null) {
                if(logger.isInfoEnabled()) {
                    logger.info("##################### $$ Bridge for Call " + self().path() + " is terminated: "
                        + bridgeEndpoint.isTerminated());
                }
                if (bridgeEndpoint.isTerminated()) {
                    // fsm.transition(message, acquiringMediaGatewayInfo);
                    // return;
                    if(logger.isInfoEnabled()) {
                        logger.info("##################### $$ Call :" + self().path() + " bridge is terminated.");
                    }
                    // final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
                    // Future<Object> future = (Future<Object>) akka.pattern.Patterns.ask(gateway, new
                    // CreateBridgeEndpoint(session), expires);
                    // MediaGatewayResponse<ActorRef> futureResponse = (MediaGatewayResponse<ActorRef>) Await.result(future,
                    // Duration.create(10, TimeUnit.SECONDS));
                    // bridge = futureResponse.get();
                    // if (!bridge.isTerminated() && bridge != null) {
                    // logger.info("Bridge for call: "+self().path()+" acquired and is not terminated");
                    // } else {
                    // logger.info("Bridge endpoint for call: "+self().path()+" is still terminated or null");
                    // }
                }
            }
            // if (bridge == null || bridge.isTerminated()) {
            // System.out.println("##################### $$ Bridge for Call "+self().path()+" is null or terminated: "+bridge.isTerminated());
            // }
            internalLink = response.get();
            internalLink.tell(new Observe(source), source);
            internalLink.tell(new InitializeLink(bridgeEndpoint, internalLinkEndpoint), source);
        }

    }

    private final class OpeningInternalLink extends AbstractAction {

        public OpeningInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(Object message) throws Exception {
            internalLink.tell(new OpenLink(internalLinkMode), source);
        }

    }

    private final class UpdatingInternalLink extends AbstractAction {

        public UpdatingInternalLink(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final UpdateLink update = new UpdateLink(ConnectionMode.SendRecv, UpdateLink.Type.PRIMARY);
            internalLink.tell(update, source);
        }

    }
    
    private final class Inactive extends FinalState {

        public Inactive(final ActorRef source) {
            super(source, MediaServerControllerState.INACTIVE);
        }

    }

    private final class Failed extends FinalState {

        public Failed(final ActorRef source) {
            super(source, MediaServerControllerState.FAILED);
        }

    }

}
