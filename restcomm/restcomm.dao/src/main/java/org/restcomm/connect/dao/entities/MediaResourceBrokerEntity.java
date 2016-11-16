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
import org.restcomm.connect.commons.dao.Sid;

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

    public MediaResourceBrokerEntity(final Sid conferenceSid, final String slaveMsId, final String slaveMsBridgeEpId,
        final String slaveMsCnfEpId, final Boolean isBridgedTogether) {
        super();
        this.conferenceSid = conferenceSid;
        this.slaveMsId = slaveMsId;
        this.slaveMsBridgeEpId = slaveMsBridgeEpId;
        this.slaveMsCnfEpId = slaveMsCnfEpId;
        this.isBridgedTogether = isBridgedTogether;
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

    @NotThreadSafe
    public static final class Builder {
        private Sid conferenceSid;
        private String slaveMsId;
        private String slaveMsBridgeEpId;
        private String slaveMsCnfEpId;
        private boolean isBridgedTogether;

        private Builder() {
            super();
            conferenceSid = null;
            slaveMsId = null;
            slaveMsBridgeEpId = null;
            slaveMsCnfEpId = null;
        }

        public MediaResourceBrokerEntity build() {
            return new MediaResourceBrokerEntity(conferenceSid, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether);
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
    }
}
