package org.restcomm.connect.commons.cache;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.commons.configuration.sets.CacheConfigurationSet;

public final class DiskCacheFactory {

    private final CacheConfigurationSet cfg;

    private final FileDownloader downloader;

    public DiskCacheFactory(Configuration cfg) {
        this(new RestcommConfiguration(cfg));
    }

    public DiskCacheFactory(RestcommConfiguration cfg) {
        this.cfg = cfg.getCache();
        this.downloader = new FileDownloader();
    }

    public DiskCache getDiskCache() {
        return new DiskCache(downloader, this.cfg.getCachePath(), this.cfg.getCacheUri(), false, cfg.isNoWavCache());
    }

    // constructor for compatibility with existing cache implementation
    public DiskCache getDiskCache(final String cachePath, final String cacheUri) {
        return new DiskCache(downloader, cachePath, cacheUri, true, cfg.isNoWavCache());
    }
}
