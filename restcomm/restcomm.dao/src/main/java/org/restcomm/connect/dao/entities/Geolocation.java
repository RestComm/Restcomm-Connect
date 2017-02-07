/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
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

package org.restcomm.connect.dao.entities;

import java.net.URI;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
public final class Geolocation {

    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final DateTime dateExecuted;
    private final Sid accountSid;
    private final String source;
    private final String deviceIdentifier;
    private final GeolocationType geolocationType;
    private final String responseStatus;
    private final String cellId;
    private final String locationAreaCode;
    private final Integer mobileCountryCode;
    private final String mobileNetworkCode;
    private final Long networkEntityAddress;
    private final Integer ageOfLocationInfo;
    private final String deviceLatitude;
    private final String deviceLongitude;
    private final Long accuracy;
    private final String physicalAddress;
    private final String internetAddress;
    private final String formattedAddress;
    private final DateTime locationTimestamp;
    private final String eventGeofenceLatitude;
    private final String eventGeofenceLongitude;
    private final Long radius;
    private final String geolocationPositioningType;
    private final String lastGeolocationResponse;
    private final String cause;
    private final String apiVersion;
    private final URI uri;

    public Geolocation(Sid sid, DateTime dateCreated, DateTime dateUpdated, DateTime dateExecuted, Sid accountSid,
                       String source, String deviceIdentifier, GeolocationType geolocationType, String responseStatus, String cellId,
                       String locationAreaCode, Integer mobileCountryCode, String mobileNetworkCode, Long networkEntityAddress,
                       Integer ageOfLocationInfo, String deviceLatitude, String deviceLongitude, Long accuracy, String physicalAddress,
                       String internetAddress, String formattedAddress, DateTime locationTimestamp, String eventGeofenceLatitude,
                       String eventGeofenceLongitude, Long radius, String geolocationPositioningType, String lastGeolocationResponse,
                       String cause, String apiVersion, URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.dateExecuted = dateExecuted;
        this.accountSid = accountSid;
        this.source = source;
        this.deviceIdentifier = deviceIdentifier;
        this.geolocationType = geolocationType;
        this.responseStatus = responseStatus;
        this.cellId = cellId;
        this.locationAreaCode = locationAreaCode;
        this.mobileCountryCode = mobileCountryCode;
        this.mobileNetworkCode = mobileNetworkCode;
        this.networkEntityAddress = networkEntityAddress;
        this.ageOfLocationInfo = ageOfLocationInfo;
        this.deviceLatitude = deviceLatitude;
        this.deviceLongitude = deviceLongitude;
        this.accuracy = accuracy;
        this.physicalAddress = physicalAddress;
        this.internetAddress = internetAddress;
        this.formattedAddress = formattedAddress;
        this.locationTimestamp = locationTimestamp;
        this.eventGeofenceLatitude = eventGeofenceLatitude;
        this.eventGeofenceLongitude = eventGeofenceLongitude;
        this.radius = radius;
        this.geolocationPositioningType = geolocationPositioningType;
        this.lastGeolocationResponse = lastGeolocationResponse;
        this.cause = cause;
        this.apiVersion = apiVersion;
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

    public DateTime getDateExecuted() {
        return dateExecuted;
    }

    public Sid getAccountSid() {
        return accountSid;
    }

    public String getSource() {
        return source;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public GeolocationType getGeolocationType() {
        return geolocationType;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public String getCellId() {
        return cellId;
    }

    public String getLocationAreaCode() {
        return locationAreaCode;
    }

    public Integer getMobileCountryCode() {
        return mobileCountryCode;
    }

    public String getMobileNetworkCode() {
        return mobileNetworkCode;
    }

    public Long getNetworkEntityAddress() {
        return networkEntityAddress;
    }

    public Integer getAgeOfLocationInfo() {
        return ageOfLocationInfo;
    }

    public String getDeviceLatitude() {
        return deviceLatitude;
    }

    public String getDeviceLongitude() {
        return deviceLongitude;
    }

    public Long getAccuracy() {
        return accuracy;
    }

    public String getPhysicalAddress() {
        return physicalAddress;
    }

    public String getInternetAddress() {
        return internetAddress;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public DateTime getLocationTimestamp() {
        return locationTimestamp;
    }

    public String getEventGeofenceLatitude() {
        return eventGeofenceLatitude;
    }

    public String getEventGeofenceLongitude() {
        return eventGeofenceLongitude;
    }

    public Long getRadius() {
        return radius;
    }

    public String getGeolocationPositioningType() {
        return geolocationPositioningType;
    }

    public String getLastGeolocationResponse() {
        return lastGeolocationResponse;
    }

    public String getCause() {
        return cause;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public URI getUri() {
        return uri;
    }

    public enum GeolocationType {
        Immediate("Immediate"), Notification("Notification");

        private final String glt;

        private GeolocationType(final String glt) {
            this.glt = glt;
        }

        public static GeolocationType getValueOf(final String glt) {
            GeolocationType[] values = values();
            for (final GeolocationType value : values) {
                if (value.toString().equals(glt)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(glt + " is not a valid GeolocationType.");
        }

        @Override
        public String toString() {
            return glt;
        }
    };

    public Geolocation setSid(Sid sid) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateCreated(DateTime dateCreated) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateUpdated(DateTime dateUpdated) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateExecuted(DateTime dateExecuted) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAccountSid(Sid accountSid) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setSource(String source) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceIdentifier(String deviceIdentifier) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeolocationType(GeolocationType geolocationType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setResponseStatus(String responseStatus) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setCellId(String cellId) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLocationAreaCode(String locationAreaCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLocationTimestamp(DateTime locationTimestamp) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMobileCountryCode(Integer mobileCountryCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMobileNetworkCode(String mobileNetworkCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setNetworkEntityAddress(Long networkEntityAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAgeOfLocationInfo(Integer ageOfLocationInfo) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceLatitude(String deviceLatitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceLongitude(String deviceLongitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAccuracy(Long accuracy) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setPhysicalAddress(String physicalAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setInternetAddress(String internetAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setFormattedAddress(String formattedAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setEventGeofenceLatitude(String eventGeofenceLatitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setEventGeofenceLongitude(String eventGeofenceLongitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setRadius(Long radius) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeolocationPositioningType(String geolocationPositioningType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLastGeolocationResponse(String lastGeolocationResponse) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setCause(String cause) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setApiVersion(String apiVersion) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUri(URI uri) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
            geolocationType, responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
            networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
            internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
            geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static final class Builder {

        private Sid sid;
        private DateTime dateUpdated;
        private Sid accountSid;
        private String source;
        private String deviceIdentifier;
        private GeolocationType geolocationType;
        private String responseStatus;
        private String cellId;
        private String locationAreaCode;
        private Integer mobileCountryCode;
        private String mobileNetworkCode;
        private Long networkEntityAddress;
        private Integer ageOfLocationInfo;
        private String deviceLatitude;
        private String deviceLongitude;
        private Long accuracy;
        private String physicalAddress;
        private String internetAddress;
        private String formattedAddress;
        private DateTime locationTimestamp;
        private String eventGeofenceLatitude;
        private String eventGeofenceLongitude;
        private Long radius;
        private String geolocationPositioningType;
        private String lastGeolocationResponse;
        private String cause;
        private String apiVersion;
        private URI uri;

        private Builder() {
            super();
        }

        public Geolocation build() {
            final DateTime now = DateTime.now();
            return new Geolocation(sid, now, dateUpdated, now, accountSid, source, deviceIdentifier, geolocationType,
                responseStatus, cellId, locationAreaCode, mobileCountryCode, mobileNetworkCode, networkEntityAddress,
                ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress, internetAddress,
                formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
        }

        public void setSid(Sid sid) {
            this.sid = sid;
        }

        public void setDateUpdated(DateTime dateUpdated) {
            this.dateUpdated = dateUpdated;
        }

        public void setAccountSid(Sid accountSid) {
            this.accountSid = accountSid;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setDeviceIdentifier(String deviceIdentifier) {
            this.deviceIdentifier = deviceIdentifier;
        }

        public void setGeolocationType(GeolocationType geolocationType) {
            this.geolocationType = geolocationType;
        }

        public void setResponseStatus(String responseStatus) {
            this.responseStatus = responseStatus;
        }

        public void setCellId(String cellId) {
            this.cellId = cellId;
        }

        public void setLocationAreaCode(String locationAreaCode) {
            this.locationAreaCode = locationAreaCode;
        }

        public void setMobileCountryCode(Integer mobileCountryCode) {
            this.mobileCountryCode = mobileCountryCode;
        }

        public void setMobileNetworkCode(String mobileNetworkCode) {
            this.mobileNetworkCode = mobileNetworkCode;
        }

        public void setNetworkEntityAddress(Long networkEntityAddress) {
            this.networkEntityAddress = networkEntityAddress;
        }

        public void setAgeOfLocationInfo(Integer ageOfLocationInfo) {
            this.ageOfLocationInfo = ageOfLocationInfo;
        }

        public void setDeviceLatitude(String devLatitude) {
            this.deviceLatitude = devLatitude;
        }

        public void setDeviceLongitude(String devLongitude) {
            this.deviceLongitude = devLongitude;
        }

        public void setAccuracy(Long accuracy) {
            this.accuracy = accuracy;
        }

        public void setPhysicalAddress(String physicalAddress) {
            this.physicalAddress = physicalAddress;
        }

        public void setInternetAddress(String internetAddress) {
            this.internetAddress = internetAddress;
        }

        public void setFormattedAddress(String formattedAddress) {
            this.formattedAddress = formattedAddress;
        }

        public void setLocationTimestamp(DateTime locationTimestamp) {
            try {
                this.locationTimestamp = locationTimestamp;
            } catch (Exception exception) {
                DateTime locTimestamp = DateTime.parse("1900-01-01");
                this.locationTimestamp = locTimestamp;
            }
        }

        public void setEventGeofenceLatitude(String eventGeofenceLat) {
            this.eventGeofenceLatitude = eventGeofenceLat;
        }

        public void setEventGeofenceLongitude(String eventGeofenceLong) {
            this.eventGeofenceLongitude = eventGeofenceLong;
        }

        public void setRadius(Long radius) {
            if (geolocationType.toString().equals(GeolocationType.Notification)) {
                this.radius = radius;
            } else {
                this.radius = null;
            }
        }

        public void setGeolocationPositioningType(String geolocationPositioningType) {
            this.geolocationPositioningType = geolocationPositioningType;
        }

        public void setLastGeolocationResponse(String lastGeolocationResponse) {
            this.lastGeolocationResponse = lastGeolocationResponse;
        }

        public void setCause(String cause) {
            if (responseStatus != null && (responseStatus.equalsIgnoreCase("rejected")
                || responseStatus.equalsIgnoreCase("unauthorized") || responseStatus.equalsIgnoreCase("failed"))) {
                this.cause = cause;
                // "cause" is only updated if "responseStatus" is not null and is either "rejected", "unauthorized" or "failed"
                // Otherwise, it's value in HTTP POST/PUT is ignored
            }
            if (responseStatus != null && (!responseStatus.equalsIgnoreCase("rejected")
                && !responseStatus.equalsIgnoreCase("unauthorized") && !responseStatus.equalsIgnoreCase("failed"))) {
                this.cause = null;
                // "cause" is set to null if "responseStatus" is not null and is neither "rejected", "unauthorized" nor "failed"
            }
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }

    }

}
