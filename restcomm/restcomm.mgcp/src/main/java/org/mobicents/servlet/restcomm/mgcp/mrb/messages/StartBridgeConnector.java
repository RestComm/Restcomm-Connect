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
package org.mobicents.servlet.restcomm.mgcp.mrb.messages;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.entities.Sid;

import akka.actor.ActorRef;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@Immutable
public final class StartBridgeConnector {

    private final ActorRef cnfEndpoint;
    private final Sid conferenceSid;
    private final String conferenceName;
    private ConnectionMode connectionMode;

    public StartBridgeConnector(final ActorRef cnfEndpoint, final Sid conferenceSid, final String conferenceName, final ConnectionMode connectionMode) {
        super();
        this.cnfEndpoint = cnfEndpoint;
        this.conferenceSid = conferenceSid;
        this.conferenceName = conferenceName;
        this.connectionMode = connectionMode;
    }

    public ActorRef cnfEndpoint() {
        return cnfEndpoint;
    }

    public Sid conferenceSid() {
        return conferenceSid;
    }

    public String conferenceName() {
        return conferenceName;
    }

    public ConnectionMode connectionMode() {
        return connectionMode;
    }
}
