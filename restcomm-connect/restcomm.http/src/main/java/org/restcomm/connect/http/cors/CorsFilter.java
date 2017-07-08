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

package org.restcomm.connect.http.cors;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.configuration.sets.RcmlserverConfigurationSet;
import org.restcomm.connect.commons.configuration.sets.impl.RcmlserverConfigurationSetImpl;
import org.restcomm.connect.commons.configuration.sources.ApacheConfigurationSource;
import org.restcomm.connect.commons.configuration.sources.ConfigurationSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.File;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {
    private final Logger logger = Logger.getLogger(CorsFilter.class);

    @Context
    private HttpServletRequest servletRequest;

    // we initialize this lazily upon first request since it can't be injected through the @Context annotation (it didn't work)
    private ServletContext lazyServletContext;

    String allowedOrigin;

    // We return Access-* headers only in case allowedOrigin is present and equals to the 'Origin' header.
    @Override
    public ContainerResponse filter(ContainerRequest cres, ContainerResponse response) {
        initLazily(servletRequest);
        String requestOrigin = cres.getHeaderValue("Origin");
        if (requestOrigin != null) { // is this is a cors request (ajax request that targets a different domain than the one the page was loaded from)
            if (allowedOrigin != null && allowedOrigin.startsWith(requestOrigin)) {  // no cors allowances make are applied if allowedOrigins == null
                // only return the origin the client informed
                response.getHttpHeaders().add("Access-Control-Allow-Origin", requestOrigin);
                response.getHttpHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
                response.getHttpHeaders().add("Access-Control-Allow-Credentials", "true");
                response.getHttpHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
                response.getHttpHeaders().add("Access-Control-Max-Age", "1209600");
            }
        }
        return response;
    }

    private void initLazily(ServletRequest request)  {
        if (lazyServletContext == null) {
            ServletContext context = request.getServletContext();
            String rootPath = context.getRealPath("/");
            rootPath = StringUtils.stripEnd(rootPath,"/"); // remove trailing "/" character
            String restcommXmlPath = rootPath + "/WEB-INF/conf/restcomm.xml";

            // ok, found restcomm.xml. Now let's get rcmlserver/base-url configuration setting
            File restcommXmlFile = new File(restcommXmlPath);
            // Create apache configuration
            XMLConfiguration apacheConf = new XMLConfiguration();
            apacheConf.setDelimiterParsingDisabled(true);
            apacheConf.setAttributeSplittingDisabled(true);
            try {
                apacheConf.load(restcommXmlPath);
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
            // Create high-level configuration
            ConfigurationSource source = new ApacheConfigurationSource(apacheConf);
            RcmlserverConfigurationSet rcmlserverConfig = new RcmlserverConfigurationSetImpl(source);
            // initialize allowedOrigin
            String baseUrl = rcmlserverConfig.getBaseUrl();
            if ( baseUrl != null && (! baseUrl.trim().equals(""))) {
                // baseUrl is set. We need to return CORS allow headers
                allowedOrigin = baseUrl;
            }

            lazyServletContext = context;

            logger.info("Initialized (lazily) CORS servlet response filter. allowedOrigin: " + allowedOrigin);
        }
    }
}
