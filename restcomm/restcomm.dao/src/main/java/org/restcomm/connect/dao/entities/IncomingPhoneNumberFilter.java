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

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * @author <a href="mailto:jean.deruelle@telestax.com">Jean Deruelle</a>
 */
@Immutable
public class IncomingPhoneNumberFilter {

    private final String accountSid;
    private final String friendlyName;
    private final String phoneNumber;
    private final String sortBy;
    private final String sortDirection;
    private final Integer limit;
    private final Integer offset;
    private final String orgSid;
    private final Boolean pureSIP;

    public IncomingPhoneNumberFilter(String accountSid, String friendlyName, String phoneNumber, String sortBy,
            String sortDirection, Integer limit, Integer offset, String orgSid, Boolean pureSIP) {
        this.accountSid = accountSid;
        this.friendlyName = friendlyName;
        this.phoneNumber = phoneNumber;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.limit = limit;
        this.offset = offset;
        this.orgSid = orgSid;
        this.pureSIP = pureSIP;
    }

    public IncomingPhoneNumberFilter(String accountSid, String friendlyName, String phoneNumber) {
        this.accountSid = accountSid;
        this.friendlyName = friendlyName;
        this.phoneNumber = phoneNumber;
        this.sortBy = null;
        this.sortDirection = null;
        this.offset = null;
        this.limit = null;
        this.orgSid = null;
        this.pureSIP = null;
    }

    public String getAccountSid() {
        return accountSid;
    }

    /**
     * @return the friendlyName
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * @return the phoneNumber
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * @return the sortBy
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * @return the sortDirection
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * @return the limit
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * @return the offset
     */
    public Integer getOffset() {
        return offset;
    }

    public String getOrgSid() {
        return orgSid;
    }

    public Boolean getPureSIP() {
        return pureSIP;
    }

    @Override
    public String toString() {
        return "IncomingPhoneNumberFilter{" + "accountSid=" + accountSid + ", friendlyName=" + friendlyName + ", phoneNumber=" + phoneNumber + ", sortBy=" + sortBy + ", sortDirection=" + sortDirection + ", limit=" + limit + ", offset=" + offset + ", orgSid=" + orgSid + ", pureSIP=" + pureSIP + '}';
    }

    public static final class Builder {

        private String accountSid = null;
        private String friendlyName = null;
        private String phoneNumber = null;
        private String sortBy = null;
        private String sortDirection = null;
        private Integer limit = null;
        private Integer offset = null;
        private String orgSid = null;
        private Boolean pureSIP = null;

        public static IncomingPhoneNumberFilter.Builder builder() {
            return new IncomingPhoneNumberFilter.Builder();
        }

        public IncomingPhoneNumberFilter build() {
            return new IncomingPhoneNumberFilter(accountSid, friendlyName, phoneNumber, sortBy, sortDirection, limit, offset, orgSid, pureSIP);
        }

        public Builder byAccountSid(String accountSid) {
            this.accountSid = accountSid;
            return this;
        }

        public Builder byPureSIP(Boolean pureSIP) {
            this.pureSIP = pureSIP;
            return this;
        }

        public Builder byFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public Builder byPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder sortedBy(String sortBy,String sortDirection) {
            this.sortBy = sortBy;
            this.sortDirection = sortDirection;
            return this;
        }


        public Builder limited(Integer limit, Integer offset) {
            this.limit = limit;
             this.offset = offset;
            return this;
        }

        public Builder byOrgSid(String orgSid) {
            this.orgSid = orgSid;
            return this;
        }
    }

}
