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

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
@Immutable
public final class Registration implements Comparable<Registration> {
    private final Sid sid;
    private final String instanceId;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final DateTime dateExpires;
    private final String addressOfRecord;
    private final String displayName;
    private final String userName;
    private final int timeToLive;
    private final String location;
    private final String userAgent;
    private final boolean webrtc;
    private final boolean isLBPresent;

    public Registration(final Sid sid, final String instanceId, final DateTime dateCreated, final DateTime dateUpdated, final String addressOfRecord,
            final String displayName, final String userName, final String userAgent, final int timeToLive,
            final String location, final boolean webRTC, final boolean isLBPresent) {
        this(sid, instanceId, dateCreated, dateUpdated, DateTime.now().plusSeconds(timeToLive), addressOfRecord, displayName, userName,
                userAgent, timeToLive, location, webRTC, isLBPresent);
    }

    public Registration(final Sid sid, final String instanceId, final DateTime dateCreated, final DateTime dateUpdated, final DateTime dateExpires,
            final String addressOfRecord, final String displayName, final String userName, final String userAgent,
            final int timeToLive, final String location, final boolean webRTC, final boolean isLBPresent) {
        super();
        this.sid = sid;
        this.instanceId = instanceId;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.dateExpires = dateExpires;
        this.addressOfRecord = addressOfRecord;
        this.displayName = displayName;
        this.userName = userName;
        this.location = location;
        this.userAgent = userAgent;
        this.timeToLive = timeToLive;
        this.webrtc = webRTC;
        this.isLBPresent = isLBPresent; //(isLBPresent != null) ? isLBPresent : false;
    }

    public Sid getSid() {
        return sid;
    }

    public String getInstanceId() { return instanceId; }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public DateTime getDateExpires() {
        return dateExpires;
    }

    public String getAddressOfRecord() {
        return addressOfRecord;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUserName() {
        return userName;
    }

    public String getLocation() {
        return location;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public boolean isWebRTC() {
        return webrtc;
    }

    public boolean isLBPresent() {
        return isLBPresent;
    }

    public Registration setTimeToLive(final int timeToLive) {
        final DateTime now = DateTime.now();
        return new Registration(sid, instanceId, dateCreated, now, now.plusSeconds(timeToLive), addressOfRecord, displayName, userName,
                userAgent, timeToLive, location, webrtc, isLBPresent);
    }

    public Registration updated() {
        final DateTime now = DateTime.now();
        return new Registration(sid, instanceId, dateCreated, now, dateExpires, addressOfRecord, displayName, userName, userAgent, timeToLive, location, webrtc, isLBPresent);
    }

    @Override
    public int compareTo(Registration registration) {
        // use reverse order of comparator to have registrations sorted in descending order
        if (this.getDateUpdated().toDate().getTime() > registration.getDateUpdated().toDate().getTime())
            return -1;
        else
            return 1;
    }

}
