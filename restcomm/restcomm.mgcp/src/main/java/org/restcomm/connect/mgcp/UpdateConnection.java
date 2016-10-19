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

import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class UpdateConnection {
    private final ConnectionDescriptor descriptor;
    private final ConnectionMode mode;
    private final ConnectionIdentifier connectionIdentifier;

    public UpdateConnection(final ConnectionDescriptor descriptor, final ConnectionMode mode, final ConnectionIdentifier connectionIdentifier) {
        super();
        this.descriptor = descriptor;
        this.mode = mode;
        this.connectionIdentifier = connectionIdentifier;
    }

    public UpdateConnection(final ConnectionDescriptor descriptor, final ConnectionMode mode) {
        this(descriptor, mode, null);
    }

    public UpdateConnection(final ConnectionDescriptor descriptor) {
        this(descriptor, null);
    }

    public UpdateConnection(final ConnectionMode mode) {
        this(null, mode);
    }

    public ConnectionDescriptor descriptor() {
        return descriptor;
    }

    public ConnectionMode mode() {
        return mode;
    }

    public ConnectionIdentifier connectionIdentifier() {
        return connectionIdentifier;
    }
}
