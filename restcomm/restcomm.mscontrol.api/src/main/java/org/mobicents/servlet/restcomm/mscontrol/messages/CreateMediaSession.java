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

package org.mobicents.servlet.restcomm.mscontrol.messages;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.telephony.ConferenceInfo;

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
    private final ConferenceInfo conferenceInfo;

    public CreateMediaSession(String connectionMode, String sessionDescription, boolean outbound, boolean webrtc, Sid callSid, final ConferenceInfo conferenceInfo) {
        super();
        this.connectionMode = connectionMode;
        this.sessionDescription = sessionDescription;
        this.outbound = outbound;
        this.webrtc = webrtc;
        this.callSid = callSid;
        this.conferenceInfo = conferenceInfo;
    }

    public CreateMediaSession(String connectionMode, String sessionDescription, boolean outbound, boolean webrtc, Sid callSid) {
        this(connectionMode, sessionDescription, outbound, webrtc, callSid, null);
    }

    public CreateMediaSession(String connectionMode) {
        this("sendrecv", "", false, false, null, null);
    }

    public CreateMediaSession(Sid callSid, final ConferenceInfo conferenceInfo) {
        this("", "", false, false, callSid, conferenceInfo);
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

    public ConferenceInfo conferenceInfo() {
        return conferenceInfo;
    }

}
