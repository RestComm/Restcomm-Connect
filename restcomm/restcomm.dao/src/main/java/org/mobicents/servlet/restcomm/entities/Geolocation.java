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

package org.mobicents.servlet.restcomm.entities;

import java.math.BigInteger;
import java.net.URI;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author fernando.mendioroz@telestax.com (Fernando Mendioroz)
 *
 */
public class Geolocation {

    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final DateTime dateExecuted;
    private final Sid accountSid;
    private final String source;
    private final String deviceIdentifier;
    private final String geolocationType;
    private final String responseStatus;
    private final String globalCellId;
    private final String locationAreaCode;
    private final Integer mobileCountryCode;
    private final Integer mobileNetworkCode;
    private final BigInteger networkEntityAddress;
    private final Integer ageOfLocationInfo;
    private final String deviceLatitude;
    private final String deviceLongitude;
    private final BigInteger accuracy;
    private final String physicalAddress;
    private final String internetAddress;
    private final String formattedAddress;
    private final DateTime locationTimestamp;
    private final String eventGeofenceLatitude;
    private final String eventGeofenceLongitude;
    private final BigInteger radius;
    private final String geolocationPositioningType;
    private final Boolean lastGeolocationResponse;
    private final String cause;
    private final String apiVersion;
    private final URI uri;

