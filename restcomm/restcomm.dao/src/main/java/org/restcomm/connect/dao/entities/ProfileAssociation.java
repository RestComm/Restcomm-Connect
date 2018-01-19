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

import java.util.Calendar;
import java.util.Date;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
@Immutable
public final class ProfileAssociation{
    private final Sid profileSid;
    private final Sid targetSid;
    private final Date dateCreated;
    private final Date dateUpdated;

    /**
     * @param profileSid
     * @param targetSid can be account or organizaation Sid
     * @param dateCreated
     * @param dateUpdated
     */
    public ProfileAssociation(final Sid profileSid, final Sid targetSid, final Date dateCreated, final Date dateUpdated) {
        super();
        this.profileSid = profileSid;
        this.targetSid = targetSid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getProfileSid() {
        return profileSid;
    }

    public Sid getTargetSid() {
        return targetSid;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param newProfileSid
     * @return
     */
    public ProfileAssociation setProfileSid(final Sid newProfileSid){
        return new ProfileAssociation(newProfileSid, targetSid, dateCreated, Calendar.getInstance().getTime());
    }
    @NotThreadSafe
    public static final class Builder {
        private Sid profileSid;
        private Sid targetSid;
        private Date dateCreated;

        private Builder() {
            super();
            profileSid = null;
            targetSid = null;
            dateCreated = null;
        }

        public ProfileAssociation build() {
            return new ProfileAssociation(profileSid, targetSid, dateCreated, Calendar.getInstance().getTime());
        }

        public void setProfileDocument(final Sid targetSid) {
            this.targetSid = targetSid;
        }

        public void setSid(final Sid profileSid) {
            this.profileSid = profileSid;
        }

        public void setDateCreated(final Date dateCreated) {
            this.dateCreated = dateCreated;
        }
    }

    @Override
    public String toString() {
        return "ProfileAssociation [profileSid=" + profileSid + ", targetSid=" + targetSid + ", dateCreated="
                + dateCreated + ", dateUpdated=" + dateUpdated + "]";
    }
}
