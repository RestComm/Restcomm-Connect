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
package org.restcomm.connect.dao.entities;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@Immutable
public final class MediaServerEntity {
    private final int msId;
    private final String compatibility;
    private final String localIpAddress;
    private final int localPort;
    private final String remoteIpAddress;
    private final int remotePort;
    private final String responseTimeout;
    private final String externalAddress;

    public MediaServerEntity(final int msId, final String compatibility,final String localIpAddress, final int localPort,
    final String remoteIpAddress, final int remotePort, final String responseTimeout, final String externalAddress) {
        super();
        this.msId = msId;
        this.compatibility = compatibility;
        this.localIpAddress = localIpAddress;
        this.localPort = localPort;
        this.remoteIpAddress = remoteIpAddress;
        this.remotePort = remotePort;
        this.responseTimeout = responseTimeout;
        this.externalAddress = externalAddress;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMsId() {
        return this.msId;
    }

    public String getCompatibility() {
        return this.compatibility;
    }

    public String getLocalIpAddress() {
        return this.localIpAddress;
    }

    public int getLocalPort() {
        return this.localPort;
    }

    public String getRemoteIpAddress() {
        return this.remoteIpAddress;
    }

    public int getRemotePort() {
        return this.remotePort;
    }

    public String getResponseTimeout() {
        return this.responseTimeout;
    }

    public String getExternalAddress() {
        return this.externalAddress;
    }

    public MediaServerEntity setMsId(int msId) {
        return new MediaServerEntity(msId, compatibility, localIpAddress, localPort, remoteIpAddress, remotePort, responseTimeout, externalAddress);
    }

    @NotThreadSafe
    public static final class Builder {
        private int msId;
        private String compatibility;
        private String localIpAddress;
        private int localPort;
        private String remoteIpAddress;
        private int remotePort;
        private String responseTimeout;
        private String externalAddress;

        private Builder() {
            super();
            compatibility = null;
            localIpAddress = null;
            remoteIpAddress = null;
            responseTimeout = null;
            externalAddress = null;
        }

        public MediaServerEntity build() {
            return new MediaServerEntity(msId, compatibility, localIpAddress, localPort, remoteIpAddress, remotePort, responseTimeout, externalAddress);
        }

        public void setMsId(int msId) {
            this.msId = msId;
        }

        public void setCompatibility(String compatibility) {
            this.compatibility = compatibility;
        }

        public void setLocalIpAddress(String localIpAddress) {
            this.localIpAddress = localIpAddress;
        }

        public void setLocalPort(int localPort) {
            this.localPort = localPort;
        }

        public void setRemoteIpAddress(String remoteIpAddress) {
            this.remoteIpAddress = remoteIpAddress;
        }

        public void setRemotePort(int remotePort) {
            this.remotePort = remotePort;
        }

        public void setResponseTimeout(String responseTimeout) {
            this.responseTimeout = responseTimeout;
        }

        public void setExternalAddress(String externalAddress) {
            this.externalAddress = externalAddress;
        }
    }
}
