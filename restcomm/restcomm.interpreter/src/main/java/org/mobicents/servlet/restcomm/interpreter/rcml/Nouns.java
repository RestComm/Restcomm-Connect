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
package org.mobicents.servlet.restcomm.interpreter.rcml;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com (Jean Deruelle)
 */
@Immutable
public final class Nouns {
    public static final String client = "Client";
    public static final String conference = "Conference";
    public static final String number = "Number";
    public static final String queue = "Queue";
    public static final String uri = "Uri";
    // https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
    public static final String SIP = "Sip";

    public Nouns() {
        super();
    }

    public static boolean isNoun(final String name) {
        if (client.equals(name))
            return true;
        if (conference.equals(name))
            return true;
        if (number.equals(name))
            return true;
        if (queue.equals(name))
            return true;
        if (uri.equals(name))
            return true;
        if (SIP.equals(name))
            return true;
        return false;
    }
}
