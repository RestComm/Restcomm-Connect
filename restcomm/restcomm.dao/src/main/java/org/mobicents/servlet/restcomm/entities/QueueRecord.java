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

import java.util.Date;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */
@JsonSerialize
public class QueueRecord {

    private final String callerSid;
    private final Date dateOfEnqueued;

    public QueueRecord(String callerSid, Date dateOfEnqueued) {
        super();
        this.callerSid = callerSid;
        this.dateOfEnqueued = dateOfEnqueued;
    }

    public String getCallerSid() {
        return callerSid;
    }

    public Date getDateOfEnqueued() {
        return dateOfEnqueued;
    }

    public DateTime toDateTime() {
        if (dateOfEnqueued != null) {
            return new DateTime(dateOfEnqueued);
        }
        return null;
    }

}
