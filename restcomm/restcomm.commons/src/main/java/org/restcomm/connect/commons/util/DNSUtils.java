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
package org.restcomm.connect.commons.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.util.mock.InetAddressMock;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class DNSUtils {

    private static boolean initialized = false;

    private static String dnsUtilImplClassName;
    private static final String DEFAULT_DNS_UTIL_CLASS_NAME = "java.net.InetAddress";

    public static void initializeDnsUtilImplClassName(Configuration conf) {
        synchronized (DNSUtils.class) {
            if (!initialized) {
                String configClass = conf.getString("dns-util[@class]");
                dnsUtilImplClassName = configClass == null ? DEFAULT_DNS_UTIL_CLASS_NAME : configClass;
                initialized = true;
            }
        }
    }

    public static InetAddress getByName(String host) throws UnknownHostException {
        InetAddress result = null;
        switch (dnsUtilImplClassName) {
        case "java.net.InetAddress":
            result = InetAddress.getByName(host);
            break;
        /* we can add implementation of another dns impl as well, for example dn4j etc*/
        case "org.restcomm.connect.commons.util.mock.InetAddressMock":
        //case "org.restcomm.connect.testsuite.mocks.InetAddressMock":
            result = InetAddressMock.getByName(host);
            break;
        default:
            result = InetAddress.getByName(host);
            break;
        }
        return result == null ? InetAddress.getByName(host): result;
    }
}