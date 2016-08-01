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

import java.net.URI;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

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
    private final String masterMsSDP;

    public ConferenceDetailRecord(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated,
            final Sid accountSid, final String status, final String friendlyName, final String apiVersion, final URI uri, final String msId, final String masterMsSDP) {
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
        this.masterMsSDP = masterMsSDP;
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

    public String getMasterMsSDP() {
        return masterMsSDP;
    }

    public ConferenceDetailRecord setStatus(final String status) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterMsSDP);
    }

    public ConferenceDetailRecord setStartTime(final DateTime startTime) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterMsSDP);
    }

    public ConferenceDetailRecord setEndTime(final DateTime endTime) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterMsSDP);
    }

    public ConferenceDetailRecord setMasterMsId(final String masterMsId) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterMsSDP);
    }

    public ConferenceDetailRecord setBridgeId(final String bridgeId) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterMsSDP);
    }

    public ConferenceDetailRecord setMasterMsSDP(final String masterMsSDP) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri, masterMsId, masterMsSDP);
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private DateTime dateCreated;
        private DateTime dateUpdated;
        private Sid accountSid;
        private String status;
        private String friendlyName;
        private String apiVersion;
        private URI uri;
        private String masterMsId;
        private String masterMsSDP;

        private Builder() {
            super();
            sid = null;
            dateCreated = null;
            dateUpdated = DateTime.now();
            accountSid = null;
            status = null;
            friendlyName = null;
            apiVersion = null;
            uri = null;
            masterMsId = null;
            masterMsSDP = null;
        }

        public ConferenceDetailRecord build() {
            return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid,
                    status, friendlyName, apiVersion, uri, masterMsId, masterMsSDP);
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

        public void setMasterMsSDP(final String masterMsSDP) {
            this.masterMsSDP = masterMsSDP;
        }
    }
}
