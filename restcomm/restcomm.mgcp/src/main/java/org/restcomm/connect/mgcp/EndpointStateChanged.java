/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

package org.restcomm.connect.mgcp;

import org.apache.http.annotation.Immutable;

/**
 * Message broadcast by an MGCP Endpoint to all observers.<br>
 * Occurs when the state of the endpoint changes.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@Immutable
public class EndpointStateChanged {

    private final EndpointState state;

    public EndpointStateChanged(EndpointState state) {
        this.state = state;
    }

    public EndpointState getState() {
        return state;
    }

}
