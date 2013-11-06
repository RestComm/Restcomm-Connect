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
package org.mobicents.servlet.restcomm.mgcp;

import java.net.InetAddress;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class MediaGatewayInfo {
    private final String name;
    // Server Info.
    final InetAddress ip;
    final int port;
    // Used for NAT traversal.
    private final boolean useNat;
    private final InetAddress externalIp;

    public MediaGatewayInfo(final String name, final InetAddress ip, final int port, final boolean useNat,
            final InetAddress externalIp) {
        super();
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.useNat = useNat;
        this.externalIp = externalIp;
    }

    public String name() {
        return name;
    }

    public InetAddress ip() {
        return ip;
    }

    public int port() {
        return port;
    }

    public boolean useNat() {
        return useNat;
    }

    public InetAddress externalIP() {
        return externalIp;
    }
}
