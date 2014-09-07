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
package org.mobicents.servlet.restcomm.provisioning.number.vi;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class TN {
    private final String tier;
    private final boolean t38;
    private final boolean cnam;
    private final String number;

    public TN(final String tier, final boolean t38, final boolean cnam, final String number) {
        super();
        this.tier = tier;
        this.t38 = t38;
        this.cnam = cnam;
        this.number = number;
    }

    public String tier() {
        return tier;
    }

    public boolean t38() {
        return t38;
    }

    public boolean cnam() {
        return cnam;
    }

    public String number() {
        return number;
    }
}
