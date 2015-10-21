package org.mobicents.servlet.restcomm.rvd.configuration;

import org.mobicents.servlet.restcomm.rvd.commons.http.SslMode;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

public class RestcommConfig {
    SslMode sslMode;
    String hostname;
    boolean useHostnameToResolveRelativeUrl;

    public RestcommConfig() {
        super();
        this.sslMode = SslMode.strict;
        this.hostname = null;
        this.useHostnameToResolveRelativeUrl = true;
    }

    public RestcommConfig(String sslMode, String hostname, String useHostnameToResolveRelativeUrl) {
        super();
        //  sslMode option
        this.sslMode = SslMode.strict;
        if ( ! RvdUtils.isEmpty(sslMode) )
            this.sslMode = SslMode.valueOf(sslMode);
        // hostname option
        this.hostname = hostname;
        // useHostnameToResolveRelativeUrl options
        try {
            this.useHostnameToResolveRelativeUrl = Boolean.parseBoolean(useHostnameToResolveRelativeUrl);
        } catch (Exception e) {
            this.useHostnameToResolveRelativeUrl = true; // default
        }
    }

    public SslMode getSslMode() {
        return sslMode;
    }

    public String getHostname() {
        return hostname;
    }

    public boolean isUseHostnameToResolveRelativeUrl() {
        return useHostnameToResolveRelativeUrl;
    }
}
