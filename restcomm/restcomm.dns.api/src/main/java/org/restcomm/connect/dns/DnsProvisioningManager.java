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
package org.restcomm.connect.dns;

import org.apache.commons.configuration.Configuration;

public interface DnsProvisioningManager {

	/**
     * Initialize the Manager with the RestComm configuration passed in restcomm.xml
     *
     * @param dnsConfiguration the configuration from restcomm.xml contained within <dns-provisioning> tags
     */
    void init(Configuration dnsConfiguration);

    /**
     * @param name The name of the domain you want to perform the action on. 
     * Enter a sub domain name only. For example to add 'company1.restcomm.com',
     *  provide only 'company1' and provide hosted zone for 'restcomm.com'
     * @param hostedZoneId  hostedZoneId The ID of the hosted zone that contains the resource record sets that you want to change.
     * If none provided, then default will be used as per configuration
     * @return true if operation successful, false otherwise.
     */
    boolean createResourceRecord(final String name, final String hostedZoneId);

    /**
     * @param name The name of the domain you want to perform the action on. 
     * Enter a sub domain name only. For example to add 'company1.restcomm.com',
     *  provide only 'company1' and provide hosted zone for 'restcomm.com'
     * @param hostedZoneId  hostedZoneId The ID of the hosted zone that contains the resource record sets that you want to change.
     * If none provided, then default will be used as per configuration
     * @return true if operation successful, false otherwise.
     */
    boolean readResourceRecord(final String name, final String hostedZoneId);

    /**
     * @param name The name of the domain you want to perform the action on. 
     * Enter a sub domain name only. For example to add 'company1.restcomm.com',
     *  provide only 'company1' and provide hosted zone for 'restcomm.com'
     * @param hostedZoneId  hostedZoneId The ID of the hosted zone that contains the resource record sets that you want to change.
     * If none provided, then default will be used as per configuration
     * @return true if operation successful, false otherwise.
     */
    boolean updateResourceRecord(final String name, final String hostedZoneId);

    /**
     * @param name The name of the domain you want to perform the action on. 
     * Enter a sub domain name only. For example to add 'company1.restcomm.com',
     *  provide only 'company1' and provide hosted zone for 'restcomm.com'
     * @param hostedZoneId  hostedZoneId The ID of the hosted zone that contains the resource record sets that you want to change.
     * If none provided, then default will be used as per configuration
     * @return true if operation successful, false otherwise.
     */
    boolean deleteResourceRecord(final String name, final String hostedZoneId);
}
