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

package org.mobicents.servlet.restcomm.provisioning.number.api;

import java.util.regex.Pattern;

/**
 * This POJO is holding the different filters that can be applied to Phone Number Search to filter the results
 *
 * <p>
 * The following filters are available for phone numbers search:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th align="left">Property</th>
 * <th align="left">Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td class='notranslate' align="left">AreaCode</td>
 * <td align="left">The areacode for the phone number</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">FilterPattern</td>
 * <td align="left">A pattern to match phone numbers on. Valid characters are '*' and [0-9a-zA-Z]. The '*' character will match any single digit.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">SmsEnabled</td>
 * <td align="left">This indicates whether the phone numbers can receive text messages. Possible values are true or false..</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">MmsEnabled</td>
 * <td align="left">This indicates whether the phone numbers can receive MMS messages. Possible values are true or false..</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">VoiceEnabled</td>
 * <td align="left">This indicates whether the phone numbers can receive calls. Possible values are true or false..</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">FaxEnabled</td>
 * <td align="left">This indicates whether the phone numbers can receive fax message. Possible values are true or false..</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">UssdEnabled</td>
 * <td align="left">This indicates whether the phone numbers can receive USSD messages. Possible values are true or false..</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">NearNumber</td>
 * <td align="left">Given a phone number, find a geographically close number within Distance miles. Distance defaults to 25 miles.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">NearLatLong</td>
 * <td align="left">Given a latitude/longitude pair lat,long find geographically close numbers within Distance miles.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">Distance</td>
 * <td align="left">Specifies the search radius for a Near- query in miles. If not specified this defaults to 25 miles. Maximum searchable distance is 500 miles.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">InPostalCode</td>
 * <td align="left">Limit results to a particular postal code. Given a phone number, search within the same postal code as that number.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">InRegion</td>
 * <td align="left">Limit results to a particular region (i.e. State/Province). Given a phone number, search within the same Region as that number.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">InRateCenter</td>
 * <td align="left">Limit results to a specific rate center, or given a phone number search within the same rate center as that number. Requires InLata to be set as well.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">InLata</td>
 * <td align="left">Limit results to a specific Local access and transport area (LATA - http://en.wikipedia.org/wiki/Local_access_and_transport_area). Given a phone number, search within the same LATA as that number.</td>
 * </tr>
 * </tbody>
 * </table>
 * @author jean.deruelle@telestax.com
 */
public class PhoneNumberSearchFilters {
    String areaCode;
    Pattern filterPattern;
    Boolean smsEnabled;
    Boolean mmsEnabled;
    Boolean voiceEnabled;
    Boolean faxEnabled;
    Boolean ussdEnabled;
    String nearNumber;
    String nearLatLong;
    String distance;
    String inPostalCode;
    String inRegion;
    String inRateCenter;
    String inLata;
    int rangeSize;
    int rangeIndex;

    PhoneNumberType phoneNumberType;

    public PhoneNumberSearchFilters() {
    }

    public PhoneNumberSearchFilters(String areaCode, Pattern filterPattern, Boolean smsEnabled, Boolean mmsEnabled, Boolean voiceEnabled,
            Boolean faxEnabled, Boolean ussdEnabled, String nearNumber, String nearLatLong, String distance, String inPostalCode, String inRegion,
            String inRateCenter, String inLata, int rangeSize, int rangeIndex, PhoneNumberType phoneNumberType) {
        this.areaCode = areaCode;
        this.filterPattern = filterPattern;
        this.smsEnabled = smsEnabled;
        this.mmsEnabled = mmsEnabled;
        this.voiceEnabled = voiceEnabled;
        this.faxEnabled = faxEnabled;
        this.ussdEnabled = ussdEnabled;
        this.nearNumber = nearNumber;
        this.nearLatLong = nearLatLong;
        this.distance = distance;
        this.inPostalCode = inPostalCode;
        this.inRegion = inRegion;
        this.inRateCenter = inRateCenter;
        this.inLata = inLata;
        this.rangeSize = rangeSize;
        this.rangeIndex = rangeIndex;
        this.phoneNumberType = phoneNumberType;
    }

    /**
     * @return the areaCode
     */
    public String getAreaCode() {
        return areaCode;
    }

    /**
     * @param areaCode the areaCode to set
     */
    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    /**
     * @return the filterPattern
     */
    public Pattern getFilterPattern() {
        return filterPattern;
    }

