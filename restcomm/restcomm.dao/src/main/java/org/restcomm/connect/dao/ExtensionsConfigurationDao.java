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

import org.restcomm.connect.extension.api.ExtensionConfigurationProperty;

import java.util.List;

/**
 * Created by gvagenas on 11/10/2016.
 */
public interface ExtensionsConfigurationDao {
    /**
     * Add a new ExtensionConfiguration property
     * @param extensionsConfigurationProperty
     */
    void addConfigurationProperty(ExtensionConfigurationProperty extensionsConfigurationProperty);

    /**
     * Update an existing ExtensionConfiguration property
     * @param extensionConfigurationProperty
     */
    void updateConfigurationProperty(ExtensionConfigurationProperty extensionConfigurationProperty);

    /**
     * Get extension configuration property by extension name and property name
     * @param extension
     * @param property
     * @return ExtensionConfigurationProperty
     */
    ExtensionConfigurationProperty getConfigurationProperty(String extension, String property);

    /**
     * Get extension configuration property by extension name, property name and extra parameter.
     * Extra parameter could be for example client name or account sid
     * @param extension
     * @param property
     * @param extraParameter
     * @return ExtensionConfigurationProperty
     */
    ExtensionConfigurationProperty getConfigurationPropertyByExtraParameter(String extension, String property, String extraParameter);

    /**
     * Get whole extension configuration by extension name
     * @param extension
     * @return
     */
    List<ExtensionConfigurationProperty> getConfigurationByExtension(String extension);
}
