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
package org.mobicents.servlet.restcomm.mgcp;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.mgcp.routing.SimpleRoundRobin;

import akka.actor.ActorRef;

/**
 * @author Maria Farooq (maria.farooq@live.com)
 */
@Immutable
public final class MediaGateways {
    public static enum Algorithm {
        ROUNDROBIN, CUSTOM
    };

    private final List<ActorRef> mediaGateways;
    private Algorithm algorithm;

    //maybe make this class singleton
    public MediaGateways(final List<ActorRef> mediaGateways, Configuration configuration) {
        super();
        this.mediaGateways = mediaGateways;

        if(configuration == null || configuration.getString("algorithm") == null || configuration.getString("algorithm").equalsIgnoreCase("roundrobin")){
            this.algorithm = Algorithm.ROUNDROBIN;
        }else{
            this.algorithm = Algorithm.CUSTOM;
        }
    }

    public ActorRef getMediaGateway() {
        if(algorithm == Algorithm.ROUNDROBIN){
            return getMediaGatewayViaRoundRobin();
        }else{
            return getMediaGatewayViaCustomeAlgo();
        }
    }

    private ActorRef getMediaGatewayViaRoundRobin(){
        if(!mediaGateways.isEmpty()){
            return mediaGateways.get(SimpleRoundRobin.getInstance(mediaGateways.size()).getNextMediaGatewayIndex());
        }
        return null;
    }

    private ActorRef getMediaGatewayViaCustomeAlgo(){
        //NOT SUPPORTED FOR NOW
        return getMediaGatewayViaRoundRobin();
    }

}
