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

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.configuration.sets.MainConfigurationSet;
import org.restcomm.connect.commons.configuration.sources.ConfigurationSource;
import org.restcomm.connect.commons.common.http.SslMode;
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
public class MainConfigurationSetImpl extends ConfigurationSet implements MainConfigurationSet {

    private static final String SSL_MODE_KEY = "http-client.ssl-mode";
    private static final String HTTP_RESPONSE_TIMEOUT = "http-client.response-timeout";
    private static final SslMode SSL_MODE_DEFAULT = SslMode.strict;
    private SslMode sslMode;
    private int responseTimeout;
    private static final String USE_HOSTNAME_TO_RESOLVE_RELATIVE_URL_KEY = "http-client.use-hostname-to-resolve-relative-url";
    private static final String HOSTNAME_TO_USE_FOR_RELATIVE_URLS_KEY = "http-client.hostname";
    private static final boolean RESOLVE_RELATIVE_URL_WITH_HOSTNAME_DEFAULT = true;
    private boolean useHostnameToResolveRelativeUrls;
    private String hostname;
    private String instanceId;
    private String apiVersion;

    public static final String BYPASS_LB_FOR_CLIENTS = "bypass-lb-for-clients";
    private boolean bypassLbForClients = false;

    public MainConfigurationSetImpl(ConfigurationSource source) {
        super(source);
        SslMode sslMode;
        boolean resolveRelativeUrlWithHostname;
        String resolveRelativeUrlHostname;
        boolean bypassLb = false;
        int timeout = 5000;

        try {
            timeout = Integer.parseInt(source.getProperty(HTTP_RESPONSE_TIMEOUT));
        } catch (Exception e) {
            throw new RuntimeException("Error initializing '" + HTTP_RESPONSE_TIMEOUT + "' configuration setting", e);
        }
        // http-client.ssl-mode
        try {
            sslMode = SSL_MODE_DEFAULT;
            String sslModeRaw = source.getProperty(SSL_MODE_KEY);
            if ( ! StringUtils.isEmpty(sslModeRaw) )
                sslMode = SslMode.valueOf(sslModeRaw);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing '" + SSL_MODE_KEY  + "' configuration setting", e);
        }
        this.sslMode = sslMode;

        // http-client.hostname
        // http-client.use-hostname-to-resolve-relative-url
        try {
            resolveRelativeUrlWithHostname = RESOLVE_RELATIVE_URL_WITH_HOSTNAME_DEFAULT;
            resolveRelativeUrlWithHostname = Boolean.valueOf(source.getProperty(USE_HOSTNAME_TO_RESOLVE_RELATIVE_URL_KEY));
            resolveRelativeUrlHostname = source.getProperty("http-client.hostname");
            bypassLb = Boolean.valueOf(source.getProperty(BYPASS_LB_FOR_CLIENTS));
        } catch (Exception e) {
            throw new RuntimeException("Error initializing '" + USE_HOSTNAME_TO_RESOLVE_RELATIVE_URL_KEY + "' configuration setting", e);
        }
        this.responseTimeout = timeout;
        this.useHostnameToResolveRelativeUrls = resolveRelativeUrlWithHostname;
        this.hostname = resolveRelativeUrlHostname;
        bypassLbForClients = bypassLb;
        apiVersion = source.getProperty("runtime-settings.api-version");
    }

    public MainConfigurationSetImpl(SslMode sslMode, int responseTimeout, boolean useHostnameToResolveRelativeUrls, String hostname, String instanceId, boolean bypassLbForClients) {
        super(null);
        this.sslMode = sslMode;
        this.responseTimeout = responseTimeout;
        this.useHostnameToResolveRelativeUrls = useHostnameToResolveRelativeUrls;
        this.hostname = hostname;
        this.instanceId = instanceId;
        this.bypassLbForClients = bypassLbForClients;
    }

    @Override
    public SslMode getSslMode() {
        return sslMode;
    }

    @Override
    public int getResponseTimeout() {
        return responseTimeout;
    }

    @Override
    public boolean isUseHostnameToResolveRelativeUrls() {
        return useHostnameToResolveRelativeUrls;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public boolean getBypassLbForClients() { return bypassLbForClients; }

    @Override
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String getInstanceId() { return this.instanceId; }

    public void setSslMode(SslMode sslMode) {
        this.sslMode = sslMode;
    }

    public void setResponseTimeout(int responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public void setUseHostnameToResolveRelativeUrls(boolean useHostnameToResolveRelativeUrls) {
        this.useHostnameToResolveRelativeUrls = useHostnameToResolveRelativeUrls;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setBypassLbForClients(boolean bypassLbForClients) {
        this.bypassLbForClients = bypassLbForClients;
    }

    public String getApiVersion() {
        return apiVersion;
    }
}
