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

import org.apache.commons.configuration.Configuration;

/**
 * <p>Interface for plugging Phone Number Provisioning Providers.</p>
 *
 * <p>Its init method will be called on startup to initialize and pass the parameters found in restcomm.xml phone-number-provisioning tag as an Apache Commons Configuration Object</p>
 * <p>the following methods will be called by RestComm during runtime</p>
 * <ul>
 *  <li>searchForNumbers : when a user is searching for a number to provision</li>
 *  <li>buyNumber : when a user is purchasing a number</li>
 *  <li>updateNumber : when a user is updating the properties/callback URLs of an already purchased number</li>
 *  <li>cancelNumber : when a user is cancelling a number</li>
 * </ul>
 *
 * @author jean.deruelle@telestax.com
 */
public interface PhoneNumberProvisioningManager {

    /**
     * Initialize the Manager with the RestComm configuration passed in restcomm.xml
     *
     * @param phoneNumberProvisioningConfiguration the configuration
     * @param teleStaxProxyConfiguration the configuration from restcomm.xml contained within <phone-number-provisioning> tags
     * @param containerConfiguration with container configuration information
     */
    void init(Configuration phoneNumberProvisioningConfiguration, Configuration teleStaxProxyConfiguration, ContainerConfiguration containerConfiguration);

    /**
     * Search for a list of numbers matching the various parameters
     *
     * @param country 2 letters Country Code as defined per http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2.
     * @param listFilters contains all the filters that can be applied to restrict the results
     * @return List of matching numbers
     */
    List<PhoneNumber> searchForNumbers(String country, PhoneNumberSearchFilters listFilters);

    /**
     * Purchase a given phone number previously searched through {@link #searchForNumbers(String, org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberSearchFilters) searchForNumbers} method.
     *
     * @param phoneNumber An available phone number - defined as msisdn Ex: 34911067000 returned from
     *        {@link #searchForNumbers(String, org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberSearchFilters) searchForNumbers} method.
     * @param phoneNumberParameters parameters set on the phone number purchase so the Provider knows where to route incoming messages (be it voice or SMS, MMS, USSD)
     * @return true if the number was bought successfully, false otherwise.
     */
    boolean buyNumber(PhoneNumber phoneNumber, PhoneNumberParameters phoneNumberParameters);

    /**
     * Update the parameters for an already purchased phone number.
     *
     * @param number An available phone number - defined as msisdn Ex: 34911067000 returned from
     *        {@link #searchForNumbers(String, org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberSearchFilters) searchForNumbers} method.
     * @param phoneNumberParameters parameters set on the phone number purchase so the Provider knows where to route incoming messages (be it voice or SMS, MMS, USSD).
     * @return true if the number was updated successfully, false otherwise.
     */
    boolean updateNumber(PhoneNumber number, PhoneNumberParameters phoneNumberParameters);

    /**
     * Cancel an already purchased phone number.
     *
     * @param number the phonenumber to cancel -defined as msisdn Ex: 34911067000
     * @return true if the number was cancelled successfully, false otherwise.
     */
    boolean cancelNumber(PhoneNumber number);

    /**
     * Returns the list of supported countries by the phone number provider
     *
     * @return a list of 2 letters Country Code as defined per http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2, representing all countries where a number can be bought or searched in.
     */
    List<String> getAvailableCountries();
}
