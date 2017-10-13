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

package org.restcomm.connect.commons.configuration.sources;

import org.restcomm.connect.commons.configuration.sources.ConfigurationSource;

import java.util.HashMap;
import java.util.Map;

/*
 * Mockup configuration source. Fill it up in your tests with values that simulate the actual source
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 */
public class EditableConfigurationSource implements ConfigurationSource {

    Map<String,String> properties = new HashMap<String,String>();

    public EditableConfigurationSource() {
        // TODO Auto-generated constructor stub
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String getProperty (String key, String defValue) {
        String result = properties.get(key);
        if (key == null || key.isEmpty())
            result = defValue;
        return result;
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
}
