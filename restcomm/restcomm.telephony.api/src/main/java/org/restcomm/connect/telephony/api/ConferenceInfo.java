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
package org.restcomm.connect.telephony.api;

import java.util.List;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;

import akka.actor.ActorRef;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class ConferenceInfo {

    private final Sid sid;
    private final List<ActorRef> participants;
    private final ConferenceStateChanged.State state;
    private final String name;
    private final boolean moderatorPresent;
    private final int globalParticipants;

    public ConferenceInfo(final Sid sid, final List<ActorRef> participants, final ConferenceStateChanged.State state,
            final String name, final boolean moderatorPresent, final int globalParticipants) {

        super();
        this.sid = sid;
        this.participants = participants;
        this.state = state;
        this.name = name;
        this.moderatorPresent = moderatorPresent;
        this.globalParticipants = globalParticipants;
    }

    public List<ActorRef> participants() {
        return participants;
    }

    public ConferenceStateChanged.State state() {
        return state;
    }

    public String name() {
        return name;
    }

    public Sid sid() {
        return sid;
    }

    public boolean isModeratorPresent() {
        return moderatorPresent;
    }

    public int globalParticipants() {
        return globalParticipants;
    }
}
