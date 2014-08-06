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
package org.mobicents.servlet.restcomm.provisioning.number.api;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * <p>The following properties are available for phone numbers from the US and Canada:</p>
 * <table>
 * 	<thead>
 * 		<tr>
 * 			<th align="left">Property</th>
 * 			<th align="left">Description</th>
 * 		</tr>
 * 	</thead>
 * 	<tbody>
 * 		<tr>
 * 			<td class='notranslate'  align="left">FriendlyName</td>
 * 			<td align="left">A nicely-formatted version of the phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">PhoneNumber</td>
 * 			<td align="left">The phone number, in <a href="response#phone-numbers">E.164</a> (i.e. "+1") format.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">Lata</td>
 * 			<td align="left">The <a href="http://en.wikipedia.org/wiki/Local_access_and_transport_area">LATA</a> of this phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">RateCenter</td>
 * 			<td align="left">The <a href="http://en.wikipedia.org/wiki/Telephone_exchange">rate center</a> of this phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">Latitude</td>
 * 			<td align="left">The latitude coordinate of this phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">Longitude</td>
 * 			<td align="left">The longitude coordinate of this phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">Region</td>
 * 			<td align="left">The two-letter state or province abbreviation of this phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">PostalCode</td>
 * 			<td align="left">The postal (zip) code of this phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">IsoCountry</td>
 * 			<td align="left">The <a href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO country code</a> of this phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">Capabilities</td>
 * 			<td align="left">This is a set of boolean properties that indicate whether a phone number can receive calls or messages.  Possible capabilities are  <code class='notranslate'>Voice</code>, <code class='notranslate'>SMS</code>, and <code class='notranslate'>MMS</code> with each having a value of either <code class='notranslate'>true</code> or <code class='notranslate'>false</code>.</td>
 * 		</tr>
 * 	</tbody>
 * </table>
 * 
 * <p>The following properties are available for phone numbers outside the US and Canada:</p>
 * 
 * <table>
 * 	<thead>
 * 		<tr>
 * 			<th align="left">Property</th>
 * 			<th align="left">Description</th>
 * 		</tr>
 * 	</thead>
 * <tbody>
 * 		<tr>
 * 			<td class='notranslate'  align="left">FriendlyName</td>
 * 			<td align="left">A nicely-formatted version of the phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">PhoneNumber</td>
 * 			<td align="left">The phone number, in <a href="response#phone-numbers">E.164</a> (i.e. "+44") format.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">IsoCountry</td>
 * 			<td align="left">The <a href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO country code</a> of this phone number.</td>
 * 		</tr>
 * 		<tr>
 * 			<td class='notranslate'  align="left">Capabilities</td>
 * 			<td align="left">This is a set of boolean properties that indicate whether a phone number can receive calls or messages.  Possible capabilities are  <code class='notranslate'>Voice</code>, <code class='notranslate'>SMS</code>, and <code class='notranslate'>MMS</code> with each having a value of either <code class='notranslate'>true</code> or <code class='notranslate'>false</code>.</td>
 * 		</tr>
 * 	</tbody>
 * </table>
 * 
 * @author jean.deruelle@telestax.com 
 */
@Immutable
public final class PhoneNumber {
    private final String friendlyName;
    private final String phoneNumber;
    private final Integer lata;
    private final String rateCenter;
    private final Double latitude;
    private final Double longitude;
    private final String region;
    private final Integer postalCode;
    private final String isoCountry;

    // Capabilities
    private final Boolean voiceCapable;
    private final Boolean smsCapable;
    private final Boolean mmsCapable;
    private final Boolean faxCapable;

    public PhoneNumber(final String friendlyName, final String phoneNumber, final Integer lata,
            final String rateCenter, final Double latitude, final Double longitude, final String region,
            final Integer postalCode, final String isoCountry) {
        this(friendlyName, phoneNumber, lata, rateCenter, latitude, longitude, region, postalCode, isoCountry, null, null,
                null, null);
    }

    public PhoneNumber(final String friendlyName, final String phoneNumber, final Integer lata,
            final String rateCenter, final Double latitude, final Double longitude, final String region,
            final Integer postalCode, final String isoCountry, final Boolean voiceCapable, final Boolean smsCapable,
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

    public PhoneNumber setFriendlyName(final String friendlyName) {
        return new PhoneNumber(friendlyName, phoneNumber, lata, rateCenter, latitude, longitude, region, postalCode,
                isoCountry, voiceCapable, smsCapable, mmsCapable, faxCapable);
    }
}
