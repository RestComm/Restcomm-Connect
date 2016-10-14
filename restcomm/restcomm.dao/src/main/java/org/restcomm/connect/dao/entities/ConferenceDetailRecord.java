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

    public ConferenceDetailRecord(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated,
            final Sid accountSid, final String status, final String friendlyName, final String apiVersion, final URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.accountSid = accountSid;
        this.status = status;
        this.friendlyName = friendlyName;
        this.apiVersion = apiVersion;
        this.uri = uri;
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

    public ConferenceDetailRecord setStatus(final String status) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri);
    }

    public ConferenceDetailRecord setStartTime(final DateTime startTime) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri);
    }

    public ConferenceDetailRecord setEndTime(final DateTime endTime) {
        return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid, status, friendlyName, apiVersion, uri);
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
        }

        public ConferenceDetailRecord build() {
            return new ConferenceDetailRecord(sid, dateCreated, DateTime.now(), accountSid,
                    status, friendlyName, apiVersion, uri);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
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
    }
}
