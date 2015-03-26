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
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class Recording {
    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final Sid accountSid;
    private final Sid callSid;
    private final Double duration;
    private final String apiVersion;
    private URI uri;

    public Recording(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
            final Sid callSid, final Double duration, final String apiVersion, final URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.accountSid = accountSid;
        this.callSid = callSid;
        this.duration = duration;
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

    public Sid getCallSid() {
        return callSid;
    }

    public Double getDuration() {
        return duration;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public URI getUri() {
        return uri;
    }

    public Recording updateUri(URI newUri) {
        this.uri = newUri;
        return this;
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private Sid accountSid;
        private Sid callSid;
        private Double duration;
        private String apiVersion;
        private URI uri;

        private Builder() {
            super();
        }

        public Recording build() {
            final DateTime now = DateTime.now();
            return new Recording(sid, now, now, accountSid, callSid, duration, apiVersion, uri);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setAccountSid(final Sid accountSid) {
            this.accountSid = accountSid;
        }

        public void setCallSid(final Sid callSid) {
            this.callSid = callSid;
        }

        public void setDuration(final double duration) {
            this.duration = duration;
        }

        public void setApiVersion(final String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public void setUri(final URI uri) {
            this.uri = uri;
        }
    }
}
