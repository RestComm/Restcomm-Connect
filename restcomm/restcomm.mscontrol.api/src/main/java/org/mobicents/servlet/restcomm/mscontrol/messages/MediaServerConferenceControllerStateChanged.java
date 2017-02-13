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

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.mscontrol.api.messages.MediaServerControllerStateChanged;
import org.restcomm.connect.mscontrol.api.messages.MediaSessionInfo;

/**
 * @author Maria
 *
 */
@Immutable
public final class MediaServerConferenceControllerStateChanged extends MediaServerControllerStateChanged{

    private final Sid conferenceSid;
    private final String conferenceState;
    private final boolean moderatorPresent;

    public MediaServerConferenceControllerStateChanged(final MediaServerControllerState state, MediaSessionInfo mediaSession, final Sid conferenceSid, final String conferenceState, final boolean moderatorPresent) {
        super(state, mediaSession);
        this.conferenceSid = conferenceSid;
        this.conferenceState = conferenceState;
        this.moderatorPresent = moderatorPresent;
    }

    public MediaServerConferenceControllerStateChanged(final MediaServerControllerState state, MediaSessionInfo mediaSession, final Sid conferenceSid) {
        this(state, mediaSession, conferenceSid, null, false);
    }

    public MediaServerConferenceControllerStateChanged(final MediaServerControllerState state, final Sid conferenceSid, final String conferenceState) {
        this(state, null, conferenceSid, conferenceState, false);
    }

    public MediaServerConferenceControllerStateChanged(final MediaServerControllerState state, final Sid conferenceSid, final String conferenceState, final boolean moderatorPresent) {
        this(state, null, conferenceSid, conferenceState, moderatorPresent);
    }

    public MediaServerConferenceControllerStateChanged(final MediaServerControllerState state, final Sid conferenceSid) {
        this(state, null, conferenceSid, null, false);
    }

    public Sid conferenceSid() {
        return conferenceSid;
    }

    public String conferenceState() {
        return conferenceState;
    }

    public boolean moderatorPresent (){
        return moderatorPresent;
    }
}
