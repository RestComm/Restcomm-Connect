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
import javax.management.ReflectionException;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.HttpConnector;
import org.restcomm.connect.commons.HttpConnectorList;

public class JBossConnectorDiscover implements HttpConnectorDiscover {

    private static final Logger LOG = Logger.getLogger(JBossConnectorDiscover.class);

    /**
     * A list of connectors. Not bound connectors will be discarded.
     *
     * @return
     * @throws MalformedObjectNameException
     * @throws NullPointerException
     * @throws UnknownHostException
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws MBeanException
     * @throws ReflectionException
     */
    @Override
    public HttpConnectorList findConnectors() throws MalformedObjectNameException, NullPointerException, UnknownHostException, AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException {
        LOG.info("Searching JBoss HTTP connectors.");
        HttpConnectorList httpConnectorList = null;
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        Set<ObjectName> jbossObjs = mbs.queryNames(new ObjectName("jboss.as:socket-binding-group=standard-sockets,socket-binding=http*"), null);
        LOG.info("JBoss Mbean found.");
        ArrayList<HttpConnector> endPoints = new ArrayList<HttpConnector>();
        if (jbossObjs != null && jbossObjs.size() > 0) {
            LOG.info("JBoss Connectors found:" + jbossObjs.size());
            for (ObjectName obj : jbossObjs) {
                Boolean bound = (Boolean) mbs.getAttribute(obj, "bound");
                if (bound) {
                    String scheme = mbs.getAttribute(obj, "name").toString().replaceAll("\"", "");
                    Integer port = (Integer) mbs.getAttribute(obj, "boundPort");
                    String address = ((String) mbs.getAttribute(obj, "boundAddress")).replaceAll("\"", "");
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Jboss Http Connector: " + scheme + "://" + address + ":" + port);
                    }
                    HttpConnector httpConnector = new HttpConnector(scheme, address, port, scheme.equalsIgnoreCase("https"));
                    endPoints.add(httpConnector);
                } else {
                    LOG.info("JBoss Connector not bound,discarding.");
                }
            }
        }
        if (endPoints.isEmpty()) {
            LOG.warn("Coundn't discover any Http Interfaces.");
        }
        httpConnectorList = new HttpConnectorList(endPoints);
        return httpConnectorList;
    }
}
