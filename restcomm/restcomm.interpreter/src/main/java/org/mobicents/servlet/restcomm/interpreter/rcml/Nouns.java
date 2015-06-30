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
        return SIP.equals(name);
    }
}
