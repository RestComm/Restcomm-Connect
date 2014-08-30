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

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * <p>
 * Tell's {@link Conference} that moderator has not yet joined and it should Transition from
 * {@link ConferenceStateChanged#State.RUNNING} to {@link ConferenceStateChanged#State.RUNNING_MODERATOR_PRESENT} or from {@link ConferenceStateChanged#State.RUNNING_MODERATOR_ABSENT} to
 * {@link ConferenceStateChanged#State.RUNNING_MODERATOR_PRESENT}
 * </p>
 *
 * @author Amit Bhayani
 *
 */
@Immutable
public class ConferenceModeratorPresent {

    public ConferenceModeratorPresent() {
        super();
    }

}
