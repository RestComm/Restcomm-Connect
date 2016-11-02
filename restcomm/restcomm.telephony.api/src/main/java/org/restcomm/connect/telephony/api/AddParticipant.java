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

import akka.actor.ActorRef;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@Immutable
public final class AddParticipant {
    private final ActorRef call;
    private final boolean startConferenceOnEnter;
    private final boolean endConferenceOnExit;
    private final boolean beep;

    public AddParticipant(final ActorRef call, final boolean startConferenceOnEnter, final boolean endConferenceOnExit, final boolean beep) {
        super();
        this.call = call;
        this.startConferenceOnEnter = startConferenceOnEnter;
        this.endConferenceOnExit = endConferenceOnExit;
        this.beep = beep;
    }

    public ActorRef call() {
        return call;
    }

    public boolean startConferenceOnEnter() {
        return startConferenceOnEnter;
    }

    public boolean endConferenceOnExit() {
        return endConferenceOnExit;
    }

    public boolean beep() {
        return beep;
    }
}
