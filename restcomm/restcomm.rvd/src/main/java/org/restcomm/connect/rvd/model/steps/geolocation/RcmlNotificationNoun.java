/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.restcomm.connect.rvd.model.steps.geolocation;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
class RcmlNotificationNoun extends RcmlGeolocationNoun {

    String eventGeofencingLatitude = null;
    String eventGeofencingLongitude = null;
    Integer geofenceRange = null;
    Integer geofenceEvent = null;

    public String getEventGeofencingLatitude() {
        return eventGeofencingLatitude;
    }

    public void setEventGeofencingLatitude(String eventGeofencingLatitude) {
        this.eventGeofencingLatitude = eventGeofencingLatitude;
    }

    public String getEventGeofencingLongitude() {
        return eventGeofencingLongitude;
    }

    public void setEventGeofencingLongitude(String eventGeofencingLongitude) {
        this.eventGeofencingLongitude = eventGeofencingLongitude;
    }

    public Integer getGeofenceRange() {
        return geofenceRange;
    }

    public void setGeofenceRange(Integer geofenceRange) {
        this.geofenceRange = geofenceRange;
    }

    public Integer getGeofenceEvent() {
        return geofenceEvent;
    }

    public void setGeofenceEvent(Integer geofenceEvent) {
        this.geofenceEvent = geofenceEvent;
    }

}
