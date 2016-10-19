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

import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class CreateLink extends AbstractCreateMessage {
    private ConnectionIdentifier connectionIdentifier;
    public CreateLink(final MediaSession session) {
        this(session, null);
    }
    public CreateLink(final MediaSession session, ConnectionIdentifier connectionIdentifier) {
        super(session);
        this.connectionIdentifier = connectionIdentifier;
    }

    public ConnectionIdentifier connectionIdentifier(){
        return connectionIdentifier;
    }
}