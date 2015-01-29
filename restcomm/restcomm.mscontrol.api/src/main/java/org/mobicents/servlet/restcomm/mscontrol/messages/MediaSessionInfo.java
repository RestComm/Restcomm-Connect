/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

package org.mobicents.servlet.restcomm.mscontrol.messages;

import java.net.InetAddress;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@Immutable
public final class MediaSessionInfo {

    private final boolean useNat;
    private final InetAddress externalAddress;
    private final String localSdp;
    private final String remoteSdp;

    public MediaSessionInfo() {
        this(false, null, "", "");
    }

    public MediaSessionInfo(boolean useNat, InetAddress externalAddress, String localSdp, String remoteSdp) {
        super();
        this.useNat = useNat;
        this.externalAddress = externalAddress;
        this.localSdp = localSdp;
        this.remoteSdp = remoteSdp;
    }

    public boolean usesNat() {
        return this.useNat;
    }

    public InetAddress getExternalAddress() {
        return externalAddress;
    }

    public String getLocalSdp() {
        return localSdp;
    }

    public String getRemoteSdp() {
        return remoteSdp;
    }

}
