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
import org.restcomm.connect.commons.dao.Sid;

/**
 * Represents a RestComm application
 *
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */

@Immutable
public final class Application {
    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final String friendlyName;
    private final Sid accountSid;
    private final String apiVersion;
    private final Boolean hasVoiceCallerIdLookup;
    private final URI uri;
    private final URI rcmlUrl;
    private final Kind kind;

    public Application(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
            final Sid accountSid, final String apiVersion, final Boolean hasVoiceCallerIdLookup, final URI uri,
            final URI rcmlUrl, Kind kind) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.friendlyName = friendlyName;
        this.accountSid = accountSid;
        this.apiVersion = apiVersion;
        this.hasVoiceCallerIdLookup = hasVoiceCallerIdLookup;
        this.uri = uri;
        this.rcmlUrl = rcmlUrl;
        this.kind = kind;
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

    public String getFriendlyName() {
        return friendlyName;
    }

    public Sid getAccountSid() {
        return accountSid;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public Boolean hasVoiceCallerIdLookup() {
        return hasVoiceCallerIdLookup;
    }

    public URI getUri() {
        return uri;
    }

    public URI getRcmlUrl() {
        return rcmlUrl;
    }

    public Kind getKind() {
        return kind;
    }

    public Application setFriendlyName(final String friendlyName) {
        return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
                uri, rcmlUrl, kind);
    }

    public Application setVoiceCallerIdLookup(final boolean hasVoiceCallerIdLookup) {
        return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
                uri, rcmlUrl, kind);
    }

    public Application setRcmlUrl(final URI rcmlUrl) {
        return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
                uri, rcmlUrl, kind);
    }

    public Application setKind(final Kind kind) {
        return new Application(sid, dateCreated, DateTime.now(), friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup,
                uri, rcmlUrl, kind);
    }

    public enum Kind {
        VOICE("voice"), SMS("sms"), USSD("ussd");

        private final String text;

        private Kind(final String text) {
            this.text = text;
        }

        public static Kind getValueOf(final String text) {
            Kind[] values = values();
            for (final Kind value : values) {
                if (value.toString().equals(text)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(text + " is not a valid application kind.");
        }

        @Override
        public String toString() {
            return text;
        }
    };

    public static final class Builder {
        private Sid sid;
        private String friendlyName;
        private Sid accountSid;
        private String apiVersion;
        private Boolean hasVoiceCallerIdLookup;
        private URI uri;
        private URI rcmlUrl;
        private Kind kind;

        private Builder() {
            super();
        }

        public Application build() {
            final DateTime now = DateTime.now();
            return new Application(sid, now, now, friendlyName, accountSid, apiVersion, hasVoiceCallerIdLookup, uri, rcmlUrl,
                    kind);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setFriendlyName(final String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public void setAccountSid(final Sid accountSid) {
            this.accountSid = accountSid;
        }

        public void setApiVersion(final String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public void setHasVoiceCallerIdLookup(final boolean hasVoiceCallerIdLookup) {
            this.hasVoiceCallerIdLookup = hasVoiceCallerIdLookup;
        }

        public void setUri(final URI uri) {
            this.uri = uri;
        }

        public void setRcmlUrl(final URI rcmlUrl) {
            this.rcmlUrl = rcmlUrl;
        }

        public void setKind(final Kind kind) {
            this.kind = kind;
        }

    }
}
