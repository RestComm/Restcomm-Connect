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
package org.restcomm.connect.mgcp;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class OpenLink {
    private final String primaryEndpointId;
    private final String secondaryEndpointId;
    private final ConnectionMode mode;

    public OpenLink(final ConnectionMode mode) {
        this(mode, null, null);
    }

    public OpenLink(final ConnectionMode mode, final String primaryEndpointId, final String secondaryEndpointId) {
        super();
        this.mode = mode;
        this.primaryEndpointId = primaryEndpointId;
        this.secondaryEndpointId = secondaryEndpointId;
    }

    public ConnectionMode mode() {
        return mode;
    }

    public String primaryEndpointId() {
        return primaryEndpointId;
    }

    public String secondaryEndpointId() {
        return secondaryEndpointId;
    }
}
