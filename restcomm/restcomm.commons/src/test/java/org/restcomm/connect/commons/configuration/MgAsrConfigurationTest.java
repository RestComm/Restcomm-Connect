package org.restcomm.connect.commons.configuration;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.configuration.sets.impl.MgAsrConfigurationSet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

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
