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
import org.mobicents.servlet.restcomm.entities.ConferenceDetailRecordFilter;
import org.mobicents.servlet.restcomm.entities.MediaServerEntity;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.mgcp.MediaResourceBrokerResponse;
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.GetConferenceMediaResourceController;
import org.mobicents.servlet.restcomm.mgcp.mrb.messages.GetMediaGateway;
import org.mobicents.servlet.restcomm.telephony.ConferenceInfo;
import org.mobicents.servlet.restcomm.telephony.ConferenceStateChanged;

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

    private ActorRef getBridgeConnector() {
        return getContext().actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new ConferenceMediaResourceController(system, mediaGatewayMap, configuration, storage);
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
        } else if (GetConferenceMediaResourceController.class.equals(klass)){
            sender.tell(new MediaResourceBrokerResponse<ActorRef>(getBridgeConnector()), self);
        }
    }

    private void onGetMediaGateway(GetMediaGateway message, ActorRef self, ActorRef sender) {
        final ConferenceInfo conferenceInfo = message.conferenceInfo();
        final Sid callSid = message.callSid();

        // if its not request for conference return home media-gateway (media-server associated with this RC instance)
        if(conferenceInfo == null){
            updateMSIdinCallDetailRecord(msId, callSid);
        }else{
            addConferenceDetailRecord(conferenceInfo, callSid);
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

    private void addConferenceDetailRecord(final ConferenceInfo conferenceInfo, final Sid callSid) {
        if(conferenceInfo == null || conferenceInfo.name() == null){
            logger.info("provided conference info/sid is null, this can lead to problems in future of this call");
        }else{
            try{
                CallDetailRecordsDao callDao = storage.getCallDetailRecordsDao();
                CallDetailRecord callRecord = callDao.getCallDetailRecord(callSid);
                if(callRecord != null){
                    logger.info("updateMSIdinConferenceDetailRecord: SID: "+conferenceInfo.sid()+" NAME: "+conferenceInfo.name()+" STATE: "+conferenceInfo.state());
                    ConferenceDetailRecordsDao dao = storage.getConferenceDetailRecordsDao();

                    // check if a conference with same name/account is running.
                    final String[] cnfNameAndAccount = conferenceInfo.name().split(":");
                    final String accountSid = cnfNameAndAccount[0];
                    final String friendlyName = cnfNameAndAccount[1];

                    ConferenceDetailRecordFilter filter = new ConferenceDetailRecordFilter(accountSid, null, null, null, friendlyName, 1, 0);
                    List<ConferenceDetailRecord> records = dao.getConferenceDetailRecords(filter);
                    boolean isConferenceRunningOnAnotherInstance = false;

                    for (ConferenceDetailRecord cdr : records){
                        if( !(cdr.getStatus().equalsIgnoreCase("COMPLETED") || cdr.getStatus().equalsIgnoreCase("FAILED")) ){
                            isConferenceRunningOnAnotherInstance = true;
                            break;
                        }
                    }

                    // this is first record of this conference on all instances of
                    if(!isConferenceRunningOnAnotherInstance){
                        final ConferenceDetailRecord.Builder conferenceBuilder = ConferenceDetailRecord.builder();
                        conferenceBuilder.setSid(Sid.generate(Sid.Type.CONFERENCE));
                        conferenceBuilder.setDateCreated(DateTime.now());

                        conferenceBuilder.setAccountSid(new Sid(accountSid));
                        conferenceBuilder.setStatus(ConferenceStateChanged.State.INITIALIZING.toString());
                        conferenceBuilder.setApiVersion(callRecord.getApiVersion());
                        final StringBuilder UriBuffer = new StringBuilder();
                        UriBuffer.append("/").append(callRecord.getApiVersion()).append("/Accounts/").append(accountSid).append("/Conferences/");
                        UriBuffer.append(conferenceInfo.sid());
                        final URI uri = URI.create(UriBuffer.toString());
                        conferenceBuilder.setUri(uri);
                        conferenceBuilder.setFriendlyName(friendlyName);
                        conferenceBuilder.setMasterMsId(msId);

                        ConferenceDetailRecord cdr = conferenceBuilder.build();
                        dao.addConferenceDetailRecord(cdr);
                    }else{
                        logger.info("A conference with same name is running. According to database record.");
                    }
                }else{
                    logger.info("call record is null");
                }
            }catch(Exception e){
                logger.error("ERROR SAVING CONFERENCE IN DATBASE");
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

                final String msIpAddress = configuration.getString("mgcp-servers.mgcp-server(" + count + ").remote-address");
                final String msPort = configuration.getString("mgcp-servers.mgcp-server(" + count + ").remote-port");
                final String timeOut = configuration.getString("mgcp-servers.mgcp-server(" + count + ").response-timeout");

                final MediaServerEntity.Builder builder = MediaServerEntity.builder();
                builder.setMsIpAddress(msIpAddress);
                builder.setMsPort(msPort);
                builder.setTimeOut(timeOut);

                MediaServersDao dao = storage.getMediaServersDao();
                final MediaServerEntity freshMediaServerEntity = builder.build();
                final List<MediaServerEntity> existingMediaServersForSameIP = dao.getMediaServerEntityByIP(msIpAddress);

                if(existingMediaServersForSameIP == null || existingMediaServersForSameIP.size()==0){
                    dao.addMediaServer(freshMediaServerEntity);
                    final List<MediaServerEntity> newMediaServerEntity = dao.getMediaServerEntityByIP(msIpAddress);
                    this.msId = newMediaServerEntity.get(0).getMsId()+"";
                }else{
                    this.msId = existingMediaServersForSameIP.get(0).getMsId()+"";
                    dao.updateMediaServer(freshMediaServerEntity);
                    if(existingMediaServersForSameIP.size()>1)
                        logger.error("in DB: there are multiple media servers registered for same IP addres");
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
