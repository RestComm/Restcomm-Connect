/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

package org.restcomm.connect.rvd.model.client;

import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.model.HttpScheme;

/**
 * A DTO for the user profile
 *
 * @author Orestis Tsakiridis
 */
public class SettingsModel {

    private String apiServerHost;
    private Integer apiServerRestPort; // null values should be allowed too
    private String apiServerUsername;
    private String apiServerPass;
    private HttpScheme apiServerScheme;
    private String appStoreDomain;


    public static SettingsModel createDefault() {
        SettingsModel settingsModel = new SettingsModel(null, null);
        settingsModel.appStoreDomain = RvdConfiguration.DEFAULT_APPSTORE_DOMAIN;
        return settingsModel;
    }

    public SettingsModel() {
    }

    public SettingsModel(String apiServerHost, Integer apiServerRestPort) {
        super();
        this.apiServerHost = apiServerHost;
        this.apiServerRestPort = apiServerRestPort;
    }


    public SettingsModel(String apiServerHost, Integer apiServerRestPort, String apiServerUsername, String apiServerPass) {
        super();
        this.apiServerHost = apiServerHost;
        this.apiServerRestPort = apiServerRestPort;
        this.apiServerUsername = apiServerUsername;
        this.apiServerPass = apiServerPass;
    }


    public String getApiServerHost() {
        return apiServerHost;
    }

    public void setApiServerHost(String apiServerHost) {
        this.apiServerHost = apiServerHost;
    }

    public Integer getApiServerRestPort() {
        return apiServerRestPort;
    }

    public void setApiServerRestPort(Integer apiServerRestPort) {
        this.apiServerRestPort = apiServerRestPort;
    }


    public String getApiServerUsername() {
        return apiServerUsername;
    }


    public void setApiServerUsername(String apiServerUsername) {
        this.apiServerUsername = apiServerUsername;
    }


    public String getApiServerPass() {
        return apiServerPass;
    }


    public void setApiServerPass(String apiServerPass) {
        this.apiServerPass = apiServerPass;
    }

    public String getAppStoreDomain() {
        return appStoreDomain;
    }

    public void setAppStoreDomain(String appStoreDomain) {
        this.appStoreDomain = appStoreDomain;
    }

    public HttpScheme getApiServerScheme() {
        return apiServerScheme;
    }

    public void setApiServerScheme(HttpScheme apiServerScheme) {
        this.apiServerScheme = apiServerScheme;
    }
}
