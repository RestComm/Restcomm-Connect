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
package org.mobicents.servlet.restcomm.mgcp.routing;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

@ThreadSafe
public class SimpleRoundRobin {

    private final int numberOfMediaServers;
    private int round;
    private static SimpleRoundRobin robin=null;

    //Singleton
    private SimpleRoundRobin(final int numberOfMediaServers){
        this.numberOfMediaServers = numberOfMediaServers;
        round = 0;
    }

    public static SimpleRoundRobin getInstance(final int numberOfMediaServers){
        if(robin == null){
            robin = new SimpleRoundRobin(numberOfMediaServers);
            return robin;
        }
        return robin;
    }

    public int getNextMediaServerIndex(){
        return getRound();
    }

    private int getRound(){
        if(round >= numberOfMediaServers){
            this.round=1;
            return 0;
        }else{
            return this.round++;
        }
    }

}
