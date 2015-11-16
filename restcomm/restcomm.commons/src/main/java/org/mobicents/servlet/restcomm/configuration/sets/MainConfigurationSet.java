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

package org.mobicents.servlet.restcomm.configuration.sets;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;
import org.mobicents.servlet.restcomm.http.SslMode;
import org.apache.commons.lang.StringUtils;

/**
 * Provides a typed interface to a set of configuration options retrieved from a
 * configuration source.
 *
 * To add a new option in this set define its name as static fields and then initialize,
 * validate it in the constructor.
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
@Immutable
public class MainConfigurationSet extends ConfigurationSet {

    public static final String SSL_MODE_KEY = "http-client.ssl-mode";
    private static final SslMode SSL_MODE_DEFAULT = SslMode.strict;
    private final SslMode sslMode;
    public static final String USE_HOSTNAME_TO_RESOLVE_RELATIVE_URL_KEY = "http-client.use-hostname-to-resolve-relative-url";
    public static final String HOSTNAME_TO_USE_FOR_RELATIVE_URLS_KEY = "http-client.hostname";
    private static final boolean RESOLVE_RELATIVE_URL_WITH_HOSTNAME_DEFAULT = true;
    private final boolean useHostnameToResolveRelativeUrls;
    private final String hostname;

    public MainConfigurationSet(ConfigurationSource source) {
        super(source,null);
        SslMode sslMode;
        boolean resolveRelativeUrlWithHostname;
        String resolveRelativeUrlHostname;

        // http-client.ssl-mode
        try {
            sslMode = SSL_MODE_DEFAULT;
            String sslModeRaw = source.getProperty(SSL_MODE_KEY);
            if ( ! StringUtils.isEmpty(sslModeRaw) )
                sslMode = SslMode.valueOf(sslModeRaw);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing '" + SSL_MODE_KEY + "' configuration setting", e);
        }
        this.sslMode = sslMode;

        // http-client.hostname
        // http-client.use-hostname-to-resolve-relative-url
        try {
            resolveRelativeUrlWithHostname = RESOLVE_RELATIVE_URL_WITH_HOSTNAME_DEFAULT;
            resolveRelativeUrlWithHostname = Boolean.valueOf(source.getProperty(USE_HOSTNAME_TO_RESOLVE_RELATIVE_URL_KEY));
            resolveRelativeUrlHostname = source.getProperty("http-client.hostname");
        } catch (Exception e) {
            throw new RuntimeException("Error initializing '" + USE_HOSTNAME_TO_RESOLVE_RELATIVE_URL_KEY + "' configuration setting", e);
        }
        this.useHostnameToResolveRelativeUrls = resolveRelativeUrlWithHostname;
        this.hostname = resolveRelativeUrlHostname;

        this.reloaded();
    }

    public SslMode getSslMode() {
        return sslMode;
    }

    public boolean isUseHostnameToResolveRelativeUrls() {
        return useHostnameToResolveRelativeUrls;
    }

    public String getHostname() {
        return hostname;
    }

}
