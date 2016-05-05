/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.InstanceIdDao;
import org.mobicents.servlet.restcomm.entities.InstanceId;
import org.mobicents.servlet.restcomm.entities.Sid;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipURI;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class GenerateInstanceId {

    private final Logger logger = Logger.getLogger(GenerateInstanceId.class);

//    private final Configuration configuration;
    private final ServletContext servletContext;
    private final InstanceIdDao instanceIdDao;
    private final String host;

    public GenerateInstanceId(ServletContext servletContext, final SipURI sipURI) throws UnknownHostException {
        this.servletContext = servletContext;
        host = sipURI.getHost()+":"+sipURI.getPort();
        logger.info("Host for InstanceId: "+host);
        instanceIdDao = ((DaoManager) servletContext.getAttribute(DaoManager.class.getName())).getInstanceIdDao();
    }

    public InstanceId instanceId() {
        InstanceId instanceId = instanceIdDao.getInstanceIdByHost(host);
        if (instanceId != null) {
            logger.info("Restcomm Instance ID: "+instanceId.toString());
        } else {
            instanceId = new InstanceId(Sid.generate(Sid.Type.INSTANCE), host, DateTime.now(), DateTime.now());
            instanceIdDao.addInstancecId(instanceId);
            logger.info("Restcomm Instance ID created: "+instanceId.toString());
        }
        servletContext.setAttribute(InstanceId.class.getName(), instanceId);
        return instanceId;
     }
}
