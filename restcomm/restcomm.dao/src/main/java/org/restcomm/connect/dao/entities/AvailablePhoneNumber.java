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

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class AvailablePhoneNumber {
    private final String friendlyName;
    private final String phoneNumber;
    private final Integer lata;
    private final String rateCenter;
    private final Double latitude;
    private final Double longitude;
    private final String region;
    private final Integer postalCode;
    private final String isoCountry;
    private final String cost;

    // Capabilities
    private final Boolean voiceCapable;
    private final Boolean smsCapable;
    private final Boolean mmsCapable;
    private final Boolean faxCapable;

    public AvailablePhoneNumber(final String friendlyName, final String phoneNumber, final Integer lata,
            final String rateCenter, final Double latitude, final Double longitude, final String region,
            final Integer postalCode, final String isoCountry, final String cost) {
        this(friendlyName, phoneNumber, lata, rateCenter, latitude, longitude, region, postalCode, isoCountry, cost, null, null,
                null, null);
    }

    public AvailablePhoneNumber(final String friendlyName, final String phoneNumber, final Integer lata,
            final String rateCenter, final Double latitude, final Double longitude, final String region,
            final Integer postalCode, final String isoCountry, final String cost, final Boolean voiceCapable, final Boolean smsCapable,
            final Boolean mmsCapable, final Boolean faxCapable) {
        super();
        this.friendlyName = friendlyName;
        this.phoneNumber = phoneNumber;
        this.lata = lata;
        this.rateCenter = rateCenter;
        this.latitude = latitude;
        this.longitude = longitude;
        this.region = region;
        this.postalCode = postalCode;
        this.isoCountry = isoCountry;
        this.cost = cost;
        this.voiceCapable = voiceCapable;
        this.smsCapable = smsCapable;
        this.mmsCapable = mmsCapable;
        this.faxCapable = faxCapable;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Integer getLata() {
        return lata;
    }

    public String getRateCenter() {
        return rateCenter;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getRegion() {
        return region;
    }

    public Integer getPostalCode() {
        return postalCode;
    }

    public String getIsoCountry() {
        return isoCountry;
    }

    public String getCost() {
        return cost;
    }

    public Boolean isVoiceCapable() {
        return this.voiceCapable;
    }

    public Boolean isSmsCapable() {
        return smsCapable;
    }

    public Boolean isMmsCapable() {
        return mmsCapable;
    }

    public Boolean isFaxCapable() {
        return faxCapable;
    }

    public AvailablePhoneNumber setFriendlyName(final String friendlyName) {
        return new AvailablePhoneNumber(friendlyName, phoneNumber, lata, rateCenter, latitude, longitude, region, postalCode,
                isoCountry, cost, voiceCapable, smsCapable, mmsCapable, faxCapable);
    }
}
