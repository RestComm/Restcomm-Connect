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
package org.mobicents.servlet.restcomm.mscontrol.messages;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class Record {
    private static final List<URI> empty = new ArrayList<URI>(0);

    private final URI destination;
    private final List<URI> prompts;
    private final int timeout;
    private final int length;
    private final String endInputKey;

    public Record(final URI recordingId, final List<URI> prompts, final int timeout, final int length, final String endInputKey) {
        super();
        this.destination = recordingId;
        this.prompts = prompts;
        this.timeout = timeout;
        this.length = length;
        this.endInputKey = endInputKey;
    }

    public Record(final URI recordingId, final int timeout, final int length, final String endInputKey) {
        super();
        this.destination = recordingId;
        this.prompts = empty;
        this.timeout = timeout;
        this.length = length;
        this.endInputKey = endInputKey;
    }

    public URI destination() {
        return destination;
    }

    public List<URI> prompts() {
        return prompts;
    }

    public boolean hasPrompts() {
        return (prompts != null && !prompts.isEmpty());
    }

    public int timeout() {
        return timeout;
    }

    public int length() {
        return length;
    }

    public String endInputKey() {
        return endInputKey;
    }

    public boolean hasEndInputKey() {
        return (endInputKey != null && !endInputKey.isEmpty());
    }
}
