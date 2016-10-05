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
package org.restcomm.connect.mgcp;

import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.restcomm.connect.mgcp.routing.SimpleRoundRobin;

import akka.actor.ActorRef;

/**
 * @author Maria Farooq (maria.farooq@live.com)
 */
@Immutable
public final class MediaServerRouter {
    public static enum Algorithm {
        ROUNDROBIN, CUSTOM
    };

    private final String[] mediaServerKeys;
    private final Map<String, ActorRef> mediaServerMap;
    private Algorithm algorithm;

    //maybe make this class singleton
    public MediaServerRouter(final Map<String, ActorRef> ServerMap, Configuration configuration) {
        super();
        this.mediaServerMap = ServerMap;
        this.mediaServerKeys = ServerMap.keySet().toArray(new String[0]);

        if(configuration == null || configuration.getString("algorithm") == null || configuration.getString("algorithm").equalsIgnoreCase("roundrobin")){
            this.algorithm = Algorithm.ROUNDROBIN;
        }else{
            this.algorithm = Algorithm.CUSTOM;
        }
    }

    public String getNextMediaServerKey() {
        if(algorithm == Algorithm.ROUNDROBIN){
            return getMediaServerViaRoundRobin();
        }else{
            return getMediaServerViaCustomeAlgo();
        }
    }

    private String getMediaServerViaRoundRobin(){
        if(mediaServerKeys !=null && mediaServerKeys.length>0){
            int roundIndex = SimpleRoundRobin.getInstance(mediaServerKeys.length).getNextMediaServerIndex();
            return mediaServerKeys[roundIndex];
        }
        return null;
    }

    private String getMediaServerViaCustomeAlgo(){
        //NOT SUPPORTED FOR NOW
        return getMediaServerViaRoundRobin();
    }

}
