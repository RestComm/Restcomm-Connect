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


/**
 * <p>
 * The following optional parameters for the DID provider to set when a number is bought or updated
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th align="left">Property</th>
 * <th align="left">Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td class='notranslate' align="left">VoiceUrl</td>
 * <td align="left">The URL in host:port format that the DID provider should request when somebody dials the new phone number.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">VoiceMethod</td>
 * <td align="left">The method that should be used to request the VoiceUrl. Only SIP or SIPS supported currently. Defaults to SIP.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">doVoiceCallerIdLookup</td>
 * <td align="left">Do a lookup of a caller's name from the CNAM database if the service provider support it. Either true or false. Defaults to false..</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">SmsUrl</td>
 * <td align="left">The URL in host:port format (SIP, SMPP, or SS7 MAP) that the DID provider should request when somebody sends an SMS to the phone number.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">SMSMethod</td>
 * <td align="left">The method that should be used to request the SmsUrl. SS7, SMPP, SIP or SIPS supported currently. Defaults to SIP.
 * If SMPP or SS7 are used, the URL of the SMSC will be provided</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">UssdUrl</td>
 * <td align="left">The URL in host:port format that the DID provider should request when somebody sends a USSD message to the phone number.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">UssdMethod</td>
 * <td align="left">The method that should be used to request the UssdUrl. SS7, SIP or SIPS supported currently. Defaults to SIP.
 * If SS7 is used, the URL of the USSD will be provided</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">FaxUrl</td>
 * <td align="left">The URL in host:port format that the DID provider should request when somebody sends a Fax message to the phone number.</td>
 * </tr>
 * <tr>
 * <td class='notranslate' align="left">FaxMethod</td>
 * <td align="left">The method that should be used to request the FaxUrl. SIP or SIPS supported currently. Defaults to SIP.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author jean.deruelle@telestax.com
 */
public final class PhoneNumberParameters {
    private String voiceUrl = null;
    private String voiceMethod = null;
    private boolean doVoiceCallerIdLookup = false;
    private String smsUrl = null;
    private String smsMethod = null;
    private String ussdUrl = null;
    private String ussdMethod = null;
    private String faxUrl = null;
    private String faxMethod = null;
    private PhoneNumberType phoneNumberType = PhoneNumberType.Global;

    public PhoneNumberParameters() {}

    /**
     * @param voiceUrl
     * @param voiceMethod
     * @param voiceCallerIdLookup
     * @param smsUrl
     * @param smsMethod
     * @param ussdUrl
     * @param ussdMethod
     * @param faxUrl
     * @param faxMethod
     */
    public PhoneNumberParameters(String voiceUrl, String voiceMethod, boolean doVoiceCallerIdLookup, String smsUrl,
            String smsMethod, String ussdUrl, String ussdMethod, String faxUrl, String faxMethod) {
        this.voiceUrl = voiceUrl;
        this.voiceMethod = voiceMethod;
        this.doVoiceCallerIdLookup = doVoiceCallerIdLookup;
        this.smsUrl = smsUrl;
        this.smsMethod = smsMethod;
        this.ussdUrl = ussdUrl;
        this.ussdMethod = ussdMethod;
        this.faxUrl = faxUrl;
        this.faxMethod = faxMethod;
    }

    /**
     * @return the voiceUrl
     */
    public String getVoiceUrl() {
        return voiceUrl;
    }

    /**
     * @param voiceUrl the voiceUrl to set
     */
    public void setVoiceUrl(String voiceUrl) {
        this.voiceUrl = voiceUrl;
    }

    /**
     * @return the voiceMethod
     */
    public String getVoiceMethod() {
        return voiceMethod;
    }

    /**
     * @param voiceMethod the voiceMethod to set
     */
    public void setVoiceMethod(String voiceMethod) {
        this.voiceMethod = voiceMethod;
    }

    /**
     * @return the doVoiceCallerIdLookup
     */
    public boolean isDoVoiceCallerIdLookup() {
        return doVoiceCallerIdLookup;
    }

    /**
     * @param doVoiceCallerIdLookup the doVoiceCallerIdLookup to set
     */
    public void setDoVoiceCallerIdLookup(boolean doVoiceCallerIdLookup) {
        this.doVoiceCallerIdLookup = doVoiceCallerIdLookup;
    }

    /**
     * @return the smsUrl
     */
    public String getSmsUrl() {
        return smsUrl;
    }

    /**
     * @param smsUrl the smsUrl to set
     */
    public void setSmsUrl(String smsUrl) {
        this.smsUrl = smsUrl;
    }

    /**
     * @return the smsMethod
     */
    public String getSmsMethod() {
        return smsMethod;
    }

    /**
     * @param smsMethod the smsMethod to set
     */
    public void setSmsMethod(String smsMethod) {
        this.smsMethod = smsMethod;
    }

    /**
     * @return the ussdUrl
     */
    public String getUssdUrl() {
        return ussdUrl;
    }

    /**
     * @param ussdUrl the ussdUrl to set
     */
    public void setUssdUrl(String ussdUrl) {
        this.ussdUrl = ussdUrl;
    }

    /**
     * @return the ussdMethod
     */
    public String getUssdMethod() {
        return ussdMethod;
    }

    /**
     * @param ussdMethod the ussdMethod to set
     */
    public void setUssdMethod(String ussdMethod) {
        this.ussdMethod = ussdMethod;
    }

    /**
     * @return the faxUrl
     */
    public String getFaxUrl() {
        return faxUrl;
    }

    /**
     * @param faxUrl the faxUrl to set
     */
    public void setFaxUrl(String faxUrl) {
        this.faxUrl = faxUrl;
    }

    /**
     * @return the faxMethod
     */
    public String getFaxMethod() {
        return faxMethod;
    }

    /**
     * @param faxMethod the faxMethod to set
     */
    public void setFaxMethod(String faxMethod) {
        this.faxMethod = faxMethod;
    }

    /**
     * @return the phoneNumberType
     */
    public PhoneNumberType getPhoneNumberType() {
        return phoneNumberType;
    }

    /**
     * @param phoneNumberType the phoneNumberType to set
     */
    public void setPhoneNumberType(PhoneNumberType phoneNumberType) {
        this.phoneNumberType = phoneNumberType;
    }
}
