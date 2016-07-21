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
package org.mobicents.servlet.restcomm.entities;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@Immutable
public final class MediaServerEntity {
    private final String msId;
    private final String msIpAddress;
    private final String msPort;
    private final String compatibility;
    private final String timeOut;

    public MediaServerEntity(final String msId, final String msIpAddress, final String msPort,
            final String compatibility, final String timeOut) {
        super();
        this.msId = msId;
        this.msIpAddress = msIpAddress;
        this.msPort = msPort;
        this.compatibility = compatibility;
        this.timeOut = timeOut;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMsId() {
        return msId;
    }

    public String getMsIpAddress() {
        return msIpAddress;
    }

    public String getMsPort() {
        return msPort;
    }

    public String getCompatibility() {
        return compatibility;
    }

    public String getTimeOut() {
        return timeOut;
    }

    @NotThreadSafe
    public static final class Builder {
        private String msId;
        private String msIpAddress;
        private String msPort;
        private String compatibility;
        private String timeOut;

        private Builder() {
            super();
            msId = null;
            msIpAddress = null;
            msPort = null;
            compatibility = null;
            timeOut = null;
        }

        public MediaServerEntity build() {
            return new MediaServerEntity(msId, msIpAddress, msPort, compatibility, timeOut);
        }

        public void setMsId(String msId) {
            this.msId = msId;
        }

        public void setMsIpAddress(String msIpAddress) {
            this.msIpAddress = msIpAddress;
        }

        public void setMsPort(String msPort) {
            this.msPort = msPort;
        }

        public void setCompatibility(String compatibility) {
            this.compatibility = compatibility;
        }

        public void setTimeOut(String timeOut) {
            this.timeOut = timeOut;
        }
    }
}
