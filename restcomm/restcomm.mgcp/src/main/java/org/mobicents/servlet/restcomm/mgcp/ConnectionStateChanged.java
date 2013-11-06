/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.mgcp;

import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class ConnectionStateChanged {
    public static enum State {
        CLOSED, HALF_OPEN, OPEN
    };

    private final ConnectionDescriptor descriptor;
    private final State state;

    public ConnectionStateChanged(final ConnectionDescriptor descriptor, final State state) {
        super();
        this.descriptor = descriptor;
        this.state = state;
    }

    public ConnectionStateChanged(final State state) {
        this(null, state);
    }

    public ConnectionDescriptor descriptor() {
        return descriptor;
    }

    public State state() {
        return state;
    }
}
