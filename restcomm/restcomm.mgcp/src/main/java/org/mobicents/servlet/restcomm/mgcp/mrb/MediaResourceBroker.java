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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.mobicents.servlet.restcomm.mgcp.MediaResourceBrokerResponse;
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.GetMRBShunt;
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.GetMediaGateway;
import org.mobicents.servlet.restcomm.telephony.ConferenceInfo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MediaResourceBroker extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final ActorSystem system;
    private final Configuration configuration;
    private final ActorRef mediaGateway;
    private String msId;
    
    private final Map<String, ActorRef> mediaGatewayMap;
    //private final MediaServerRouter msRouter;
    
    private final DaoManager storage;
    // Observer pattern
    private final List<ActorRef> observers;

    //public MediaResourceBroker(ActorSystem system, Map<String, ActorRef> gateways, Configuration configuration, DaoManager storage){
    public MediaResourceBroker(ActorSystem system, Map<String, ActorRef> gateways, Configuration configuration, DaoManager storage){
        super();
        
        this.system = system;
        this.configuration = configuration;
        this.storage = storage;
        //this.msRouter = new MediaServerRouter(gateways, configuration);
        this.mediaGatewayMap = gateways;

        // Observers
        this.observers = new ArrayList<ActorRef>(1);

        saveMediaServersInDB();
        this.mediaGateway = mediaGatewayMap.get(this.msId);
    }

    private ActorRef getMRBShunt() {
        return getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new MRBShunt(system, mediaGatewayMap, configuration, storage);
            }
        }));
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
        } else if (GetMRBShunt.class.equals(klass)){
            sender.tell(new MediaResourceBrokerResponse<ActorRef>(getMRBShunt()), self);
        }
    }

    private void onGetMediaGateway(GetMediaGateway message, ActorRef self, ActorRef sender) {
        final ConferenceInfo conferenceInfo = message.conferenceInfo();
        final Sid callSid = message.callSid();

        // if its not request for conference return home media-gateway (media-server associated with this RC instance)
        if(conferenceInfo == null){
            updateMSIdinCallDetailRecord(msId, callSid);
        }else{
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

    private void saveMediaServersInDB() {

        List<Object> mgcpMediaServers = configuration.getList("mgcp-servers.mgcp-server.local-address");
        int mgcpMediaServerListSize = mgcpMediaServers.size();

        //TODO remove this log line after completion
        logger.info("Available Media gateways are: "+mgcpMediaServerListSize);

        for (int count = 0; count < mgcpMediaServerListSize; count++) {

            //To which of these MS is relative is considered one to one with this RC instance.
            String relativeMS = configuration.getString("mgcp-servers.mgcp-server(" + count + ").is-relative-ms");

            if(relativeMS != null && Boolean.parseBoolean(relativeMS)){

                final String msId = configuration.getString("mgcp-servers.mgcp-server(" + count + ").ms-id");
                final String msIpAddress = configuration.getString("mgcp-servers.mgcp-server(" + count + ").remote-address");
                final String msPort = configuration.getString("mgcp-servers.mgcp-server(" + count + ").remote-port");
                final String timeOut = configuration.getString("mgcp-servers.mgcp-server(" + count + ").response-timeout");

                this.msId = msId;

                final MediaServerEntity.Builder builder = MediaServerEntity.builder();
                builder.setMsId(msId);
                builder.setMsIpAddress(msIpAddress);
                builder.setMsPort(msPort);
                builder.setTimeOut(timeOut);

                MediaServersDao dao = storage.getMediaServersDao();
                final MediaServerEntity freshMediaServerEntity = builder.build();
                final MediaServerEntity existingMediaServerEntity = dao.getMediaServerEntity(msId);

                if(existingMediaServerEntity == null){
                    dao.addMediaServer(freshMediaServerEntity);
                }else{
                    dao.updateMediaServer(freshMediaServerEntity);
                }
            }
        }
    }

    @Override
    public void postStop() {
        // Cleanup resources
        cleanup();

        // Clean observers
        observers.clear();

        // Terminate actor
        getContext().stop(self());
    }

    protected void cleanup() {}

/*
 * 
    private String getMSIdinCallDetailRecord(Sid callSid){
        CallDetailRecordsDao dao = storage.getCallDetailRecordsDao();
        CallDetailRecord cdr = dao.getCallDetailRecord(callSid);

        return cdr.getMsId();
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
                logger.info("invalid callSid");
                return;
            }
            mediaGateway = mediaGatewayMap.get(msId);
            addConferenceDetailRecord(conferenceInfo, msId, callSid);
        }

        sender.tell(new MediaResourceBrokerResponse<ActorRef>(mediaGateway), self);
    }*/
}
