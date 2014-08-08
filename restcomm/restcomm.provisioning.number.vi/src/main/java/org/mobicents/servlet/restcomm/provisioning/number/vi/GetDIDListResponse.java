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
package org.mobicents.servlet.restcomm.provisioning.number.vi;

import java.util.List;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author jean.deruelle@telestax.com
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class GetDIDListResponse {
    private final String name;
    private final String status;
    private final int code;
    private final List<State> states;

    public GetDIDListResponse(final String name, final String status, final int code, final List<State> states) {
        super();
        this.name = name;
        this.status = status;
        this.code = code;
        this.states = states;
    }

    public String name() {
        return name;
    }

    public String status() {
        return status;
    }

    public int code() {
        return code;
    }

    public List<State> states() {
        return states;
    }
}
