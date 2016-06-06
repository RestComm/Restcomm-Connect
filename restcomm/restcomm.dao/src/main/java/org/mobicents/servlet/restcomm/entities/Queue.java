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

import java.net.URI;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.entities.Client.Builder;

import com.cedarsoftware.util.io.JsonReader;

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */
@Immutable
public class Queue {
    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final String friendlyName;
    private final Integer averageWaitTime;
    private final Integer currentSize;
    private final Integer maxSize;
    private final Sid accountSid;
    private final URI uri;
    private final byte[] queue;
    private static final Integer AVERAGE_WAIT_TIME = 0;

    public Queue(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
            final Integer averageWaitTime, final Integer currentSize, final Integer maxSize, final Sid accountSid,
            final URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.friendlyName = friendlyName;
        this.averageWaitTime = averageWaitTime;
        this.currentSize = currentSize;
        this.maxSize = maxSize;
        this.accountSid = accountSid;
        this.uri = uri;
        this.queue = null;
    }

    public Queue(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
            final Integer averageWaitTime, final Integer currentSize, final Integer maxSize, final Sid accountSid,
            final URI uri, final byte[] queue) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.friendlyName = friendlyName;
        this.averageWaitTime = averageWaitTime;
        this.currentSize = currentSize;
        this.maxSize = maxSize;
        this.accountSid = accountSid;
        this.uri = uri;
        this.queue = queue;
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

    public String getFriendlyName() {
        return friendlyName;
    }

    public Integer getAverageWaitTime() {
        return averageWaitTime;
    }

    public Integer getCurrentSize() {
        return currentSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public Sid getAccountSid() {
        return accountSid;
    }

    public URI getUri() {
        return uri;
    }

    public byte[] getQueue() {
        return queue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Queue setQueue(final byte[] queue) {

        return new Queue(sid, dateCreated, DateTime.now(), friendlyName, AVERAGE_WAIT_TIME, currentSize, maxSize, accountSid,
                uri, queue);
    }

    public Queue setCurrentSize(final Integer currentSize) {

        return new Queue(sid, dateCreated, DateTime.now(), friendlyName, AVERAGE_WAIT_TIME, currentSize, maxSize, accountSid,
                uri, queue);
    }

    public Queue setFriendlyName(final String friendlyName) {

        return new Queue(sid, dateCreated, DateTime.now(), friendlyName, AVERAGE_WAIT_TIME, currentSize, maxSize, accountSid,
                uri);
    }

    public Queue setMaxSize(final Integer maxSize) {

        return new Queue(sid, dateCreated, DateTime.now(), friendlyName, AVERAGE_WAIT_TIME, currentSize, maxSize, accountSid,
                uri);
    }

    @SuppressWarnings("unchecked")
    public java.util.Queue<QueueRecord> toCollectionFromBytes() {
        java.util.Queue<QueueRecord> queue = new java.util.LinkedList<QueueRecord>();
        if (this.queue != null) {
            queue = (java.util.Queue<QueueRecord>) JsonReader.jsonToJava(new String(this.queue));
            System.out.println(queue.element());
        }
        return (queue != null) ? queue : new java.util.LinkedList<QueueRecord>();
    }

    public static final class Builder {
        private Sid sid;
        private String friendlyName;
        private Integer averageWaitTime;
        private Integer currentSize;
        private Integer maxSize;
        private Sid accountSid;
        private URI uri;
        private byte[] queue;

        private Builder() {
            super();
        }

        public Queue build() {
            final DateTime now = DateTime.now();
            return new Queue(sid, now, now, friendlyName, averageWaitTime, currentSize, maxSize, accountSid, uri, queue);
        }

        public void setSid(Sid sid) {
            this.sid = sid;
        }

        public void setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public void setAverageWaitTime(Integer averageWaitTime) {
            this.averageWaitTime = averageWaitTime;
        }

        public void setCurrentSize(Integer currentSize) {
            this.currentSize = currentSize;
        }

        public void setMaxSize(Integer maxSize) {
            this.maxSize = maxSize;
        }

        public void setAccountSid(Sid accountSid) {
            this.accountSid = accountSid;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }

        public void setQueue(byte[] queue) {
            this.queue = queue;
        }

    }
}
