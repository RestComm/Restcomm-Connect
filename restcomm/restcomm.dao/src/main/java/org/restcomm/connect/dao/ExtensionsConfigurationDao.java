/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.restcomm.connect.dao;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.extension.api.ConfigurationException;
import org.restcomm.connect.extension.api.ExtensionConfiguration;

import java.util.List;

/**
 * Created by gvagenas on 11/10/2016.
 */
public interface ExtensionsConfigurationDao {
    /**
     * Add a new ExtensionConfiguration
     * @param extensionConfiguration
     */
    void addConfiguration(ExtensionConfiguration extensionConfiguration) throws ConfigurationException;

    /**
     * Update an existing ExtensionConfiguration
     * @param extensionConfiguration
     */
    void updateConfiguration(ExtensionConfiguration extensionConfiguration) throws ConfigurationException;

    /**
     * Get extension configuration by extension name
     * @param extensionName
     * @return ExtensionConfiguration
     */
    ExtensionConfiguration getConfigurationByName(String extensionName);

    /**
     * Get extension configuration by Sid
     * @param extensionSid
     * @return ExtensionConfiguration
     */
    ExtensionConfiguration getConfigurationBySid(Sid extensionSid);

    /**
     * Get all extension configuration
     * @return List<ExtensionConfiguration>
     */
    List<ExtensionConfiguration> getAllConfiguration();

    /**
     * Get all extension configuration of a specific type
     * @return List<ExtensionConfiguration>
     */
    List<ExtensionConfiguration> getAllConfigurationByType(ExtensionConfiguration.configurationType type);

    /**
     * Delete extension configuration by extension name
     * @param extensionName
     */
    void deleteConfigurationByName(String extensionName);

    /**
     * Delete extension configuration by Sid
     * @param extensionSid
     */
    void deleteConfigurationBySid(Sid extensionSid);

    /**
     * Check if there is a newer version of the configuration in the DB using extension name
     * @param extensionName
     * @param dateTime
     * @return
     */
    boolean isLatestVersionByName(String extensionName, DateTime dateTime);

    /**
     * Check if there is a newer version of the configuration in the DB using extension sid
     * @param extensionSid
     * @param dateTime
     * @return
     */
    boolean isLatestVersionBySid(Sid extensionSid, DateTime dateTime);


    /**
     * Validate extension configuration based on the type of the configuration data
     * @param extensionConfiguration
     * @return
     */
    boolean validate(ExtensionConfiguration extensionConfiguration);

    /**
     * Get account specific ExtensionConfiguration
     * @param accountSid
     * @param extensionSid
     * @return ExtensionConfiguration
     */
    ExtensionConfiguration getAccountExtensionConfiguration(String accountSid, String extensionSid);

    /**
     * Add a new account specific ExtensionConfiguration
     * @param extensionConfiguration
     * @param accountSid
     */
    void addAccountExtensionConfiguration(ExtensionConfiguration extensionConfiguration, Sid accountSid) throws ConfigurationException;

    /**
     * Update an existing account specific ExtensionConfiguration
     * @param extensionConfiguration
     * @param accountSid
     */
    void updateAccountExtensionConfiguration(ExtensionConfiguration extensionConfiguration, Sid accountSid) throws ConfigurationException;

    /**
     * Delete account specific ExtensionConfiguration
     * @param accountSid
     * @param extensionSid
     */
    void deleteAccountExtensionConfiguration(String accountSid, String extensionSid);
}
