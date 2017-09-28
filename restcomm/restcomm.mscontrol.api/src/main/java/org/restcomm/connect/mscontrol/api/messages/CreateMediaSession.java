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

package org.restcomm.connect.mscontrol.api.messages;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.MediaAttributes;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
@Immutable
public final class CreateMediaSession {

    private final boolean outbound;
    private final String connectionMode;
    private final String sessionDescription;
    private final boolean webrtc;
    private final Sid callSid;
    private final String conferenceName;
    private final MediaAttributes mediaAttributes;

    public CreateMediaSession(String connectionMode, String sessionDescription, boolean outbound, boolean webrtc, Sid callSid, final String conferenceName, final MediaAttributes mediaAttributes) {
        super();
        this.connectionMode = connectionMode;
        this.sessionDescription = sessionDescription;
        this.outbound = outbound;
        this.webrtc = webrtc;
        this.callSid = callSid;
        this.conferenceName = conferenceName;
        this.mediaAttributes = mediaAttributes;
    }

    public CreateMediaSession(String connectionMode, String sessionDescription, boolean outbound, boolean webrtc, Sid callSid) {
        this(connectionMode, sessionDescription, outbound, webrtc, callSid, new MediaAttributes());
    }

    public CreateMediaSession(String connectionMode, String sessionDescription, boolean outbound, boolean webrtc, Sid callSid, final MediaAttributes mediaAttributes) {
        this(connectionMode, sessionDescription, outbound, webrtc, callSid, null, mediaAttributes);
    }

    public CreateMediaSession(String connectionMode) {
        this("sendrecv", "", false, false, null, null, new MediaAttributes());
    }

    public CreateMediaSession(Sid callSid, final String conferenceName) {
        this("", "", false, false, callSid, conferenceName, new MediaAttributes());
    }

    public CreateMediaSession(Sid callSid) {
        this("", "", false, false, callSid, null, new MediaAttributes());
    }

    public CreateMediaSession() {
        this("", "", false, false, null, null, new MediaAttributes());
    }

    public String getConnectionMode() {
        return connectionMode;
    }

    public String getSessionDescription() {
        return sessionDescription;
    }

    public boolean isOutbound() {
        return outbound;
    }

    public boolean isWebrtc() {
        return webrtc;
    }

    public Sid callSid() {
        return callSid;
    }

    public String conferenceName() {
        return conferenceName;
    }

    public MediaAttributes mediaAttributes() { return mediaAttributes; }
}
