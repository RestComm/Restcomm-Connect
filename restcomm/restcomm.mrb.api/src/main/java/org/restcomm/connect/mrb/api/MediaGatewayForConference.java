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

package org.restcomm.connect.mrb.api;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;

import akka.actor.ActorRef;

/**
 * @author Maria Farooq (maria.farooq@telestax.com)
 */
@Immutable
public final class MediaGatewayForConference {
    private final Sid conferenceSid;
    private final ActorRef mediaGateway;
    private final String masterConfernceEndpointIdName;
    private final boolean isThisMaster;

    public MediaGatewayForConference(final Sid conferenceSid, final ActorRef mediaGateway, final String masterConfernceEndpointIdName, final boolean isThisMaster) {
        super();
        this.conferenceSid = conferenceSid;
        this.mediaGateway = mediaGateway;
        this.masterConfernceEndpointIdName = masterConfernceEndpointIdName;
        this.isThisMaster = isThisMaster;
    }

    public Sid conferenceSid() {
        return conferenceSid;
    }

    public ActorRef mediaGateway() {
        return mediaGateway;
    }

    public String masterConfernceEndpointIdName() {
        return masterConfernceEndpointIdName;
    }

    public boolean isThisMaster() {
        return isThisMaster;
    }

    @Override
    public String toString() {
        return "MediaGatewayForConference [conferenceSid=" + conferenceSid + ", mediaGateway=" + mediaGateway
                + ", masterConfernceEndpointIdName=" + masterConfernceEndpointIdName + ", isThisMaster=" + isThisMaster
                + "]";
    }
}
