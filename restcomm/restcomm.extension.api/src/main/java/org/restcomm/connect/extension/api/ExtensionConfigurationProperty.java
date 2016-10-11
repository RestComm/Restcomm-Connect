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

package org.restcomm.connect.extension.api;

import org.joda.time.DateTime;

/**
 * Created by gvagenas on 11/10/2016.
 */
public class ExtensionConfigurationProperty {

    private String extension;
    private String property;
    private String extraParameter;
    private String propertyValue;
    private DateTime dateCreated;
    private DateTime dateUpdated;

    public ExtensionConfigurationProperty(final String extension, final String property, final String extraParameter, final String propertyValue, final DateTime dateCreated, final DateTime dateUpdated) {
        this.extension = extension;
        this.property = property;
        this.extraParameter = extraParameter;
        this.propertyValue = propertyValue;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
    }

    public ExtensionConfigurationProperty(String extension, String property, String extraParameter, String propertyValue) {
        this(extension, property, extraParameter, propertyValue, DateTime.now(), DateTime.now());
    }

    public ExtensionConfigurationProperty(String extension, String property, String extraParameter) {
        this(extension, property, extraParameter, null);
    }

    public ExtensionConfigurationProperty(String extension, String property) {
        this(extension, property, null, null);
    }

    public ExtensionConfigurationProperty(String extension) {
        this(extension, null);
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getExtraParameter() {
        return extraParameter;
    }

    public void setExtraParameter(String extraParameter) {
        this.extraParameter = extraParameter;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(DateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(DateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }
}
