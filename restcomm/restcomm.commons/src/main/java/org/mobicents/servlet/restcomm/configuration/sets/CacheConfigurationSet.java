package org.mobicents.servlet.restcomm.configuration.sets;

import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

public class CacheConfigurationSet extends ConfigurationSet {
    public static final String CACHE_NO_WAV_KEY = "runtime-settings.cache-no-wav";
    public static final String CACHE_PATH_KEY = "runtime-settings.cache-path";
    public static final String CACHE_URI_KEY = "runtime-settings.cache-uri";

    private boolean noWavCache;
    private String cachePath;
    private String cacheUri;

    public CacheConfigurationSet(ConfigurationSource source) {
        super(source);

        String value = source.getProperty(CACHE_NO_WAV_KEY);

        // default flag value is "false" if appropriate key "cache-no-wav" is absent in configuration *.xml file
        noWavCache = (value == null) ? false : Boolean.valueOf(source.getProperty(CACHE_NO_WAV_KEY));

        cachePath = source.getProperty(CACHE_PATH_KEY);

        cacheUri = source.getProperty(CACHE_URI_KEY);
    }

    public boolean isNoWavCache() {
        return noWavCache;
    }

    public String getCachePath() {
        return cachePath;
    }

    public String getCacheUri() {
        return cacheUri;
    }
}
