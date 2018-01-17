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

import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@Immutable
public final class Profile {
    private final Sid sid;
    private final byte[] profileDocument;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;

    public Profile(final Sid sid, final byte[] profileDocument, final DateTime dateCreated, final DateTime dateUpdated) {
        super();
        this.sid = sid;
        this.profileDocument = profileDocument;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getSid() {
        return sid;
    }

    public byte[] getProfileDocument() {
        return profileDocument;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private byte[] profileDocument;
        private DateTime dateCreated;

        private Builder() {
            super();
            sid = null;
            profileDocument = null;
            dateCreated = null;
        }

        public Profile build() {
            return new Profile(sid, profileDocument, dateCreated, DateTime.now());
        }

        public void setProfileDocument(final byte[] profileDocument) {
            this.profileDocument = profileDocument;
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setDateCreated(final DateTime dateCreated) {
            this.dateCreated = dateCreated;
        }
    }

}
