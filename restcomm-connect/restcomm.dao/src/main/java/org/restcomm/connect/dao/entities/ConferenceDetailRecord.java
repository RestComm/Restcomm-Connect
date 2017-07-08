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

import java.net.URI;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
@Immutable
public final class ConferenceDetailRecord {
    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final Sid accountSid;
    private final String status;
    private final String friendlyName;
    private final String apiVersion;
    private final URI uri;
    private final String masterMsId;
    private final boolean masterPresent;
    private final String masterConfernceEndpointId;
    private final String masterIVREndpointId;
    private final String masterIVREndpointSessionId;
    private final String masterBridgeEndpointId;
    private final String masterBridgeEndpointSessionId;
    private final String masterBridgeConnectionIdentifier;
    private final String masterIVRConnectionIdentifier;
    private final boolean moderatorPresent;

    public ConferenceDetailRecord(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
            final String status, final String friendlyName, final String apiVersion, final URI uri, final String msId,
            final String masterConfernceEndpointId, final boolean isMasterPresent, final String masterIVREndpointId, final String masterIVREndpointSessionId,
            final String masterBridgeEndpointId, final String masterBridgeEndpointSessionId, final String masterBridgeConnectionIdentifier,
            final String masterIVRConnectionIdentifier, final boolean moderatorPresent) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.accountSid = accountSid;
        this.status = status;
        this.friendlyName = friendlyName;
        this.apiVersion = apiVersion;
        this.uri = uri;
        this.masterMsId = msId;
        this.masterConfernceEndpointId = masterConfernceEndpointId;
        this.masterPresent = isMasterPresent;
        this.masterIVREndpointId = masterIVREndpointId;
        this.masterIVREndpointSessionId = masterIVREndpointSessionId;
        this.masterBridgeEndpointId = masterBridgeEndpointId;
        this.masterBridgeEndpointSessionId = masterBridgeEndpointSessionId;
        this.masterBridgeConnectionIdentifier = masterBridgeConnectionIdentifier;
        this.masterIVRConnectionIdentifier = masterIVRConnectionIdentifier;
        this.moderatorPresent = moderatorPresent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getSid() {
        return sid;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public Sid getAccountSid() {
        return accountSid;
    }

    public String getStatus() {
        return status;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public URI getUri() {
        return uri;
    }

    public String getMasterMsId() {
        return masterMsId;
    }

    public String getMasterConferenceEndpointId() {
        return masterConfernceEndpointId;
    }

    public String getMasterBridgeEndpointId() {
        return masterBridgeEndpointId;
    }

    public String getMasterIVREndpointId() {
        return masterIVREndpointId;
    }

    public String getMasterIVREndpointSessionId() {
        return masterIVREndpointSessionId;
    }

    public String getMasterBridgeEndpointSessionId() {
        return masterBridgeEndpointSessionId;
    }

    public boolean isMasterPresent() {
        return masterPresent;
    }

    public String getMasterBridgeConnectionIdentifier() {
        return masterBridgeConnectionIdentifier;
    }

    public String getMasterIVRConnectionIdentifier() {
        return masterIVRConnectionIdentifier;
    }

    public boolean isModeratorPresent() {
        return moderatorPresent;
    }

    public ConferenceDetailRecord setStatus(final String status) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    public ConferenceDetailRecord setMasterConfernceEndpointId(final String masterConfernceEndpointId) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    public ConferenceDetailRecord setMasterIVREndpointId(final String masterIVREndpointId) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    public ConferenceDetailRecord setMasterIVREndpointSessionId(final String masterIVREndpointSessionId) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    public ConferenceDetailRecord setMasterBridgeEndpointSessionId(final String masterBridgeEndpointSessionId) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    public ConferenceDetailRecord setMasterBridgeEndpointId(final String masterBridgeEndpointId) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    public ConferenceDetailRecord setMasterBridgeConnectionIdentifier(final String masterBridgeConnectionIdentifier) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    public ConferenceDetailRecord setMasterIVRConnectionIdentifier(final String masterIVRConnectionIdentifier) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    public ConferenceDetailRecord setMasterPresent(final boolean masterPresent) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    public ConferenceDetailRecord setModeratorPresent(final boolean moderatorPresent) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, masterPresent, masterIVREndpointId, masterIVREndpointSessionId, masterBridgeEndpointId, masterBridgeEndpointSessionId, masterBridgeConnectionIdentifier, masterIVRConnectionIdentifier, moderatorPresent);
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private DateTime dateCreated;
        private Sid accountSid;
        private String status;
        private String friendlyName;
        private String apiVersion;
        private URI uri;
        private String masterMsId;
        private String masterConfernceEndpointId;
        private String masterIVREndpointId;
        private boolean isMasterPresent;

        private Builder() {
            super();
            sid = null;
            dateCreated = null;
            accountSid = null;
            status = null;
            friendlyName = null;
            apiVersion = null;
            uri = null;
            masterMsId = null;
            masterConfernceEndpointId = null;
            masterIVREndpointId = null;
            isMasterPresent = true;
        }

        public ConferenceDetailRecord build() {
            return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid,
                    status, friendlyName, apiVersion, uri, masterMsId, masterConfernceEndpointId, isMasterPresent, masterIVREndpointId, null, null, null, null, null, false);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setMasterMsId(final String msId) {
            this.masterMsId = msId;
        }

        public void setDateCreated(final DateTime dateCreated) {
            this.dateCreated = dateCreated;
        }

        public void setAccountSid(final Sid accountSid) {
            this.accountSid = accountSid;
        }

        public void setStatus(final String status) {
            this.status = status;
        }

        public void setFriendlyName(final String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public void setApiVersion(final String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public void setUri(final URI uri) {
            this.uri = uri;
        }

        public void setMasterConfernceEndpointId(final String masterConfernceEndpointId) {
            this.masterConfernceEndpointId = masterConfernceEndpointId;
        }

        public void setMasterIVREndpointId(final String masterIVREndpointId) {
            this.masterIVREndpointId = masterIVREndpointId;
        }
    }

    @Override
    public String toString() {
        return "ConferenceDetailRecord [sid=" + sid + ", dateCreated=" + dateCreated + ", dateUpdated=" + dateUpdated
                + ", accountSid=" + accountSid + ", status=" + status + ", friendlyName=" + friendlyName
                + ", apiVersion=" + apiVersion + ", uri=" + uri + ", masterMsId=" + masterMsId + ", masterPresent="
                + masterPresent + ", masterConfernceEndpointId=" + masterConfernceEndpointId + ", masterIVREndpointId="
                + masterIVREndpointId + ", masterIVREndpointSessionId=" + masterIVREndpointSessionId
                + ", masterBridgeEndpointId=" + masterBridgeEndpointId + ", masterBridgeEndpointSessionId="
                + masterBridgeEndpointSessionId + ", masterBridgeConnectionIdentifier="
                + masterBridgeConnectionIdentifier + ", masterIVRConnectionIdentifier=" + masterIVRConnectionIdentifier
                + ", moderatorPresent=" + moderatorPresent + "]";
    }

}
