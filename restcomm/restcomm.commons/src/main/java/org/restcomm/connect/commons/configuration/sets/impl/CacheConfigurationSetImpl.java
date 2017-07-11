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

import org.restcomm.connect.commons.configuration.sets.CacheConfigurationSet;
import org.restcomm.connect.commons.configuration.sources.ConfigurationSource;

public class CacheConfigurationSetImpl extends ConfigurationSet implements CacheConfigurationSet {
    public static final String CACHE_NO_WAV_KEY = "runtime-settings.cache-no-wav";
    public static final String CACHE_PATH_KEY = "runtime-settings.cache-path";
    public static final String CACHE_URI_KEY = "runtime-settings.cache-uri";

    private boolean noWavCache;
    private String cachePath;
    private String cacheUri;

    public CacheConfigurationSetImpl(ConfigurationSource source) {
        super(source);

        String value = source.getProperty(CACHE_NO_WAV_KEY);

        // default flag value is "false" if appropriate key "cache-no-wav" is absent in configuration *.xml file
        noWavCache = (value == null) ? false : Boolean.valueOf(source.getProperty(CACHE_NO_WAV_KEY));

        cachePath = source.getProperty(CACHE_PATH_KEY);

        cacheUri = source.getProperty(CACHE_URI_KEY);
    }

    public CacheConfigurationSetImpl(boolean noWavCache, String cachePath, String cacheUri) {
        super(null);
        this.noWavCache = noWavCache;
        this.cachePath = cachePath;
        this.cacheUri = cacheUri;
    }

    @Override
    public boolean isNoWavCache() {
        return noWavCache;
    }

    @Override
    public String getCachePath() {
        return cachePath;
    }

    @Override
    public String getCacheUri() {
        return cacheUri;
    }

    public void setNoWavCache(boolean noWavCache) {
        this.noWavCache = noWavCache;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    public void setCacheUri(String cacheUri) {
        this.cacheUri = cacheUri;
    }
}
