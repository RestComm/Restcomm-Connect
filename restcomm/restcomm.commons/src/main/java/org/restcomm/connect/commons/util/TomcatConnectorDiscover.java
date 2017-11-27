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

import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.ReflectionException;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.HttpConnector;
import org.restcomm.connect.commons.HttpConnectorList;

public class TomcatConnectorDiscover implements HttpConnectorDiscover {

    private static final Logger LOG = Logger.getLogger(TomcatConnectorDiscover.class);

    @Override
    public HttpConnectorList findConnectors() throws MalformedObjectNameException, NullPointerException, UnknownHostException, AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException {
        LOG.info("Searching Tomcat HTTP connectors.");
        HttpConnectorList httpConnectorList = null;
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        Set<ObjectName> tomcatObjs = mbs.queryNames(new ObjectName("*:type=Connector,*"), Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));

        ArrayList<HttpConnector> endPoints = new ArrayList<HttpConnector>();
        if (tomcatObjs != null && tomcatObjs.size() > 0) {
            for (ObjectName obj : tomcatObjs) {
                String scheme = mbs.getAttribute(obj, "scheme").toString().replaceAll("\"", "");
                String port = obj.getKeyProperty("port").replaceAll("\"", "");
                String address = obj.getKeyProperty("address").replaceAll("\"", "");
                if (LOG.isInfoEnabled()) {
                    LOG.info("Tomcat Http Connector: " + scheme + "://" + address + ":" + port);
                }
                HttpConnector httpConnector = new HttpConnector(scheme, address, Integer.parseInt(port), scheme.equalsIgnoreCase("https"));
                endPoints.add(httpConnector);
            }
        }
        if (endPoints.isEmpty()) {
            LOG.warn("Coundn't discover any Http Interfaces");
        }
        httpConnectorList = new HttpConnectorList(endPoints);
        return httpConnectorList;
    }
}
