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
package org.restcomm.connect.mgcp.mrb;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.restcomm.connect.commons.loader.ObjectFactory;
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.ConferenceDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.MediaServersDao;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceDetailRecordFilter;
import org.restcomm.connect.dao.entities.MediaServerEntity;
import org.restcomm.connect.dao.entities.Sid;
import org.restcomm.connect.mgcp.MediaResourceBrokerResponse;
import org.restcomm.connect.mgcp.PowerOnMediaGateway;
import org.restcomm.connect.mrb.api.GetConferenceMediaResourceController;
import org.restcomm.connect.mrb.api.GetMediaGateway;
import org.restcomm.connect.mrb.api.MediaGatewayForConference;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.CreateProviderException;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpStack;

public class MediaResourceBroker extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final ActorSystem system;
    private final Configuration configuration;
    private final DaoManager storage;
    private final ClassLoader loader;
    private final ActorRef localMediaGateway;
    private String localMsId;
    private Map<String, ActorRef> mediaGatewayMap;

    private JainMgcpStack mgcpStack;
    private JainMgcpProvider mgcpProvider;

    private final List<ActorRef> observers;

    private MediaServerEntity localMediaServerEntity;

    public MediaResourceBroker(ActorSystem system, Configuration configuration, DaoManager storage, final ClassLoader loader) throws UnknownHostException{
        super();

        this.system = system;
        this.configuration = configuration;
        this.storage = storage;
        this.loader = loader;

        // Observers
        this.observers = new ArrayList<ActorRef>(1);

        localMediaServerEntity = uploadLocalMediaServersInDataBase();
        bindMGCPStack(localMediaServerEntity.getLocalIpAddress(), localMediaServerEntity.getLocalPort());
        this.localMediaGateway = turnOnMediaGateway(localMediaServerEntity);
        this.mediaGatewayMap = new HashMap<String, ActorRef>();
        mediaGatewayMap.put(localMediaServerEntity.getMsId()+"", localMediaGateway);
    }

    private void bindMGCPStack(String ip, int port) throws UnknownHostException {
        mgcpStack = new JainMgcpStackImpl(InetAddress.getByName(ip), port);
        try {
            mgcpProvider = mgcpStack.createProvider();
        } catch (final CreateProviderException exception) {
            logger.error(exception, "Could not create a JAIN MGCP provider.");
        }
    }

    private ActorRef turnOnMediaGateway(MediaServerEntity mediaServerEntity) throws UnknownHostException {

        final ActorRef gateway = system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                final String classpath = configuration.getString("mgcp-server[@class]");
                return (UntypedActor) new ObjectFactory(loader).getObjectInstance(classpath);
            }
        }));

        final PowerOnMediaGateway.Builder builder = PowerOnMediaGateway.builder();
        builder.setName(configuration.getString("mgcp-server[@name]"));

        if(logger.isInfoEnabled())
            logger.info("turnOnMediaGateway local ip: "+localMediaServerEntity.getLocalIpAddress()+" local port: "+localMediaServerEntity.getLocalPort()
            +" remote ip: "+mediaServerEntity.getRemoteIpAddress()+" remote port: "+mediaServerEntity.getRemotePort());

        builder.setLocalIP(InetAddress.getByName(localMediaServerEntity.getLocalIpAddress()));
        builder.setLocalPort(localMediaServerEntity.getLocalPort());
        builder.setRemoteIP(InetAddress.getByName(mediaServerEntity.getRemoteIpAddress()));
        builder.setRemotePort(mediaServerEntity.getRemotePort());

        if (mediaServerEntity.getExternalAddress() != null) {
            builder.setExternalIP(InetAddress.getByName(mediaServerEntity.getExternalAddress()));
            builder.setUseNat(true);
        } else {
            builder.setUseNat(false);
        }

        builder.setTimeout(Long.parseLong(mediaServerEntity.getResponseTimeout()));
        builder.setStack(mgcpStack);
        builder.setProvider(mgcpProvider);

        final PowerOnMediaGateway powerOn = builder.build();
        gateway.tell(powerOn, null);

        return gateway;
    }

    private ActorRef getConferenceMediaResourceController() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new ConferenceMediaResourceController(localMsId, localMediaGateway, configuration, storage, self());
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
            sender.tell(new MediaResourceBrokerResponse<ActorRef>(getConferenceMediaResourceController()), self);
        }
    }

    private void onGetMediaGateway(GetMediaGateway message, ActorRef self, ActorRef sender) throws Exception {
        // if a specific MS is not asked return local MS.
        if(message.msId() == null){
            final String conferenceName = message.conferenceName();
            final Sid callSid = message.callSid();

            // if its not request for conference return home media-gateway (media-server associated with this RC instance)
            if(conferenceName == null){
                updateMSIdinCallDetailRecord(localMsId, callSid);
                sender.tell(new MediaResourceBrokerResponse<ActorRef>(localMediaGateway), self);
            }else{
                final Sid conferenceSid = addConferenceDetailRecord(conferenceName, callSid);
                sender.tell(new MediaResourceBrokerResponse<MediaGatewayForConference>(new MediaGatewayForConference(conferenceSid, localMediaGateway)), self);
            }
        }else{
            ActorRef remoteMediaGateway;
            // check if this MS is already available
            if(mediaGatewayMap.containsKey(message.msId())){
                remoteMediaGateway = mediaGatewayMap.get(message.msId());
            }else{
                //if not then fetch it from DB and turn it on as well add to local map.
                MediaServersDao dao = storage.getMediaServersDao();
                MediaServerEntity remoteMediaServerEntity = dao.getMediaServer(message.msId());
                remoteMediaGateway = turnOnMediaGateway(remoteMediaServerEntity);
                mediaGatewayMap.put(message.msId(), remoteMediaGateway);
            }
            sender.tell(new MediaResourceBrokerResponse<ActorRef>(remoteMediaGateway), self);
        }
    }

    private void updateMSIdinCallDetailRecord(final String msId, final Sid callSid){
        if(callSid == null){
            logger.error("Call Id is not specisfied");
        }else{
            CallDetailRecordsDao dao = storage.getCallDetailRecordsDao();
            CallDetailRecord cdr = dao.getCallDetailRecord(callSid);
            if(cdr != null){
                cdr = cdr.setMsId(msId);
                dao.updateCallDetailRecord(cdr);
            }else{
                logger.warning("provided call id did not found");
            }
        }

    }

    private Sid addConferenceDetailRecord(final String conferenceName, final Sid callSid) throws Exception {
       Sid sid = null;
        if(conferenceName == null ){
            logger.error("provided conference name is null, this can lead to problems in future of this call");
        }else{
            CallDetailRecordsDao callDao = storage.getCallDetailRecordsDao();
            CallDetailRecord callRecord = callDao.getCallDetailRecord(callSid);
            if(callRecord != null){
                ConferenceDetailRecordsDao dao = storage.getConferenceDetailRecordsDao();

                // check if a conference with same name/account is running.
                final String[] cnfNameAndAccount = conferenceName.split(":");
                final String accountSid = cnfNameAndAccount[0];
                final String friendlyName = cnfNameAndAccount[1];

                ConferenceDetailRecordFilter filter = new ConferenceDetailRecordFilter(accountSid, "RUNNING%", null, null, friendlyName, 1, 0);
                logger.info("ConferenceDetailRecordFilter: "+filter.toString());
                List<ConferenceDetailRecord> records = dao.getConferenceDetailRecords(filter);

                if(records != null && records.size()>0){
                    final ConferenceDetailRecord cdr = records.get(0);
                    sid = cdr.getSid();
                    logger.info("A conference with same name is running. According to database record. given SID is: "+sid);
                }else{
                    // this is first record of this conference on all instances of
                    final ConferenceDetailRecord.Builder conferenceBuilder = ConferenceDetailRecord.builder();
                    sid = Sid.generate(Sid.Type.CONFERENCE);
                    conferenceBuilder.setSid(sid);
                    conferenceBuilder.setDateCreated(DateTime.now());

                    conferenceBuilder.setAccountSid(new Sid(accountSid));
                    conferenceBuilder.setStatus("RUNNING_INITIALIZING");
                    conferenceBuilder.setApiVersion(callRecord.getApiVersion());
                    final StringBuilder UriBuffer = new StringBuilder();
                    UriBuffer.append("/").append(callRecord.getApiVersion()).append("/Accounts/").append(accountSid).append("/Conferences/");
                    UriBuffer.append(sid);
                    final URI uri = URI.create(UriBuffer.toString());
                    conferenceBuilder.setUri(uri);
                    conferenceBuilder.setFriendlyName(friendlyName);
                    conferenceBuilder.setMasterMsId(localMsId);

                    ConferenceDetailRecord cdr = conferenceBuilder.build();
                    dao.addConferenceDetailRecord(cdr);

                    //getting CDR again as it is a conditional insert(select if exists or insert) to handle concurrency (incase another participant joins on another instance at very same time)
                    cdr = dao.getConferenceDetailRecords(filter).get(0);
                    sid = cdr.getSid();
                    logger.info("addConferenceDetailRecord: SID: "+sid+" NAME: "+conferenceName);
                }
            }else{
                logger.error("call record is null");
            }
        }
        return sid;
    }

    private MediaServerEntity uploadLocalMediaServersInDataBase() {
        String localIpAdressForMediaGateway = configuration.getString("mgcp-server.local-address");
        int localPortAdressForMediaGateway = Integer.parseInt(configuration.getString("mgcp-server.local-port"));
        String remoteIpAddress = configuration.getString("mgcp-server.remote-address");
        int remotePort = Integer.parseInt(configuration.getString("mgcp-server.remote-port"));
        String responseTimeout = configuration.getString("mgcp-server.response-timeout");
        String externalAddress = configuration.getString("mgcp-server.external-address");

        final MediaServerEntity.Builder builder = MediaServerEntity.builder();
        builder.setLocalIpAddress(localIpAdressForMediaGateway);
        builder.setLocalPort(localPortAdressForMediaGateway);
        builder.setRemoteIpAddress(remoteIpAddress);
        builder.setRemotePort(remotePort);
        builder.setResponseTimeout(responseTimeout);
        builder.setExternalAddress(externalAddress);

        MediaServersDao dao = storage.getMediaServersDao();
        MediaServerEntity mediaServerEntity = builder.build();
        final List<MediaServerEntity> existingMediaServersForSameIP = dao.getMediaServerEntityByIP(remoteIpAddress);

        if(existingMediaServersForSameIP == null || existingMediaServersForSameIP.size()==0){
            dao.addMediaServer(mediaServerEntity);
            final List<MediaServerEntity> newMediaServerEntity = dao.getMediaServerEntityByIP(remoteIpAddress);
            this.localMsId = newMediaServerEntity.get(0).getMsId()+"";
        }else{
            this.localMsId = existingMediaServersForSameIP.get(0).getMsId()+"";
            mediaServerEntity = mediaServerEntity.setMsId(Integer.parseInt(this.localMsId));
            dao.updateMediaServer(mediaServerEntity);
            if(existingMediaServersForSameIP.size()>1)
                logger.error("in DB: there are multiple media servers registered for same IP address");
        }
        return mediaServerEntity;
    }

    @Override
    public void postStop() {
        // Cleanup resources
        cleanup();

        // Clean observers
        observers.clear();

        // Terminate actor
        system.stop(self());
    }

    protected void cleanup() {}
}