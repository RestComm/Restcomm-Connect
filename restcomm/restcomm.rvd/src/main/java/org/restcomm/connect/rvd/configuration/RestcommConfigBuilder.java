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

package org.restcomm.connect.rvd.configuration;

import org.restcomm.connect.rvd.commons.http.SslMode;

public class RestcommConfigBuilder {
    private SslMode sslMode = SslMode.strict;
    private String hostname = null;
    private boolean useHostnameToResolveRelativeUrl = true;
    private String authServerUrl = null;
    private String realmPublicKey = null;
    private String realm = null;

    public RestcommConfigBuilder setSslMode(SslMode sslMode) {
        this.sslMode = sslMode;
        return this;
    }

    public RestcommConfigBuilder setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public RestcommConfigBuilder setUseHostnameToResolveRelativeUrl(boolean useHostnameToResolveRelativeUrl) {
        this.useHostnameToResolveRelativeUrl = useHostnameToResolveRelativeUrl;
        return this;
    }

    public RestcommConfigBuilder setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
        return this;
    }

    public RestcommConfigBuilder setRealmPublicKey(String realmPublicKey) {
        this.realmPublicKey = realmPublicKey;
        return this;
    }

    public RestcommConfigBuilder setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    public RestcommConfig build() {
        return new RestcommConfig(sslMode,hostname,useHostnameToResolveRelativeUrl, authServerUrl,realmPublicKey,realm);
    }
}