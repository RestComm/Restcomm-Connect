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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;

import java.text.ParseException;
import java.util.List;

/**
 * @author <a href="mailto:n.congvu@gmail.com">vunguyen</a>
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */

@Immutable
public class SmsMessageFilter {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

    private String accountSid;
    private List<String> accountSidSet; // if not-null we need the cdrs that belong to several accounts
    private String recipient;
    private String sender;
    private String startTime;  // to initialize it pass string arguments with  yyyy-MM-dd format
    private String endTime;
    private String body;
    private Integer limit;
    private Integer offset;
    private String instanceid;
    private String smppMessageId;

    private SmsMessageFilter() {

    }

    @Deprecated
    public SmsMessageFilter(String accountSid, List<String> accountSidSet, String recipient, String sender, String startTime, String endTime,
                            String body, Integer limit, Integer offset) throws ParseException {
        this(accountSid, accountSidSet, recipient, sender, startTime, endTime, body, limit, offset, null);
    }

    @Deprecated
    public SmsMessageFilter(String accountSid, List<String> accountSidSet, String recipient, String sender, String startTime, String endTime,
                            String body, Integer limit, Integer offset, String instanceId) throws ParseException {
        this.accountSid = accountSid;
        this.accountSidSet = accountSidSet;

        // The LIKE keyword uses '%' to match any (including 0) number of characters, and '_' to match exactly one character
        // Add here the '%' keyword so +15126002188 will be the same as 15126002188 and 6002188
        if (recipient != null)
            recipient = "%".concat(recipient);
        if (sender != null)
            sender = "%".concat(sender);

        this.recipient = recipient;
        this.sender = sender;
        this.body = body;
        this.limit = limit;
        this.offset = offset;
        if (startTime != null) {
            //Date date = DATE_FORMAT.parse(startTime);
            this.startTime = startTime;
        } else
            this.startTime = null;

        if (endTime != null) {
            //Date date = DATE_FORMAT.parse(endTime);
            this.endTime = endTime;
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

    public String getRecipient() {
        return recipient;
    }

    public String getSender() {
        return sender;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getBody() {
        return body;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getInstanceid() {
        return instanceid;
    }

    public String getSmppMessageId() {
        return smppMessageId;
    }

    public static Builder builer() {
        return new Builder();
    }

    public static class Builder {

        private final SmsMessageFilter filter;

        private Builder() {
            this.filter = new SmsMessageFilter();
            this.filter.limit = 50;
            this.filter.offset = 0;
        }

        public Builder accountSid(String accountSid) {
            this.filter.accountSid = accountSid;
            return this;
        }

        public Builder accountSidSet(List<String> accountSidSet) {
            this.filter.accountSidSet = accountSidSet;
            return this;
        }

        public Builder recipient(String recipient) {
            if (recipient != null && !recipient.isEmpty()) {
                this.filter.recipient = (recipient.startsWith("%") ? "" : "%") + this.filter.recipient;
            }
            return this;
        }

        public Builder sender(String sender) {
            if (sender != null && !sender.isEmpty()) {
                this.filter.sender = (sender.startsWith("%") ? "" : "%") + this.filter.sender;
            }
            return this;
        }

        public Builder startTime(DateTime startTime) {
            if (startTime != null) {
                this.filter.startTime = DATE_TIME_FORMATTER.print(startTime);
            }
            return this;
        }

        public Builder endTime(DateTime endTime) {
            if (endTime != null) {
                this.filter.endTime = DATE_TIME_FORMATTER.print(endTime);
            }
            return this;
        }

        public Builder body(String body) {
            this.filter.body = body;
            return this;
        }

        public Builder limit(int limit) {
            this.filter.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.filter.offset = offset;
            return this;
        }

        public Builder instanceId(String instanceId) {
            this.filter.instanceid = instanceId;
            return this;
        }

        public Builder smppMessageId(String smppMessageId) {
            this.filter.smppMessageId = smppMessageId;
            return this;
        }

        public SmsMessageFilter build() {
            return this.filter;
        }

    }
}
