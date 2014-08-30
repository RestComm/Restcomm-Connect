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
package org.mobicents.servlet.restcomm.telephony.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrLookup;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public class ConfigurationStringLookup extends StrLookup {
    private final Map<String, String> dictionary;

    public ConfigurationStringLookup() {
        super();
        dictionary = new HashMap<String, String>();
    }

    public void addProperty(final String name, final String value) {
        dictionary.put(name, value);
    }

    @Override
    public String lookup(final String key) {
        final String result = dictionary.get(key);
        if (result != null) {
            return result;
        } else {
            return key;
        }
    }
}
