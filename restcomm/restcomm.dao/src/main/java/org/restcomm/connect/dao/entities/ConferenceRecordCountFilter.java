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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

@Immutable
public class ConferenceRecordCountFilter {

    private final String accountSid;
    private final String status;
    private final Date dateCreated;
    private final Date dateUpdated;
    private final String friendlyName;
    private final Integer limit;
    private final Integer offset;
    private final String masterMsId;

    public ConferenceRecordCountFilter(String accountSid, String status, String dateCreated, String dateUpdated,
            String friendlyName, Integer limit, Integer offset, String masterMsId) throws ParseException {
        this.accountSid = accountSid;
        this.status = status;
        this.friendlyName = friendlyName;
        this.limit = limit;
        this.offset = offset;
        this.masterMsId = masterMsId;
        if (dateCreated != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            Date date = parser.parse(dateCreated);
            this.dateCreated = date;
        } else {
            this.dateCreated = null;
        }

        if (dateUpdated != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            Date date = parser.parse(dateUpdated);
            this.dateUpdated = date;
        } else {
            this.dateUpdated = null;
        }
    }

    public String getSid() {
        return accountSid;
    }

    public String getStatus() {
        return status;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getMasterMsId() {
        return masterMsId;
    }

    public static ConferenceRecordCountFilter.Builder builder() {
        return new ConferenceRecordCountFilter.Builder();
    }

    public static final class Builder {

        private String accountSid = null;
        private String status = null;
        private String dateCreated = null;
        private String dateUpdated = null;
        private String friendlyName = null;
        private Integer limit = null;
        private Integer offset = null;
        private String masterMsId = null;

        public ConferenceRecordCountFilter build() throws ParseException {
            return new ConferenceRecordCountFilter(accountSid, status, dateCreated, dateUpdated, friendlyName, limit, offset, masterMsId);
        }
        public Builder byAccountSid(String accountSid) {
            this.accountSid = accountSid;
            return this;
        }

        public Builder byStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder byDateCreated(String dateCreated) {
            this.dateCreated = dateCreated;
            return this;
        }

        public Builder byDateUpdated(String dateUpdated) {
            this.dateUpdated = dateUpdated;
            return this;
        }

        public Builder byFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public Builder byLimit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder byOffset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public Builder byMasterMsId(String masterMsId) {
            this.masterMsId = masterMsId;
            return this;
        }



    }

}
