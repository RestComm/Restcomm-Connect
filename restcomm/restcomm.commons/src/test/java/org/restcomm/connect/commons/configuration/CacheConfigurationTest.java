package org.restcomm.connect.commons.configuration;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.commons.configuration.sets.CacheConfigurationSet;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CacheConfigurationTest {
    private static final String CACHE_PATH_VALUE = "${restcomm:home}/cache";
    private static final String CACHE_URI_VALUE = "http://127.0.0.1:8080/restcomm/cache";

    private RestcommConfiguration defaultCfg;
    private RestcommConfiguration enabledNoWavCacheCfg;
    private RestcommConfiguration disabledNoWavCacheCfg;

    private RestcommConfiguration createCfg(final String cfgFileName) throws ConfigurationException, MalformedURLException {
        URL url = this.getClass().getResource(cfgFileName);

        Configuration xml = new XMLConfiguration(url);
        return new RestcommConfiguration(xml);
    }

    @Before
    public void before() throws ConfigurationException, MalformedURLException {
        defaultCfg = createCfg("/restcomm.xml");
        enabledNoWavCacheCfg = createCfg("/restcomm-cache-no-wav-true.xml");
        disabledNoWavCacheCfg = createCfg("/restcomm-cache-no-wav-false.xml");
    }

    public CacheConfigurationTest() {
        super();
    }

    @Test
    public void checkCacheSection() throws ConfigurationException, MalformedURLException {
        CacheConfigurationSet cacheSet = defaultCfg.getCache();

        assertTrue(CACHE_PATH_VALUE.equals(cacheSet.getCachePath()));
        assertTrue(CACHE_URI_VALUE.equals(cacheSet.getCacheUri()));
    }

    @Test
    public void checkNoWavCacheFlagAbsenceInCfg() {
        // the default value of "noWavCache" flag is "false" if <cache-no-wav> tag doesn't present in *.xml
        assertFalse(defaultCfg.getCache().isNoWavCache());
    }

    @Test
    public void checkNoWavCacheFlagEnabledInCfg() {
        assertTrue(enabledNoWavCacheCfg.getCache().isNoWavCache());
    }

    @Test
    public void checkNoWavCacheFlagDisabledInCfg() {
        assertFalse(disabledNoWavCacheCfg.getCache().isNoWavCache());
    }
}
