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
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class Hangup {
    private String message;
    private Integer sipResponse;

    public Hangup() {
        super();
    }

    public Hangup(final String message, final Integer sipResponse) {
        this.message = message;
        this.sipResponse = sipResponse;
    }

    public Hangup(final String message) {
        this.message = message;
    }

    public Hangup(final Integer sipResponse) {
        this.sipResponse = sipResponse;
    }

    public String getMessage() {
        return message;
    }

    public Integer getSipResponse() {
        return sipResponse;
    }

    @Override
    public String toString() {
        return "Hangup [message=" + message + ", sipResponse=" + sipResponse + "]";
    }
}
