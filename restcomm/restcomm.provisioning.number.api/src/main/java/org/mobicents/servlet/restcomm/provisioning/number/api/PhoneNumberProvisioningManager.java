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

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;

/**
 * @author jean.deruelle@telestax.com
 *
 */
public interface PhoneNumberProvisioningManager {

    /**
     * Initialize the Manager with the restcommm configuration passed in restcomm.xml
     *
     * @param phoneNumberProvisioningConfiguration the configuration
     * @param teleStaxProxyConfiguration if TeleStax proxy is enabled for DID provisioning
     */
    void init(Configuration phoneNumberProvisioningConfiguration, Configuration teleStaxProxyConfiguration);

    /**
     * Search for a list of numbers matching the various parameters
     *
     * @param country 2 letters Country Code as defined per http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2.
     * @param areaCode the areaCode (US only)
     * @param searchPattern A matching pattern. Ex: 888
     * @param smsEnabled if the number should be SMS capable
     * @param mmsEnabled if the number should be MMS capable
     * @param voiceEnabled if the number should be Voice capable
     * @param faxEnabled if the number should be Fax capable
     * @param rangeSize Range size (max 100, default 10). Ex: 25
     * @param rangeIndex Range index (>0, default 1). Ex: 2
     * @return List of matching numbers
     */
    List<PhoneNumber> searchForNumbers(String country, String areaCode, Pattern searchPattern, boolean smsEnabled,
            boolean mmsEnabled, boolean voiceEnabled, boolean faxEnabled, int rangeSize, int rangeIndex);

    /**
     * Purchase a given inbound number.
     *
     * @param country 2 letters Country Code as defined per http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2.
     * @param number An available inbound number - defined as msisdn Ex: 34911067000 returned from
     *        {@link #searchForNumbers(String, String, String, int, int) searchForNumbers} method.
     * @param smsHttpURL The URL where the SMS received to the inbound number should be sent to
     * @param smsType The associated system type for SMPP client only Ex: inbound
     * @param voiceURL The SIP URL to which an incoming call or message should be sent to.
     * @return true if the number was bought successfuly, false otherwise.
     */
    boolean buyNumber(String country, String number, String smsHttpURL, String smsType, String voiceURL);

    /**
     * Update the callbacks URL for an already purchased inbound number.
     *
     * @param country 2 letters Country Code as defined per http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2.
     * @param number the inbound number for which to modify the callnbacks - defined as msisdn Ex: 34911067000
     * @param smsHttpURL The URL where the SMS received to the inbound number should be sent to
     * @param smsType The associated system type for SMPP client only Ex: inbound
     * @param voiceURL The SIP URL to which an incoming call or message should be sent to.
     * @return true if the number was bought successfuly, false otherwise.
     */
    boolean updateNumber(String country, String number, String smsHttpURL, String smsType, String voiceURL);

    /**
     * Cancel an already purchased inbound number.
     *
     * @param country 2 letters Country Code as defined per http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2.
     * @param number the inbound number to cancel -defined as msisdn Ex: 34911067000
     */
    boolean cancelNumber(String country, String number);
}
