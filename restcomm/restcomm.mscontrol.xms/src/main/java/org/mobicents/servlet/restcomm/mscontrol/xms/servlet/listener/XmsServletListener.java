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

package org.mobicents.servlet.restcomm.mscontrol.xms.servlet.listener;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerControllerFactory;
import org.mobicents.servlet.restcomm.mscontrol.xms.XmsControllerFactory;

import com.vendor.dialogic.javax.media.mscontrol.sip.DlgcSipServlet;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@Immutable
public final class XmsServletListener implements SipServletListener {

    private static final Logger logger = Logger.getLogger(XmsServletListener.class);

    private static final String DRIVER_NAME = "com.dialogic.dlg309";

    private Driver dlgcDriver;
    private MsControlFactory msControlFactory;

    public XmsServletListener() {
        super();
    }

    private void loadDriver() throws MsControlException {
        this.dlgcDriver = DriverManager.getDriver(DRIVER_NAME);
        if (dlgcDriver == null) {
            throw new MsControlException("No driver with name " + DRIVER_NAME + " was found!");
        }
        this.msControlFactory = this.dlgcDriver.getFactory(null);
    }

    @Override
    public void servletInitialized(SipServletContextEvent event) {
        if (DlgcSipServlet.class.equals(event.getSipServlet().getClass())) {
            // Dialogic SIP Servlet has initialized!
            try {
                // Load the XMS connector
                loadDriver();

                // Set the MSControlFactory in the XMS Controller Factory kept in context
                ServletContext context = event.getServletContext();
                XmsControllerFactory xmsFactory = (XmsControllerFactory) context
                        .getAttribute(MediaServerControllerFactory.class.getName());
                xmsFactory.setMsControlFactory(this.msControlFactory);
            } catch (MsControlException e) {
                logger.error("Could not load XMS JSR-309 driver: " + e.getMessage(), e);
            }
        }
    }

}
