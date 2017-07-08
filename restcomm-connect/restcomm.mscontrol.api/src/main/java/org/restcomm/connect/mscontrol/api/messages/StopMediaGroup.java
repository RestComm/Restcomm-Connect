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
package org.restcomm.connect.mscontrol.api.messages;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author Maria Farooq
 */
@Immutable
public final class StopMediaGroup {
    private boolean liveCallModification;

    private Boolean beep;

    /**
     * @param beep - If a beep will be played in that case we don't need to send EndSignal(StopMediaGroup)
     * to media-server as media-server will automatically stop beep when it will receive
     * play command for beep. If a beep wont be played, then conference need to send
     * EndSignal(StopMediaGroup) to media-server to stop ongoing music-on-hold.
     * https://github.com/RestComm/Restcomm-Connect/issues/2024
     * this is applicable only for restcomm mediaserver
     */
    public StopMediaGroup(final Boolean beep) {
         super();
        this.beep = beep;
    }

    public StopMediaGroup() {
        super();
    }

    public StopMediaGroup(final boolean liveCallModification) {
        this.liveCallModification = liveCallModification;
    }

    public boolean isLiveCallModification() {
        return liveCallModification;
    }

    public Boolean beep() {
        return beep;
    }
}
