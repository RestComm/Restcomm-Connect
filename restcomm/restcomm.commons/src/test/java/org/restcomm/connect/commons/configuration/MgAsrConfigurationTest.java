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

package org.restcomm.connect.commons.configuration;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Test;
import org.restcomm.connect.commons.configuration.sets.impl.MgAsrConfigurationSet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MgAsrConfigurationTest {

    private List<String> expectedDrivers = Arrays.asList("driver1", "driver2");

    private RestcommConfiguration createCfg(final String cfgFileName) throws ConfigurationException,
                                                                             MalformedURLException {
        URL url = this.getClass().getResource(cfgFileName);

        Configuration xml = new XMLConfiguration(url);
        return new RestcommConfiguration(xml);
    }

    public MgAsrConfigurationTest() {
        super();
    }

    @Test
    public void checkMgAsrSection() throws ConfigurationException, MalformedURLException {
        MgAsrConfigurationSet conf = createCfg("/restcomm.xml").getMgAsr();

        assertTrue(CollectionUtils.isEqualCollection(expectedDrivers, conf.getDrivers()));
        assertEquals("driver1", conf.getDefaultDriver());
    }

    @Test
    public void checkNoMgAsrSection() throws ConfigurationException, MalformedURLException {
        MgAsrConfigurationSet conf = createCfg("/restcomm-no-mg-asr.xml").getMgAsr();

        assertTrue(CollectionUtils.isEqualCollection(Collections.emptyList(), conf.getDrivers()));
        assertNull(conf.getDefaultDriver());
    }

}
