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
public final class MediaResourceBrokerEntity {
    private final Sid conferenceSid;
    private String slaveMsId;
    private String slaveMsBridgeEpId;
    private String slaveMsCnfEpId;
    private boolean isBridgedTogether;
    private String slaveMsSDP;

    public MediaResourceBrokerEntity(final Sid conferenceSid, final String slaveMsId, final String slaveMsBridgeEpId,
        final String slaveMsCnfEpId, final Boolean isBridgedTogether, String slaveMsSDP) {
        super();
        this.conferenceSid = conferenceSid;
        this.slaveMsId = slaveMsId;
        this.slaveMsBridgeEpId = slaveMsBridgeEpId;
        this.slaveMsCnfEpId = slaveMsCnfEpId;
        this.isBridgedTogether = isBridgedTogether;
        this.slaveMsSDP = slaveMsSDP;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getConferenceSid() {
        return conferenceSid;
    }

    public String getSlaveMsId() {
        return slaveMsId;
    }

    public String getSlaveMsBridgeEpId() {
        return slaveMsBridgeEpId;
    }

    public String getSlaveMsCnfEpId() {
        return slaveMsCnfEpId;
    }

    public boolean isBridgedTogether() {
        return isBridgedTogether;
    }

    public String getSlaveMsSDP() {
        return slaveMsSDP;
    }

    public MediaResourceBrokerEntity setSlaveMsId(String slaveMsId) {
        return new MediaResourceBrokerEntity(conferenceSid, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether, slaveMsSDP);
    }

    public MediaResourceBrokerEntity setSlaveMsBridgeEpId(String slaveMsBridgeEpId) {
        return new MediaResourceBrokerEntity(conferenceSid, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether, slaveMsSDP);
    }

    public MediaResourceBrokerEntity setSlaveMsCnfEpId(String slaveMsCnfEpId) {
        return new MediaResourceBrokerEntity(conferenceSid, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether, slaveMsSDP);
    }

    public MediaResourceBrokerEntity setBridgedTogether(boolean isBridgedTogether) {
        return new MediaResourceBrokerEntity(conferenceSid, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether, slaveMsSDP);
    }

    public MediaResourceBrokerEntity setSlaveMsSDP(String slaveMsSDP) {
        return new MediaResourceBrokerEntity(conferenceSid, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether, slaveMsSDP);
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid conferenceSid;
        private String slaveMsId;
        private String slaveMsBridgeEpId;
        private String slaveMsCnfEpId;
        private boolean isBridgedTogether;
        private String slaveMsSDP;

        private Builder() {
            super();
            conferenceSid = null;
            slaveMsId = null;
            slaveMsBridgeEpId = null;
            slaveMsCnfEpId = null;
            slaveMsSDP = null;
        }

        public MediaResourceBrokerEntity build() {
            return new MediaResourceBrokerEntity(conferenceSid, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether, slaveMsSDP);
        }

        public void setConferenceSid(Sid conferenceSid) {
            this.conferenceSid = conferenceSid;
        }

        public void setSlaveMsId(String slaveMsId) {
            this.slaveMsId = slaveMsId;
        }

        public void setSlaveMsBridgeEpId(String slaveMsBridgeEpId) {
            this.slaveMsBridgeEpId = slaveMsBridgeEpId;
        }

        public void setSlaveMsCnfEpId(String slaveMsCnfEpId) {
            this.slaveMsCnfEpId = slaveMsCnfEpId;
        }

        public void setBridgedTogether(boolean isBridgedTogether) {
            this.isBridgedTogether = isBridgedTogether;
        }

        public void setSlaveMsSDP(String slaveMsSDP) {
            this.slaveMsSDP = slaveMsSDP;
        }
    }
}
