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

import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.MediaServersDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.MediaServerEntity;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.mgcp.GetMediaGateway;
import org.mobicents.servlet.restcomm.mgcp.MediaResourceBrokerResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaServerRouter;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MediaResourceBroker extends UntypedActor{

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

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
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        if (GetMediaGateway.class.equals(klass)) {
            getMediaGateway(message, sender);
        }
    }

    private void getMediaGateway(Object message, ActorRef sender) {
        final ActorRef self = self();

        final GetMediaGateway request = (GetMediaGateway) message;
        final boolean conference = request.conference();
        final Sid callSid = request.callSid();
        String msId = null;
        ActorRef mediaGateway = null;

        // if its not request for conference return media-gateway according to algo.
        if(!conference){
            msId = msRouter.getNextMediaServerKey();
            mediaGateway = mediaGatewayMap.get(msId);
            updateMSIdinCallDetailRecord(msId, callSid);
        }else{
            // get the call and see where it is connected and return same msid so call and its conferenceendpoint are on same mediaserver
            msId = getMSIdinCallDetailRecord(callSid);
            mediaGateway = mediaGatewayMap.get(msId);
        }

        sender.tell(new MediaResourceBrokerResponse<ActorRef>(mediaGateway), self);
    }

    private void updateMSIdinCallDetailRecord(String mediaServerId, Sid callSid){
        if(callSid == null){
            logger.warning("Call Id is not specisfied");
        }else{
            CallDetailRecordsDao dao = storage.getCallDetailRecordsDao();
            CallDetailRecord cdr = dao.getCallDetailRecord(callSid);

            cdr.setMsId(mediaServerId);
            dao.updateCallDetailRecord(cdr);
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

}
