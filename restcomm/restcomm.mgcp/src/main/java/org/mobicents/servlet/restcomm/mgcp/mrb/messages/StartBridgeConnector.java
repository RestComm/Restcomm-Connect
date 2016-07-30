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
import org.mobicents.servlet.restcomm.mgcp.MediaSession;

import akka.actor.ActorRef;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@Immutable
public final class StartBridgeConnector {

    private final ActorRef cnfEndpoint;
    private final Sid callSid;
    private final Sid conferenceSid;
    private final String conferenceName;
    private final MediaSession mediaSession;

    public StartBridgeConnector(final ActorRef cnfEndpoint, final Sid callSid, final Sid conferenceSid, final String conferenceName, final MediaSession mediaSession) {
        super();
        this.cnfEndpoint = cnfEndpoint;
        this.callSid = callSid;
        this.conferenceSid = conferenceSid;
        this.conferenceName = conferenceName;
        this.mediaSession = mediaSession;
    }

    public ActorRef cnfEndpoint() {
        return cnfEndpoint;
    }

    public Sid callSid() {
        return callSid;
    }

    public Sid conferenceSid() {
        return conferenceSid;
    }

    public String conferenceName() {
        return conferenceName;
    }

    public MediaSession mediaSession() {
        return mediaSession;
    }
}
