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
package org.restcomm.connect.mrb;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.CreateProviderException;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpStack;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import org.restcomm.connect.commons.loader.ObjectFactory;
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.ConferenceDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.MediaServersDao;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceDetailRecordFilter;
import org.restcomm.connect.dao.entities.MediaServerEntity;
import org.restcomm.connect.mgcp.MediaResourceBrokerResponse;
import org.restcomm.connect.mgcp.PowerOnMediaGateway;
import org.restcomm.connect.mrb.api.GetConferenceMediaResourceController;
import org.restcomm.connect.mrb.api.GetMediaGateway;
import org.restcomm.connect.mrb.api.MediaGatewayForConference;
import org.restcomm.connect.mrb.api.StartMediaResourceBroker;
import org.restcomm.connect.telephony.api.ConferenceStateChanged;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class MediaResourceBrokerGeneric extends RestcommUntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    protected Configuration configuration;
    protected DaoManager storage;
    protected ClassLoader loader;
    protected ActorRef localMediaGateway;
    protected String localMsId;
    protected Map<String, ActorRef> mediaGatewayMap;

    protected JainMgcpStack mgcpStack;
    protected JainMgcpProvider mgcpProvider;

    protected MediaServerEntity localMediaServerEntity;

    public MediaResourceBrokerGeneric(){
        super();
        if (logger.isInfoEnabled()) {
            logger.info(" ********** Community MediaResourceBroker Constructed");
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** MediaResourceBroker " + self().path() + " Processing Message: " + klass.getName());
        }
        if (StartMediaResourceBroker.class.equals(klass)) {
            onStartMediaResourceBroker((StartMediaResourceBroker)message, self, sender);
        } else if (GetMediaGateway.class.equals(klass)) {
            onGetMediaGateway((GetMediaGateway) message, self, sender);
        } else if (GetConferenceMediaResourceController.class.equals(klass)){
            sender.tell(new MediaResourceBrokerResponse<ActorRef>(getConferenceMediaResourceController()), self);
        }
    }

    /**
     * @param message
     * @param self
     * @param sender
     * @throws UnknownHostException
     */
    protected void onStartMediaResourceBroker(StartMediaResourceBroker message, ActorRef self, ActorRef sender) throws UnknownHostException{
        this.configuration = message.configuration();
        this.storage = message.storage();
        this.loader = message.loader();

        localMediaServerEntity = uploadLocalMediaServersInDataBase();
        bindMGCPStack(localMediaServerEntity.getLocalIpAddress(), localMediaServerEntity.getLocalPort());
        this.localMediaGateway = turnOnMediaGateway(localMediaServerEntity);
        this.mediaGatewayMap = new HashMap<String, ActorRef>();
        mediaGatewayMap.put(localMediaServerEntity.getMsId()+"", localMediaGateway);
    }

    /**
     * @param ip
     * @param port
     * @throws UnknownHostException
     */
    protected void bindMGCPStack(String ip, int port) throws UnknownHostException {
        mgcpStack = new JainMgcpStackImpl(InetAddress.getByName(ip), port);
        try {
            mgcpProvider = mgcpStack.createProvider();
        } catch (final CreateProviderException exception) {
            logger.error(exception, "Could not create a JAIN MGCP provider.");
        }
    }


    private ActorRef gateway() {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public UntypedActor create() throws Exception {
                final String classpath = configuration.getString("mgcp-server[@class]");
                return (UntypedActor) new ObjectFactory(loader).getObjectInstance(classpath);
            }
        });
        return getContext().actorOf(props);
    }

    /**
     * @param mediaServerEntity
     * @return
     * @throws UnknownHostException
     */
    protected ActorRef turnOnMediaGateway(MediaServerEntity mediaServerEntity) throws UnknownHostException {

        if (logger.isDebugEnabled()) {
            String mgcpServer = configuration.getString("mgcp-server[@class]");
            logger.debug("Will switch on media gateway: "+mgcpServer);
        }

        ActorRef gateway = gateway();

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

    /**
     * @return ConferenceMediaResourceController Community version actor
     */
    protected ActorRef getConferenceMediaResourceController() {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public UntypedActor create() throws Exception {
                return new ConferenceMediaResourceControllerGeneric(localMediaGateway, configuration, storage, self());
            }
        });
        return getContext().actorOf(props);
    }

    /**
     * @param message     -    GetMediaGateway
     *     -    (callSid: if MediaGateway is required for a call
     *     -    conferenceName: if MediaGateway is required for a conference
     *     -    msId: or if MediaGateway is required for a specific MediaServer in cluster)
     * @param self
     * @param sender
     * @throws Exception
     */
    protected void onGetMediaGateway(GetMediaGateway message, ActorRef self, ActorRef sender) throws Exception {
        if(logger.isDebugEnabled()){
            logger.debug(""+message.toString());
        }
        final String conferenceName = message.conferenceName();
        final Sid callSid = message.callSid();

        // if its not request for conference return home media-gateway (media-server associated with this RC instance)
        if(conferenceName == null){
            updateMSIdinCallDetailRecord(localMsId, callSid);
            sender.tell(new MediaResourceBrokerResponse<ActorRef>(localMediaGateway), self);
        }else{
            final MediaGatewayForConference mgfc = addConferenceDetailRecord(conferenceName, callSid);
            sender.tell(new MediaResourceBrokerResponse<MediaGatewayForConference>(mgfc), self);
        }
    }

    /**
     * @param msId
     * @param callSid
     *
     */
    protected void updateMSIdinCallDetailRecord(final String msId, final Sid callSid){
        if(callSid == null){
            if(logger.isDebugEnabled())
                logger.debug("Call Id is not specisfied, it can be an outbound call.");
        }else{
            CallDetailRecordsDao dao = storage.getCallDetailRecordsDao();
            CallDetailRecord cdr = dao.getCallDetailRecord(callSid);
            if(cdr != null){
                cdr = cdr.setMsId(msId);
                dao.updateCallDetailRecord(cdr);
            }else{
                logger.error("provided call id did not found");
            }
        }

    }

    /**
     * @param conferenceName
     * @param callSid
     * @return
     * @throws Exception
     */
    protected MediaGatewayForConference addConferenceDetailRecord(final String conferenceName, final Sid callSid) throws Exception {
       Sid sid = null;
       MediaGatewayForConference mgc = null;
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
                List<ConferenceDetailRecord> records = dao.getConferenceDetailRecords(filter);
                ConferenceDetailRecord cdr;
                if(records != null && records.size()>0){
                    cdr = records.get(0);
                    sid = cdr.getSid();
                    if(logger.isInfoEnabled())
                        logger.info("A conference with same name is running. According to database record. given SID is: "+sid);
                }else{
                    // this is first record of this conference on all instances of
                    addNewConferenceRecord(accountSid, callRecord, friendlyName);

                    //getting CDR again as it is a conditional insert(select if exists or insert) to handle concurrency (incase another participant joins on another instance at very same time)
                    cdr = dao.getConferenceDetailRecords(filter).get(0);
                    sid = cdr.getSid();
                    if(logger.isInfoEnabled())
                        logger.info("addConferenceDetailRecord: SID: "+sid+" NAME: "+conferenceName);
                }
                mgc = new MediaGatewayForConference(sid, localMediaGateway, null, false);
            }else{
                logger.error("call record is null");
            }
        }
        return mgc;
    }

    /**
     * addNewConferenceRecord
     * @param accountSid
     * @param callRecord
     * @param friendlyName
     */
    protected void addNewConferenceRecord(String accountSid, CallDetailRecord callRecord, String friendlyName){
        final ConferenceDetailRecord.Builder conferenceBuilder = ConferenceDetailRecord.builder();
        Sid sid = Sid.generate(Sid.Type.CONFERENCE);
        conferenceBuilder.setSid(sid);
        conferenceBuilder.setDateCreated(DateTime.now());

        conferenceBuilder.setAccountSid(new Sid(accountSid));
        conferenceBuilder.setStatus(ConferenceStateChanged.State.RUNNING_INITIALIZING+"");
        conferenceBuilder.setApiVersion(callRecord.getApiVersion());
        final StringBuilder UriBuffer = new StringBuilder();
        UriBuffer.append("/").append(callRecord.getApiVersion()).append("/Accounts/").append(accountSid).append("/Conferences/");
        UriBuffer.append(sid);
        final URI uri = URI.create(UriBuffer.toString());
        conferenceBuilder.setUri(uri);
        conferenceBuilder.setFriendlyName(friendlyName);
        conferenceBuilder.setMasterMsId(localMsId);

        ConferenceDetailRecord cdr = conferenceBuilder.build();
        storage.getConferenceDetailRecordsDao().addConferenceDetailRecord(cdr);
    }

    /**
     * @return
     */
    protected MediaServerEntity uploadLocalMediaServersInDataBase() {
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
        if(logger.isInfoEnabled())
            logger.info("MRB on post stop");
        // Cleanup resources
        cleanup();

        // Terminate actor
        getContext().stop(self());
    }

    protected void cleanup() {
        try {
            if (mgcpStack != null){
                mgcpStack = null;
            }
            mediaGatewayMap = null;
        } catch (Exception e) {
            logger.error("Exception is cleanup: ", e);
        }
    }
}