    public Geolocation(Sid sid, DateTime dateCreated, DateTime dateUpdated, DateTime dateExecuted, Sid accountSid,
            String source, String deviceIdentifier, String geolocationType, String responseStatus, String globalCellId,
            String locationAreaCode, Integer mobileCountryCode, Integer mobileNetworkCode, BigInteger networkEntityAddress,
            Integer ageOfLocationInfo, String deviceLatitude, String deviceLongitude, BigInteger accuracy,
            String physicalAddress, String internetAddress, String formattedAddress, DateTime locationTimestamp,
            String eventGeofenceLatitude, String eventGeofenceLongitude, BigInteger radius, String geolocationPositioningType,
            Boolean lastGeolocationResponse, String cause, String apiVersion, URI uri) {
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
        this.globalCellId = globalCellId;
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

    public String getGeolocationType() {
        return geolocationType;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public String getGlobalCellId() {
        return globalCellId;
    }

    public String getLocationAreaCode() {
        return locationAreaCode;
    }

    public Integer getMobileCountryCode() {
        return mobileCountryCode;
    }

    public Integer getMobileNetworkCode() {
        return mobileNetworkCode;
    }

    public BigInteger getNetworkEntityAddress() {
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

    public BigInteger getAccuracy() {
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

    public BigInteger getRadius() {
        return radius;
    }

    public String getGeolocationPositioningType() {
        return geolocationPositioningType;
    }

    public Boolean getLastGeolocationResponse() {
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

    public Geolocation setSid(Sid sid) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateCreated(DateTime dateCreated) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateUpdated(DateTime dateUpdated) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDateExecuted(DateTime dateExecuted) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAccountSid(Sid accountSid) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setSource(String source) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceIdentifier(String deviceIdentifier) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeolocationType(String geolocationType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setResponseStatus(String responseStatus) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGlobalCellId(String globalCellId) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLocationAreaCode(String locationAreaCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLocationTimeStamp(DateTime locationTimestamp) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMobileCountryCode(Integer mobileCountryCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setMobileNetworkCode(Integer mobileNetworkCode) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setNetworkEntityAddress(BigInteger networkEntityAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceLatitude(String deviceLatitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setDeviceLongitude(String deviceLongitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setAccuracy(BigInteger accuracy) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setPhysicalAddress(String physicalAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setInternetAddress(String internetAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setFormattedAddress(String formattedAddress) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setEventGeofenceLatitude(String eventGeofenceLatitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setEventGeofenceLongitude(String eventGeofenceLongitude) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setRadius(BigInteger radius) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setGeolocationPositioningType(String geolocationPositioningType) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setLastGeolocationResponse(Boolean lastGeolocationResponse) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setCause(String cause) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setApiVersion(String apiVersion) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
                networkEntityAddress, ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress,
                internetAddress, formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);
    }

    public Geolocation setUri(URI uri) {
        return new Geolocation(sid, dateCreated, dateUpdated, dateExecuted, accountSid, source, deviceIdentifier,
                geolocationType, responseStatus, globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode,
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
        private Sid accountSid;
        private String source;
        private String deviceIdentifier;
        private String geolocationType;
        private String responseStatus;
        private String globalCellId;
        private String locationAreaCode;
        private Integer mobileCountryCode;
        private Integer mobileNetworkCode;
        private BigInteger networkEntityAddress;
        private Integer ageOfLocationInfo;
        private String deviceLatitude;
        private String deviceLongitude;
        private BigInteger accuracy;
        private String physicalAddress;
        private String internetAddress;
        private String formattedAddress;
        private DateTime locationTimestamp;
        private String eventGeofenceLatitude;
        private String eventGeofenceLongitude;
        private BigInteger radius;
        private String geolocationPositioningType;
        private Boolean lastGeolocationResponse;
        private String cause;
        private String apiVersion;
        private URI uri;

        private Builder() {
            super();
        }

        public Geolocation build() {
            final DateTime now = DateTime.now();
            return new Geolocation(sid, now, now, now, accountSid, source, deviceIdentifier, geolocationType, responseStatus,
                    globalCellId, locationAreaCode, mobileCountryCode, mobileNetworkCode, networkEntityAddress,
                    ageOfLocationInfo, deviceLatitude, deviceLongitude, accuracy, physicalAddress, internetAddress,
                    formattedAddress, locationTimestamp, eventGeofenceLatitude, eventGeofenceLongitude, radius,
                    geolocationPositioningType, lastGeolocationResponse, cause, apiVersion, uri);

        }

        public void setSid(Sid sid) {
            this.sid = sid;
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

        public void setGeolocationType(String geolocationType) {
            this.geolocationType = geolocationType;
        }

        public void setResponseStatus(String responseStatus) {
            this.responseStatus = responseStatus;
        }

        public void setGlobalCellId(String globalCellId) {
            this.globalCellId = globalCellId;
        }

        public void setLocationAreaCode(String locationAreaCode) {
            this.locationAreaCode = locationAreaCode;
        }

        public void setMobileCountryCode(Integer mobileCountryCode) {
            this.mobileCountryCode = mobileCountryCode;
        }

        public void setMobileNetworkCode(Integer mobileNetworkCode) {
            this.mobileNetworkCode = mobileNetworkCode;
        }

        public void setNetworkEntityAddress(BigInteger networkEntityAddress) {
            this.networkEntityAddress = networkEntityAddress;
        }

        public void setAgeOfLocationInfo(Integer ageOfLocationInfo) {
            this.ageOfLocationInfo = ageOfLocationInfo;
        }

        public void setDeviceLatitude(String deviceLatitude) {
            this.deviceLatitude = deviceLatitude;
        }

        public void setDeviceLongitude(String deviceLongitude) {
            this.deviceLongitude = deviceLongitude;
        }

        public void setAccuracy(BigInteger accuracy) {
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
            this.locationTimestamp = locationTimestamp;
        }

        public void setEventGeofenceLatitude(String eventGeofenceLatitude) {
            this.eventGeofenceLatitude = eventGeofenceLatitude;
        }

        public void setEventGeofenceLongitude(String eventGeofenceLongitude) {
            this.eventGeofenceLongitude = eventGeofenceLongitude;
        }

        public void setRadius(BigInteger radius) {
            this.radius = radius;
        }

        public void setGeolocationPositioningType(String geolocationPositioningType) {
            this.geolocationPositioningType = geolocationPositioningType;
        }

        public void setLastGeolocationResponse(Boolean lastGeolocationResponse) {
            this.lastGeolocationResponse = lastGeolocationResponse;
        }

        public void setCause(String cause) {
            this.cause = cause;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }

    }

}
