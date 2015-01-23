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
public final class Play {
    private final List<URI> uris;
    private final int iterations;

    public Play(final List<URI> uris, final int iterations) {
        super();
        this.uris = uris;
        this.iterations = iterations;
    }

    public Play(final URI uri, final int iterations) {
        super();
        this.uris = new ArrayList<URI>();
        uris.add(uri);
        this.iterations = iterations;
    }

    public List<URI> uris() {
        return uris;
    }

    public int iterations() {
        return iterations;
    }
}
