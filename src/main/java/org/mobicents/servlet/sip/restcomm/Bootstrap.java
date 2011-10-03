/*
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
package org.mobicents.servlet.sip.restcomm;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.callmanager.Jsr309MediaServerManager;
import org.mobicents.servlet.sip.restcomm.callmanager.SipGatewayManager;

public class Bootstrap implements ServletContextListener {
  private static final Logger logger = Logger.getLogger(Bootstrap.class);

  @Override public void contextDestroyed(final ServletContextEvent event) {
	// Initialize the JSR-309 stack.
	final Jsr309MediaServerManager mediaServerManager = Jsr309MediaServerManager.getInstance();
	mediaServerManager.shutdown();
  }

  @Override public void contextInitialized(final ServletContextEvent event) {
    final ServletContext context = event.getServletContext();
    final String path = context.getRealPath("/conf/restcomm.xml");
    if(logger.isInfoEnabled()) {
      logger.info("loading configuration file located at " + path);
    }
    // Load configuration
    XMLConfiguration configuration = null;
    try {
	  configuration = new XMLConfiguration(path);
	} catch(final ConfigurationException exception) {
      logger.error("The RestComm environment could not be bootstrapped.", exception);
	}
	// Initialize the JSR-309 stack.
	final Jsr309MediaServerManager mediaServerManager = Jsr309MediaServerManager.getInstance();
	mediaServerManager.configure(configuration);
	// Initialize the SIP gateway.
	final SipGatewayManager gatewayManager = SipGatewayManager.getInstance();
	gatewayManager.configure(configuration);
	// Initialize the environment.
    final Environment environment = Environment.getInstance();
	environment.configure(configuration);
  }
}
