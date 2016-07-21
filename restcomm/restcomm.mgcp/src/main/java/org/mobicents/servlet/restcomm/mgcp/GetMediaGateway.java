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

package org.mobicents.servlet.restcomm.mgcp;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author Maria Farooq (maria.farooq@telestax.com)
 */
@Immutable
public final class GetMediaGateway {
    private final Sid callSid;
    private final String mediaServerPreference;
    private final boolean conference;
    private final String conferenceName;

    public GetMediaGateway(final Sid sid, final String mediaServerPreference, final boolean conference, final String conferenceName) {
        super();
        this.callSid = sid;
        this.mediaServerPreference = mediaServerPreference;
        this.conference = conference;
        this.conferenceName = conferenceName;
    }

    public GetMediaGateway(final Sid sid, final String mediaServerPreference){
        this(sid, mediaServerPreference, false, null);
    }

    public GetMediaGateway(final Sid callSid){
        this(callSid, null, false, null);
    }

    public Sid callSid() {
        return callSid;
    }

    public String mediaServerPreference() {
        return mediaServerPreference;
    }

    public boolean conference() {
        return conference;
    }

    public String conferenceName() {
        return conferenceName;
    }

}
