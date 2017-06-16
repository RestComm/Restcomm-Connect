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

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * <p>
 * Tell's {@link Conference} that moderator has not yet joined and it should Transition from
 * {@link ConferenceStateChanged#State.RUNNING} to {@link ConferenceStateChanged#State.RUNNING_MODERATOR_PRESENT} or from {@link ConferenceStateChanged#State.RUNNING_MODERATOR_ABSENT} to
 * {@link ConferenceStateChanged#State.RUNNING_MODERATOR_PRESENT}
 * </p>
 *
 * @author Amit Bhayani
 * @author Maria Farooq
 */
@Immutable
public class ConferenceModeratorPresent {

    private final Boolean beep;

    /**
     * @param beep - If a beep will be played in that case we don't need to send EndSignal(StopMediaGroup)
     * to media-server as media-server will automatically stop beep when it will receive
     * play command for beep. If a beep wont be played, then conference need to send
     * EndSignal(StopMediaGroup) to media-server to stop ongoing music-on-hold.
     * https://github.com/RestComm/Restcomm-Connect/issues/2024
     * this is applicable only for restcomm mediaserver
     */
    public ConferenceModeratorPresent(final Boolean beep) {
         super();
        this.beep = beep;
    }

    public Boolean beep() {
        return beep;
    }

}
