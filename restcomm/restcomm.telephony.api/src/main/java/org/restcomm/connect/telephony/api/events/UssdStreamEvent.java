/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2018, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it andor modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but OUT ANY WARRANTY; out even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 *  along  this program.  If not, see <http:www.gnu.orglicenses>
 */

package org.restcomm.connect.telephony.api.events;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.stream.StreamEvent;

/**
 * @author laslo.horvat@telestax.com (Laslo Horvat)
 */
@Immutable
public final class UssdStreamEvent implements StreamEvent {

    private final Sid sid;
    private final Sid accountSid;
    private final String from;
    private final String to;
    private final UssdStreamEvent.Status status;
    private final UssdStreamEvent.Direction direction;
    private final String request;
    private final DateTime dateCreated;

    public UssdStreamEvent(final Sid sid, final Sid accountSid, final String from, final String to, final Status status,
                           final Direction direction, final String request, final DateTime dateCreated) {
        super();
        this.sid = sid;
        this.accountSid = accountSid;
        this.from = from;
        this.to = to;
        this.status = status;
        this.direction = direction;
        this.request = request;
        this.dateCreated = dateCreated;

    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getSid() {
        return sid;
    }

    public Sid getAccountSid() {
        return accountSid;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Status getStatus() {
        return status;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getRequest() {
        return request;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }


    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private Sid accountSid;
        private String from;
        private String to;
        private Status status;
        private Direction direction;
        private String request;
        private DateTime dateCreated;

        private Builder() {
            super();
        }

        public UssdStreamEvent build() {
            if (dateCreated == null) {
                dateCreated = DateTime.now();
            }

            return new UssdStreamEvent(sid, accountSid, from, to, status, direction, request, dateCreated);
        }

        public Builder setSid(Sid sid) {
            this.sid = sid;
            return this;
        }

        public Builder setAccountSid(Sid accountSid) {
            this.accountSid = accountSid;
            return this;
        }

        public Builder setFrom(String from) {
            this.from = from;
            return this;
        }

        public Builder setTo(String to) {
            this.to = to;
            return this;
        }

        public Builder setStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder setDirection(Direction direction) {
            this.direction = direction;
            return this;
        }

        public Builder setRequest(String request) {
            this.request = request;
            return this;
        }

        public Builder setDateCreated(DateTime dateCreated) {
            this.dateCreated = dateCreated;
            return this;
        }
    }

    public enum Direction {
        INBOUND("inbound"), OUTBOUND("outbound");

        private final String text;

        private Direction(final String text) {
            this.text = text;
        }

        public static UssdStreamEvent.Direction getDirectionValue(final String text) {
            final UssdStreamEvent.Direction[] values = values();
            for (final UssdStreamEvent.Direction value : values) {
                if (value.toString().equals(text)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(text + " is not a valid direction.");
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public enum Status {
        STARTED("started"),
        QUEUED("queued"),
        RINGING("ringing"),
        IN_PROGRESS("in-progress"),
        CANCELED("canceled"),
        FAILED("failed"),
        NOT_FOUND("not_found"),
        PROCESSING("processing"),
        COMPLETED("completed");

        private final String text;

        private Status(final String text) {
            this.text = text;
        }

        public static UssdStreamEvent.Status getStatusValue(final String text) {
            final UssdStreamEvent.Status[] values = values();
            for (final UssdStreamEvent.Status value : values) {
                if (value.toString().equals(text)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(text + " is not a valid status.");
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
