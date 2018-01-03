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

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
@Immutable
public final class ConferenceClosingFilter {
    private final String sid;
    private final String status;
    private final String slaveMsId;
    private final Date dateUpdated;
    private final boolean amIMaster;
    private boolean completed;

    public ConferenceClosingFilter(String sid, String status, String slaveMsId, boolean amIMaster) {
        super();
        this.sid = sid;
        this.status = status;
        this.slaveMsId = slaveMsId;
        this.dateUpdated = Calendar.getInstance().getTime();
        this.amIMaster = amIMaster;
    }

    public String getSid() {
        return sid;
    }

    public String getStatus() {
        return status;
    }

    public String getSlaveMsId() {
        return slaveMsId;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    public boolean isAmIMaster() {
        return amIMaster;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
