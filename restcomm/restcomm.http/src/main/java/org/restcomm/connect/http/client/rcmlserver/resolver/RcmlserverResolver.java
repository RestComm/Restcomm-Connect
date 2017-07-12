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

package org.restcomm.connect.http.client.rcmlserver.resolver;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RcmlserverResolver {
    protected static Logger logger = Logger.getLogger(RcmlserverResolver.class);
    static RcmlserverResolver singleton;

    String rvdOrigin;
    String filterPrefix; //  a pattern used to match against relative urls in application.rcmlUrl. If null, to resolving will occur.

    static final String DEFAULT_FILTER_PREFIX = "/restcomm-rvd/";

    // not really a clean singleton pattern but a way to init once and use many. Only the first time this method is called the parameters are used in the initialization
    public static RcmlserverResolver getInstance(String rvdOrigin, String apiPath, boolean reinit) {
        if (singleton == null || reinit) {
            singleton = new RcmlserverResolver(rvdOrigin, apiPath);
        }
        return singleton;
    }

    public static RcmlserverResolver getInstance(String rvdOrigin, String apiPath) {
        return getInstance(rvdOrigin, apiPath, false);
    }

    public RcmlserverResolver(String rvdOrigin, String apiPath) {
        this.rvdOrigin = rvdOrigin;
        if ( ! StringUtils.isEmpty(apiPath) ) {
            // extract the first part of the path and use it as a filter prefix
            Pattern pattern = Pattern.compile("/([^/]*)((/.*)|$)");
            Matcher matcher = pattern.matcher(apiPath);
            if (matcher.matches() && matcher.group(1) != null) {
                filterPrefix = "/" + matcher.group(1) + "/";
            } else
                filterPrefix = DEFAULT_FILTER_PREFIX;
            logger.info("RcmlserverResolver initialized. Urls starting with '" + filterPrefix + "' will get prepended with '" + rvdOrigin + "'");
        } else
            filterPrefix = null;
    }

    // if rvdOrigin is null no point in trying to resolve RVD location. We will return passed uri instead
    public URI resolveRelative(URI uri) {
        if (uri != null && rvdOrigin != null && filterPrefix != null) {
            if (uri.isAbsolute())
                return uri;
            try {
                String uriString = uri.toString();
                if (uriString.startsWith(filterPrefix))
                    return new URI(rvdOrigin + uri.toString());
            } catch (URISyntaxException e) {
                logger.error("Cannot resolve uri: " + uri.toString() + ". Ignoring...", e);
            }
        }
        return uri;
    }
}
