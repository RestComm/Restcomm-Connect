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
    private final URI uri;

    public Queue(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
            final Integer averageWaitTime, final Integer currentSize, final Integer maxSize, final URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.friendlyName = friendlyName;
        this.averageWaitTime = averageWaitTime;
        this.currentSize = currentSize;
        this.maxSize = maxSize;
        this.uri = uri;
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

    public URI getUri() {
        return uri;
    }

}
