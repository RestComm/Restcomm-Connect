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
package org.mobicents.servlet.restcomm.telephony;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author amit.bhayani@telestax.com (Amit Bhayani)
 */
public final class ConferenceStateChanged {
    public static enum State {
        RUNNING_MODERATOR_ABSENT, RUNNING_MODERATOR_PRESENT, COMPLETED, FAILED
    };

    private final String name;
    private final State state;

    public ConferenceStateChanged(final String name, final State state) {
        super();
        this.name = name;
        this.state = state;
    }

    public String name() {
        return name;
    }

    public State state() {
        return state;
    }
}
