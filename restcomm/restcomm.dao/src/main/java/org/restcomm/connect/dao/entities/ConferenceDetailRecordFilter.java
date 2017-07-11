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

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */

@Immutable
public class ConferenceDetailRecordFilter {

    private final String accountSid;
    private final String status;
    private final Date dateCreated;
    private final Date dateUpdated;
    private final String friendlyName;
    private final Integer limit;
    private final Integer offset;

    public ConferenceDetailRecordFilter(String accountSid, String status, String dateCreated, String dateUpdated,
            String friendlyName, Integer limit, Integer offset) throws ParseException {
        this.accountSid = accountSid;
        this.status = status;
        this.friendlyName = friendlyName;
        this.limit = limit;
        this.offset = offset;
        if (dateCreated != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            Date date = parser.parse(dateCreated);
            this.dateCreated = date;
        } else
            this.dateCreated = null;

        if (dateUpdated != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            Date date = parser.parse(dateUpdated);
            this.dateUpdated = date;
        } else
            this.dateUpdated = null;
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

}
