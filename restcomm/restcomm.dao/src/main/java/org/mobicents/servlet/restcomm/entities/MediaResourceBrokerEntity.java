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
    private final Sid callSid;
    private final Sid conferenceSid;
    private String masterMsId;
    private String masterMsBridgeEpId;
    private String masterMsCnfEpId;
    private String slaveMsId;
    private String slaveMsBridgeEpId;
    private String slaveMsCnfEpId;
    private boolean isBridgedTogether;

    public MediaResourceBrokerEntity(final Sid callSid, final Sid conferenceSid, final String masterMsId, final String masterMsBridgeEpId, final String masterMsCnfEpId,
    final String slaveMsId, final String slaveMsBridgeEpId, final String slaveMsCnfEpId, final Boolean isBridgedTogether) {
        super();
        this.callSid = callSid;
        this.conferenceSid = conferenceSid;
        this.masterMsId = masterMsId;
        this.masterMsBridgeEpId = masterMsBridgeEpId;
        this.masterMsCnfEpId = masterMsCnfEpId;
        this.slaveMsId = slaveMsId;
        this.slaveMsBridgeEpId = slaveMsBridgeEpId;
        this.slaveMsCnfEpId = slaveMsCnfEpId;
        this.isBridgedTogether = isBridgedTogether;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getCallSid() {
		return callSid;
	}

	public Sid getConferenceSid() {
		return conferenceSid;
	}

	public String getMasterMsId() {
		return masterMsId;
	}

	public String getMasterMsBridgeEpId() {
		return masterMsBridgeEpId;
	}

	public String getMasterMsCnfEpId() {
		return masterMsCnfEpId;
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
        private Sid callSid;
        private Sid conferenceSid;
        private String masterMsId;
        private String masterMsBridgeEpId;
        private String masterMsCnfEpId;
        private String slaveMsId;
        private String slaveMsBridgeEpId;
        private String slaveMsCnfEpId;
        private boolean isBridgedTogether;

        private Builder() {
            super();
            callSid = null;
            conferenceSid = null;
            masterMsId = null;
            masterMsBridgeEpId = null;
            masterMsCnfEpId = null;
            slaveMsId = null;
            slaveMsBridgeEpId = null;
            slaveMsCnfEpId = null;
        }

        public MediaResourceBrokerEntity build() {
            return new MediaResourceBrokerEntity(callSid, conferenceSid, masterMsId, masterMsBridgeEpId, masterMsCnfEpId, slaveMsId, slaveMsBridgeEpId, slaveMsCnfEpId, isBridgedTogether);
        }

		public Sid getCallSid() {
			return callSid;
		}

		public Sid getConferenceSid() {
			return conferenceSid;
		}

		public String getMasterMsId() {
			return masterMsId;
		}

		public String getMasterMsBridgeEpId() {
			return masterMsBridgeEpId;
		}

		public String getMasterMsCnfEpId() {
			return masterMsCnfEpId;
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
    }
}
