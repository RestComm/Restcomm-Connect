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
package org.restcomm.connect.telephony.api;

import java.math.BigDecimal;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author maria.farooq@telestax.com
 */
@Immutable
public class UpdateCallInfo {
    private final Sid sid;
    private final CallStateChanged.State status;
    private final DateTime startTime;
    private final DateTime endTime;
    private final Boolean upateDuration;
    private final Boolean updateRingDuration;
    private final BigDecimal price;
    private final String answeredBy;
    private final Sid conferenceSid;
    private final Boolean muted;
    private final Boolean startConferenceOnEnter;
    private final Boolean endConferenceOnExit;
    private final Boolean beep;
    private final Boolean onHold;
    private final String msId;

    public UpdateCallInfo(final Sid sid, CallStateChanged.State status, DateTime startTime, DateTime endTime, Boolean upateDuration, Boolean updateRingDuration,
            BigDecimal price, String answeredBy, Sid conferenceSid, Boolean muted, Boolean startConferenceOnEnter,
            Boolean endConferenceOnExit, final Boolean beep, Boolean onHold, String msId) {
        super();
        this.sid = sid;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.upateDuration = upateDuration;
        this.updateRingDuration = updateRingDuration;
        this.price = price;
        this.answeredBy = answeredBy;
        this.conferenceSid = conferenceSid;
        this.muted = muted;
        this.startConferenceOnEnter = startConferenceOnEnter;
        this.endConferenceOnExit = endConferenceOnExit;
        this.onHold = onHold;
        this.msId = msId;
        this.beep = beep;
    }

    public Sid getSid() {
        return sid;
    }

    public CallStateChanged.State getStatus() {
        return status;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public Boolean upateDuration() {
        return upateDuration;
    }

    public Boolean updateRingDuration() {
        return updateRingDuration;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getAnsweredBy() {
        return answeredBy;
    }

    public Sid getConferenceSid() {
        return conferenceSid;
    }

    public Boolean getMuted() {
        return muted;
    }

    public Boolean getStartConferenceOnEnter() {
        return startConferenceOnEnter;
    }

    public Boolean getEndConferenceOnExit() {
        return endConferenceOnExit;
    }

    public Boolean getOnHold() {
        return onHold;
    }

    public Boolean beep() {
        return beep;
    }

    public String getMsId() {
        return msId;
    }

    @Override
    public String toString() {
        return "UpdateCallInfo [sid=" + sid + ", status=" + status + ", startTime=" + startTime + ", endTime=" + endTime
                + ", upateDuration=" + upateDuration + ", updateRingDuration=" + updateRingDuration + ", price=" + price
                + ", answeredBy=" + answeredBy + ", conferenceSid=" + conferenceSid + ", muted=" + muted
                + ", startConferenceOnEnter=" + startConferenceOnEnter + ", endConferenceOnExit=" + endConferenceOnExit
                + ", beep=" + beep + ", onHold=" + onHold + ", msId=" + msId + "]";
    }
}
