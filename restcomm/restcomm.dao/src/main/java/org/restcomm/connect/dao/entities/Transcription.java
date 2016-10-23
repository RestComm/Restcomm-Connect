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

import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;

import org.joda.time.DateTime;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class Transcription implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final Sid accountSid;
    private final Status status;
    private final Sid recordingSid;
    private final Double duration;
    private final String transcriptionText;
    private final BigDecimal price;
    private Currency priceUnit;
    private final URI uri;

    public Transcription(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
            final Status status, final Sid recordingSid, final Double duration, final String transcriptionText,
            final BigDecimal price, final Currency priceUnit, final URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.accountSid = accountSid;
        this.status = status;
        this.recordingSid = recordingSid;
        this.duration = duration;
        this.transcriptionText = transcriptionText;
        this.price = price;
        this.priceUnit = priceUnit;
        this.uri = uri;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getSid() {
        return sid;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public Sid getAccountSid() {
        return accountSid;
    }

    public Status getStatus() {
        return status;
    }

    public Sid getRecordingSid() {
        return recordingSid;
    }

    public Double getDuration() {
        return duration;
    }

    public String getTranscriptionText() {
        return transcriptionText;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Currency getPriceUnit() {
        return priceUnit;
    }

    public URI getUri() {
        return uri;
    }

    public Transcription setStatus(final Status status) {
        final DateTime now = DateTime.now();
        return new Transcription(sid, dateCreated, now, accountSid, status, recordingSid, duration, transcriptionText, price,
                priceUnit, uri);
    }

    public Transcription setTranscriptionText(final String text) {
        final DateTime now = DateTime.now();
        return new Transcription(sid, dateCreated, now, accountSid, status, recordingSid, duration, text, price, priceUnit, uri);
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private Sid accountSid;
        private Status status;
        private Sid recordingSid;
        private Double duration;
        private String transcriptionText;
        private BigDecimal price;
        private Currency priceUnit;

        private URI uri;

        private Builder() {
            super();
        }

        public Transcription build() {
            final DateTime now = DateTime.now();
            return new Transcription(sid, now, now, accountSid, status, recordingSid, duration, transcriptionText, price,
                    priceUnit, uri);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setAccountSid(final Sid accountSid) {
            this.accountSid = accountSid;
        }

        public void setStatus(final Status status) {
            this.status = status;
        }

        public void setRecordingSid(final Sid recordingSid) {
            this.recordingSid = recordingSid;
        }

        public void setDuration(final double duration) {
            this.duration = duration;
        }

        public void setTranscriptionText(final String transcriptionText) {
            this.transcriptionText = transcriptionText;
        }

        public void setPrice(final BigDecimal price) {
            this.price = price;
        }

        public void setPriceUnit(Currency priceUnit) {
            this.priceUnit = priceUnit;
        }

        public void setUri(final URI uri) {
            this.uri = uri;
        }
    }

    public enum Status {
        IN_PROGRESS("in-progress"), COMPLETED("completed"), FAILED("failed");

        private final String text;

        private Status(final String text) {
            this.text = text;
        }

        public static Status getStatusValue(final String text) {
            final Status[] values = values();
            for (final Status value : values) {
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
