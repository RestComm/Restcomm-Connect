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
package org.mobicents.servlet.restcomm.util;

import java.util.regex.Pattern;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class IPUtils {
    private static final Pattern LOOPBACK = Pattern.compile("(^127\\.0\\.0\\.1)");
    private static final Pattern NON_ROUTABLE_CLASS_A = Pattern.compile("(^10\\.\\d{0,3}\\.\\d{0,3}\\.\\d{0,3})");
    private static final Pattern NON_ROUTABLE_CLASS_B = Pattern.compile("(^172\\.1[6-9]\\.\\d{0,3}\\.\\d{0,3})|"
            + "(^172\\.2[0-9]\\.\\d{0,3}\\.\\d{0,3})|(^172\\.3[0-1]\\.\\d{0,3}\\.\\d{0,3})");
    private static final Pattern NON_ROUTABLE_CLASS_C = Pattern.compile("(^192\\.168\\.\\d{0,3}\\.\\d{0,3})");

    private IPUtils() {
        super();
    }

    public static boolean isRoutableAddress(final String address) {
        return !(LOOPBACK.matcher(address).matches() || NON_ROUTABLE_CLASS_A.matcher(address).matches()
                || NON_ROUTABLE_CLASS_B.matcher(address).matches() || NON_ROUTABLE_CLASS_C.matcher(address).matches());
    }
}
