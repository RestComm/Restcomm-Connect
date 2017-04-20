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
 * @author maria-farooq@live.com (Maria Farooq)
 */
@Immutable
public final class Organization {
    private final Sid sid;
    private final String domainName;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;

    public Organization(final Sid sid, final String domainName, final DateTime dateCreated, final DateTime dateUpdated) {
        super();
        this.sid = sid;
        this.domainName = domainName;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getSid() {
        return sid;
    }

    public String getDomainName() {
        return domainName;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public Organization setDomainName(final String domainName) {
        return new Organization(sid, domainName, dateCreated, DateTime.now());
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private String domainName;
        private DateTime dateCreated;
        private DateTime dateUpdated;

        private Builder() {
            super();
            sid = null;
            domainName = null;
            dateCreated = DateTime.now();
            dateUpdated = DateTime.now();
        }

        public Organization build() {
            return new Organization(sid, domainName, dateCreated, dateUpdated);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setSomainName(final String domainName) {
            this.domainName = domainName;
        }

        @Override
        public String toString() {
            return "Builder [sid=" + sid + ", domainName=" + domainName + ", dateCreated=" + dateCreated + "]";
        }
    }

}
