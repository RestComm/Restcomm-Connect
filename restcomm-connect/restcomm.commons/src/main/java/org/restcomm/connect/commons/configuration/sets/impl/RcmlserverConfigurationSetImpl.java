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

import org.apache.commons.lang.StringUtils;
import org.restcomm.connect.commons.configuration.sets.RcmlserverConfigurationSet;
import org.restcomm.connect.commons.configuration.sources.ConfigurationSource;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RcmlserverConfigurationSetImpl extends ConfigurationSet implements RcmlserverConfigurationSet {
    private static final String BASE_URL_KEY = "rcmlserver.base-url";
    private static final String API_PATH_KEY = "rcmlserver.api-path";
    private static final String NOTIFY_KEY = "rcmlserver.notifications";
    private static final String TIMEOUT_KEY = "rcmlserver.timeout";
    private static final String TIMEOUT_PER_NOTIFICATION_KEY = "rcmlserver.timeout-per-notification";
    private String baseUrl = null;
    private String apiPath = null;
    private Boolean notify = false;
    private Integer timeout = 5000;
    private Integer timeoutPerNotification = 500;

    public RcmlserverConfigurationSetImpl(ConfigurationSource source) {
        super(source);

        String value = source.getProperty(BASE_URL_KEY);
        if ( !StringUtils.isEmpty(value) ) {
            value = value.trim();
            if (value.endsWith("/")) // remove trailing '/' if present
                value = value.substring(0,value.length()-2);
            this.baseUrl = value;
        }

        value = source.getProperty(API_PATH_KEY);
        if ( !StringUtils.isEmpty(value) ) {
            value = value.trim();
            if (value.endsWith("/")) // remove trailing '/' if present
                value = value.substring(0,value.length()-2);
            this.apiPath = value;
        }

        value = source.getProperty(NOTIFY_KEY);
        try {
            this.notify = Boolean.parseBoolean(value);
        } catch (Exception e) {}

        value = source.getProperty(TIMEOUT_KEY);
        try {
            this.timeout = Integer.parseInt(value);
        } catch (Exception e) {}

        value = source.getProperty(TIMEOUT_PER_NOTIFICATION_KEY);
        try {
            this.timeoutPerNotification= Integer.parseInt(value);
        } catch (Exception e) {}
    }

    public RcmlserverConfigurationSetImpl(ConfigurationSource source, String baseUrl, Boolean notify, Integer timeout, Integer timeoutPerNotification) {
        super(source);
        this.baseUrl = baseUrl;
        this.notify = notify;
        this.timeout = timeout;
        this.timeoutPerNotification = timeoutPerNotification;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getApiPath() {
        return apiPath;
    }

    @Override
    public Boolean getNotify() {
        return notify;
    }

    @Override
    public Integer getTimeout() {
        return this.timeout;
    }

    @Override
    public Integer getTimeoutPerNotification() {
        return this.timeoutPerNotification;
    }

}
