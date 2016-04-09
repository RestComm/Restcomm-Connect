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
    private final Sid conferenceSid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final Sid accountSid;
    private final String status;
    private final DateTime startTime;
    private final DateTime endTime;
    private final String friendlyName;
    private final String apiVersion;
    private final URI uri;

    public ConferenceDetailRecord(final Sid conferenceSid, final DateTime dateCreated, final DateTime dateUpdated,
            final Sid accountSid, final String status, final DateTime startTime, final DateTime endTime, final String friendlyName, final String apiVersion, final URI uri) {
        super();
        this.conferenceSid = conferenceSid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.accountSid = accountSid;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.friendlyName = friendlyName;
        this.apiVersion = apiVersion;
        this.uri = uri;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getConferenceSid() {
        return conferenceSid;
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

    public DateTime getStartTime() {
        return startTime;
    }

    public DateTime getEndTime() {
        return endTime;
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

    public ConferenceDetailRecord setStatus(final String status) {
        return new ConferenceDetailRecord(conferenceSid, dateCreated, DateTime.now(), accountSid, status, startTime, endTime, friendlyName, apiVersion, uri);
    }

    public ConferenceDetailRecord setStartTime(final DateTime startTime) {
        return new ConferenceDetailRecord(conferenceSid, dateCreated, DateTime.now(), accountSid, status, startTime, endTime, friendlyName, apiVersion, uri);
    }

    public ConferenceDetailRecord setEndTime(final DateTime endTime) {
        return new ConferenceDetailRecord(conferenceSid, dateCreated, DateTime.now(), accountSid, status, startTime, endTime, friendlyName, apiVersion, uri);
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid conferenceSid;
        private DateTime dateCreated;
        private DateTime dateUpdated;
        private Sid accountSid;
        private String status;
        private DateTime startTime;
        private DateTime endTime;
        private String friendlyName;
        private String apiVersion;
        private URI uri;

        private Builder() {
            super();
            conferenceSid = null;
            dateCreated = null;
            dateUpdated = DateTime.now();
            accountSid = null;
            status = null;
            startTime = null;
            endTime = null;
            friendlyName = null;
            apiVersion = null;
            uri = null;
        }

        public ConferenceDetailRecord build() {
            return new ConferenceDetailRecord(conferenceSid, dateCreated, DateTime.now(), accountSid,
                    status, startTime, endTime, friendlyName, apiVersion, uri);
        }

        public void setConferenceSid(final Sid conferenceSid) {
            this.conferenceSid = conferenceSid;
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

        public void setStartTime(final DateTime startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(final DateTime endTime) {
            this.endTime = endTime;
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
    }
}