    /**
     * @param filterPattern the filterPattern to set
     */
    public void setFilterPattern(Pattern filterPattern) {
        this.filterPattern = filterPattern;
    }

    /**
     * @return the smsEnabled
     */
    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    /**
     * @param smsEnabled the smsEnabled to set
     */
    public void setSmsEnabled(Boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    /**
     * @return the mmsEnabled
     */
    public Boolean getMmsEnabled() {
        return mmsEnabled;
    }

    /**
     * @param mmsEnabled the mmsEnabled to set
     */
    public void setMmsEnabled(Boolean mmsEnabled) {
        this.mmsEnabled = mmsEnabled;
    }

    /**
     * @return the voiceEnabled
     */
    public Boolean getVoiceEnabled() {
        return voiceEnabled;
    }

    /**
     * @param voiceEnabled the voiceEnabled to set
     */
    public void setVoiceEnabled(Boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
    }

    /**
     * @return the faxEnabled
     */
    public Boolean getFaxEnabled() {
        return faxEnabled;
    }

    /**
     * @param faxEnabled the faxEnabled to set
     */
    public void setFaxEnabled(Boolean faxEnabled) {
        this.faxEnabled = faxEnabled;
    }

    /**
     * @return the ussdEnabled
     */
    public Boolean getUssdEnabled() {
        return ussdEnabled;
    }

    /**
     * @param ussdEnabled the ussdEnabled to set
     */
    public void setUssdEnabled(Boolean ussdEnabled) {
        this.ussdEnabled = ussdEnabled;
    }

    /**
     * @return the nearNumber
     */
    public String getNearNumber() {
        return nearNumber;
    }

    /**
     * @param nearNumber the nearNumber to set
     */
    public void setNearNumber(String nearNumber) {
        this.nearNumber = nearNumber;
    }

    /**
     * @return the nearLatLong
     */
    public String getNearLatLong() {
        return nearLatLong;
    }

    /**
     * @param nearLatLong the nearLatLong to set
     */
    public void setNearLatLong(String nearLatLong) {
        this.nearLatLong = nearLatLong;
    }

    /**
     * @return the distance
     */
    public String getDistance() {
        return distance;
    }

    /**
     * @param distance the distance to set
     */
    public void setDistance(String distance) {
        this.distance = distance;
    }

    /**
     * @return the inPostalCode
     */
    public String getInPostalCode() {
        return inPostalCode;
    }

    /**
     * @param inPostalCode the inPostalCode to set
     */
    public void setInPostalCode(String inPostalCode) {
        this.inPostalCode = inPostalCode;
    }

    /**
     * @return the inRegion
     */
    public String getInRegion() {
        return inRegion;
    }

    /**
     * @param inRegion the inRegion to set
     */
    public void setInRegion(String inRegion) {
        this.inRegion = inRegion;
    }

    /**
     * @return the inRateCenter
     */
    public String getInRateCenter() {
        return inRateCenter;
    }

    /**
     * @param inRateCenter the inRateCenter to set
     */
    public void setInRateCenter(String inRateCenter) {
        this.inRateCenter = inRateCenter;
    }

    /**
     * @return the inLata
     */
    public String getInLata() {
        return inLata;
    }

    /**
     * @param inLata the inLata to set
     */
    public void setInLata(String inLata) {
        this.inLata = inLata;
    }

    /**
     * @return the rangeSize
     */
    public int getRangeSize() {
        return rangeSize;
    }

    /**
     * @param rangeSize the rangeSize to set
     */
    public void setRangeSize(int rangeSize) {
        this.rangeSize = rangeSize;
    }

    /**
     * @return the rangeIndex
     */
    public int getRangeIndex() {
        return rangeIndex;
    }

    /**
     * @param rangeIndex the rangeIndex to set
     */
    public void setRangeIndex(int rangeIndex) {
        this.rangeIndex = rangeIndex;
    }

    /**
     * @return the type of phone number to search for
     */
    public PhoneNumberType getPhoneNumberTypeSearch() {
        return phoneNumberType;
    }

    /**
     * @param phoneNumberTypeSearch the type of phone number to search for
     */
    public void setPhoneNumberTypeSearch(PhoneNumberType phoneNumberTypeSearch) {
        this.phoneNumberType = phoneNumberTypeSearch;
    }
}
