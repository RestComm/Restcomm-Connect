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
import org.restcomm.connect.commons.dao.Sid;

/**
 * Created by gvagenas on 12/10/2016.
 */
public class ExtensionSpecificConfiguration {
    public enum configurationType {
        XML, JSON
    }
    private Sid sid;
    //Specific SID can be either account sid or client sid
    private Sid specificSid;
    private String extensionName;
    private Object configurationData;
    private configurationType configurationType;
    private DateTime dateCreated;
    private DateTime dateUpdated;

    public ExtensionSpecificConfiguration(Sid sid, Sid specificSid, String extensionName, Object configurationData,
                                          configurationType configurationType, DateTime dateCreated, DateTime dateUpdated) {
        this.sid = sid;
        this.specificSid = specificSid;
        this.extensionName = extensionName;
        this.configurationData = configurationData;
        this.configurationType = configurationType;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
    }

    public ExtensionSpecificConfiguration(Sid sid, Sid specificSid, String extensionName, Object configurationData,
                                          configurationType configurationType, DateTime dateCreated) {
        this(sid, specificSid, extensionName, configurationData, configurationType, dateCreated, DateTime.now());
    }

    public Sid getSid() {
        return sid;
    }

    public Sid getSpecificSid() { return specificSid; }

    public String getExtensionName() {
        return extensionName;
    }

    public Object getConfigurationData() {
        return configurationData;
    }

    public configurationType getConfigurationType() { return configurationType; }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(DateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public void setConfigurationData(Object configurationData, configurationType configurationType) {
        this.configurationData = configurationData;
        this.configurationType = configurationType;
        this.dateUpdated = DateTime.now();
    }
}
