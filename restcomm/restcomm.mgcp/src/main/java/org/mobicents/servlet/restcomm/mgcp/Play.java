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
package org.mobicents.servlet.restcomm.mgcp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class Play {
    private final List<URI> announcements;
    private final int iterations;

    public Play(final List<URI> announcements, final int iterations) {
        super();
        this.announcements = announcements;
        this.iterations = iterations;
    }

    public Play(final URI announcement, final int iterations) {
        super();
        this.announcements = new ArrayList<URI>();
        announcements.add(announcement);
        this.iterations = iterations;
    }

    public List<URI> announcements() {
        return announcements;
    }

    public int iterations() {
        return iterations;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        if (!announcements.isEmpty()) {
            buffer.append("an=");
            for (int index = 0; index < announcements.size(); index++) {
                buffer.append(announcements.get(index));
                if (index < announcements.size() - 1) {
                    buffer.append(";");
                }
            }
            if (iterations > 0) {
                buffer.append(" ");
                buffer.append("it=").append(iterations);
            }
        }
        return buffer.toString();
    }
}
