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
import java.util.List;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * @author <a href="mailto:n.congvu@gmail.com">vunguyen</a>
 */

@Immutable
public class TranscriptionFilter {
    private final String accountSid;
    private final List<String> accountSidSet; // if not-null we need the cdrs that belong to several accounts
    private final Date startTime;  // to initialize it pass string arguments with  yyyy-MM-dd format
    private final Date endTime;
    private final String transcriptionText;
    private final Integer limit;
    private final Integer offset;
    private final String instanceid;

    public TranscriptionFilter(String accountSid, List<String> accountSidSet, String startTime, String endTime,
                                  String transcriptionText, Integer limit, Integer offset) throws ParseException {
        this(accountSid, accountSidSet, startTime,endTime, transcriptionText, limit,offset,null);
    }

    public TranscriptionFilter(String accountSid, List<String> accountSidSet, String startTime, String endTime,
                                  String transcriptionText, Integer limit, Integer offset, String instanceId) throws ParseException {
        this.accountSid = accountSid;
        this.accountSidSet = accountSidSet;

        this.transcriptionText = transcriptionText;
        this.limit = limit;
        this.offset = offset;
        if (startTime != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            Date date = parser.parse(startTime);
            this.startTime = date;
        } else
            this.startTime = null;

        if (endTime != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            Date date = parser.parse(endTime);
            this.endTime = date;
        } else {
            this.endTime = null;
        }
        if (instanceId != null && !instanceId.isEmpty()) {
            this.instanceid = instanceId;
        } else {
            this.instanceid = null;
        }
    }

    public String getSid() {
        return accountSid;
    }

    public List<String> getAccountSidSet() {
        return accountSidSet;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public String getTranscriptionText() {
        return transcriptionText;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getInstanceid() { return instanceid; }
}
