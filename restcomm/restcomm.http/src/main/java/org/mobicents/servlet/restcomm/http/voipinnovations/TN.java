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
package org.mobicents.servlet.restcomm.http.voipinnovations;

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
