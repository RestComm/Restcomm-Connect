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

    public CreateMediaSession(String connectionMode, String sessionDescription, boolean outbound, boolean webrtc, Sid callSid, final String conferenceName) {
        super();
        this.connectionMode = connectionMode;
        this.sessionDescription = sessionDescription;
        this.outbound = outbound;
        this.webrtc = webrtc;
        this.callSid = callSid;
        this.conferenceName = conferenceName;
    }

    public CreateMediaSession(String connectionMode, String sessionDescription, boolean outbound, boolean webrtc, Sid callSid) {
        this(connectionMode, sessionDescription, outbound, webrtc, callSid, null);
    }

    public CreateMediaSession(String connectionMode) {
        this("sendrecv", "", false, false, null, null);
    }

    public CreateMediaSession(Sid callSid, final String conferenceName) {
        this("", "", false, false, callSid, conferenceName);
    }

    public CreateMediaSession(Sid callSid) {
        this("", "", false, false, callSid, null);
    }

    public CreateMediaSession() {
        this("", "", false, false, null, null);
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

}
