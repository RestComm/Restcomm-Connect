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
package org.restcomm.connect.mrb.mock;

import java.net.UnknownHostException;
import java.util.HashMap;

import org.restcomm.connect.mgcp.MediaResourceBrokerResponse;
import org.restcomm.connect.mrb.api.GetConferenceMediaResourceController;
import org.restcomm.connect.mrb.api.GetMediaGateway;
import org.restcomm.connect.mrb.api.StartMediaResourceBroker;
import org.restcomm.connect.mrb.MediaResourceBrokerGeneric;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 *
 */
public class MockMediaResourceBroker extends MediaResourceBrokerGeneric {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    public MockMediaResourceBroker(){
        super();
        if (logger.isInfoEnabled()) {
            logger.info(" ********** MockMediaResourceBroker Constructed.");
        }
    }
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef sender = sender();
        ActorRef self = self();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** MockMediaResourceBroker " + self().path() + " Processing Message: " + klass.getName());
        }
        if (StartMediaResourceBroker.class.equals(klass)) {
            onStartMediaResourceBroker((StartMediaResourceBroker)message, self, sender);
        } else if (GetMediaGateway.class.equals(klass)) {
            onGetMediaGateway((GetMediaGateway) message, self, sender);
        } else if (GetConferenceMediaResourceController.class.equals(klass)){
            sender.tell(new MediaResourceBrokerResponse<ActorRef>(getConferenceMediaResourceController()), self);
        } else if(String.class.equals(klass)){
            //this is for experimental purpose to see how akka actor behave on exceptions..
            //remove try catch to do the experiment..
            //try{
                testExceptionOnRecieve((String)message, self, sender);
            /*}catch(Exception e){
                logger.error("MockMediaResourceBroker onReceive: {}", e);
            }finally{
                sender.tell(message, self);
            }*/
        }
    }

    private void testExceptionOnRecieve(String message, ActorRef self, ActorRef sender){
        String s = null;
        s.equalsIgnoreCase("blabla");
    }

    /* (non-Javadoc)
     * @see org.restcomm.connect.telscale.mrb.MediaResourceBroker#onStartMediaResourceBroker(org.restcomm.connect.mrb.api.StartMediaResourceBroker, akka.actor.ActorRef, akka.actor.ActorRef)
     */
    @Override
    protected void onStartMediaResourceBroker(StartMediaResourceBroker message, ActorRef self, ActorRef sender) throws UnknownHostException{
        this.configuration = message.configuration();
        this.storage = message.storage();
        this.loader = message.loader();
        this.supervisor = message.supervisor();

        localMediaServerEntity = uploadLocalMediaServersInDataBase();
        this.localMediaGateway = turnOnMediaGateway(localMediaServerEntity);
        this.mediaGatewayMap = new HashMap<String, ActorRef>();
        mediaGatewayMap.put(localMediaServerEntity.getMsId()+"", localMediaGateway);
    }

    @Override
    public void postStop() {
        if(logger.isInfoEnabled())
            logger.info("MRB Mock post stop called");
        super.postStop();
    }

}
