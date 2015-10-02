package org.mobicents.servlet.restcomm.configuration.sets;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;
import org.mobicents.servlet.restcomm.http.SslMode;
import org.apache.commons.lang.StringUtils;

@Immutable
public class MainConfigurationSet extends ConfigurationSet {

    private static final String SSL_MODE_KEY = "http-client.ssl-mode";
    private static final SslMode SSL_MODE_DEFAULT = SslMode.allowall;
    private final SslMode sslMode;

    public MainConfigurationSet(ConfigurationSource source) {
        super(source);
        SslMode sslMode;
        try {
            sslMode = SSL_MODE_DEFAULT;
            String sslModeRaw = source.getProperty(SSL_MODE_KEY);
            if ( ! StringUtils.isEmpty(sslModeRaw) )
                sslMode = SslMode.valueOf(sslModeRaw);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing '" + SSL_MODE_KEY + "' configuration setting", e);
        }
        this.sslMode = sslMode;
    }

    public SslMode getSslMode() {
        return sslMode;
    }

}
