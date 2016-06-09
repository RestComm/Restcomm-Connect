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

import org.apache.log4j.Logger;
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

    protected Logger logger = Logger.getLogger(MainConfigurationSet.class);

    private static final String SSL_MODE_KEY = "http-client.ssl-mode";
    private static final String HTTP_RESPONSE_TIMEOUT = "http-client.response-timeout";
    private static final SslMode SSL_MODE_DEFAULT = SslMode.strict;
    private final SslMode sslMode;
    private final int responseTimeout;
    private static final String USE_HOSTNAME_TO_RESOLVE_RELATIVE_URL_KEY = "http-client.use-hostname-to-resolve-relative-url";
    private static final String HOSTNAME_TO_USE_FOR_RELATIVE_URLS_KEY = "http-client.hostname";
    private static final boolean RESOLVE_RELATIVE_URL_WITH_HOSTNAME_DEFAULT = true;
    private final boolean useHostnameToResolveRelativeUrls;
    private final String hostname;
    private String instanceId;

    public static final String BYPASS_LB_FOR_CLIENTS = "bypass-lb-for-clients";
    private boolean bypassLbForClients = false;
    // keycloak related properties
    private static final String IDENTITY_AUTH_SERVER_URL = "identity.auth-server-url";
    private static final String IDENTITY_REALM_PUBLIC_KEY = "identity.realm-public-key";
    private static final String IDENTITY_REALM = "identity.realm";
    private String identityAuthServerUrl;
    private String identityRealmPublicKey;
    private String identityRealm;


    public MainConfigurationSet(ConfigurationSource source) {
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
        // initialize keycloak properties
        initIdentity(source);

    }

    private void initIdentity(ConfigurationSource source) {
        // identity.auth-server-url
        this.identityAuthServerUrl = source.getProperty(IDENTITY_REALM_PUBLIC_KEY);
        this.identityRealm = source.getProperty(IDENTITY_REALM);
        this.identityRealmPublicKey = source.getProperty(IDENTITY_REALM_PUBLIC_KEY);
        if (!StringUtils.isEmpty(this.identityAuthServerUrl)) {
            if (StringUtils.isEmpty(this.identityRealm) || StringUtils.isEmpty(this.identityRealmPublicKey)) {
                logger.error("Inconsistent identity configuration! Keycloak based authorization will not work.");
                return;
            }
        }
    }

    public SslMode getSslMode() {
        return sslMode;
    }

    public int getResponseTimeout() {
        return responseTimeout;
    }

    public boolean isUseHostnameToResolveRelativeUrls() {
        return useHostnameToResolveRelativeUrls;
    }

    public String getHostname() {
        return hostname;
    }

    public boolean getBypassLbForClients() { return bypassLbForClients; }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() { return this.instanceId; }

    public String getIdentityAuthServerUrl() {return identityAuthServerUrl;}

    public String getIdentityRealmPublicKey() {return identityRealmPublicKey;}

    public String getIdentityRealm() {return identityRealm;}
}
