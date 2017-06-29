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

package org.restcomm.connect.commons.configuration.sets.impl;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.configuration.sources.ConfigurationSource;

import java.util.Collections;
import java.util.List;

/**
 * Created by gdubina on 26.06.17.
 */
public class MgAsrConfigurationSet extends ConfigurationSet {

    private final List<String> drivers;

    private final String defaultDriver;

    public MgAsrConfigurationSet(ConfigurationSource source, Configuration config) {
        super(source);
        this.drivers = Collections.unmodifiableList(config.getList("runtime-settings.mg-asr-drivers.driver"));
        this.defaultDriver = config.getString("runtime-settings.mg-asr-drivers[@default]");
    }

    public List<String> getDrivers() {
        return this.drivers;
    }

    public String getDefaultDriver() {
        return defaultDriver;
    }
}
