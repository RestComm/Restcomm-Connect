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
package org.mobicents.servlet.sip.restcomm.callmanager.freeswitch;

import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.Environment;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManagerException;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreterContext;

public final class FreeswitchCallManager extends SipServlet implements CallManager {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(FreeswitchCallManager.class);
  // Thread pool for executing interpreters.
  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
  
  private static Environment environment;
  private static FreeswitchServer mediaServer;
  private static SipFactory sipFactory;
  
  public FreeswitchCallManager() {
    super();
  }
  
  @Override public Call createCall(String from, String to) throws CallManagerException {
  	return null;
  }
  
  @Override protected void doAck(final SipServletRequest request) throws ServletException, IOException {
    
  }
  
  @Override protected void doBye(final SipServletRequest request) throws ServletException, IOException {
    
  }

  @Override protected void doCancel(final SipServletRequest request) throws ServletException, IOException {
    
  }

  @Override protected void doInvite(final SipServletRequest request) throws ServletException, IOException {
	try {
	  // Forward the call to FreeSwitch.
	  final B2buaHelper b2bua = request.getB2buaHelper();
	  final SipServletRequest freeswitchRequest = b2bua.createRequest(request);
	  freeswitchRequest.send();
	  
	  // Hand the call to the interpreter for processing.
	  final TwiMLInterpreterContext context = new TwiMLInterpreterContext(null);
	  final TwiMLInterpreter interpreter = new TwiMLInterpreter(context);
	  interpreter.initialize();
	  EXECUTOR.submit(interpreter);
	} catch(final Exception exception) {
	  throw new ServletException(exception);
	}
  }

  @Override protected void doSuccessResponse(final SipServletResponse response) throws ServletException, IOException {
	final SipServletRequest request = response.getRequest();
	final SipSession session = response.getSession();
	if(request.getMethod().equals("INVITE") && response.getStatus() == SipServletResponse.SC_OK) {
	  final SipServletRequest ack = response.createAck();
	  ack.send();
	  
	}
  }

  @Override public void destroy() {
	// Clean up.
	environment.shutdown();
	EXECUTOR.shutdownNow();
  }
  
  private String getUUID(final SipServletRequest request) {
    return request.getHeader("X-UUID");
  }

  @Override public void init(final ServletConfig config) throws ServletException {
	final ServletContext context = config.getServletContext();
    final String path = context.getRealPath("/conf/restcomm.xml");
    if(LOGGER.isInfoEnabled()) {
      LOGGER.info("loading configuration file located at " + path);
    }
    // Load configuration
    XMLConfiguration configuration = null;
    try {
	  configuration = new XMLConfiguration(path);
	} catch(final ConfigurationException exception) {
      LOGGER.error("The RestComm environment could not be bootstrapped.", exception);
	}
    // Initialize the media server.
    mediaServer = new FreeswitchServer();
    // Initialize the SIP factory.
	sipFactory = (SipFactory)config.getServletContext().getAttribute(SIP_FACTORY);
	// Bootstrap the environment.
 	environment = Environment.getInstance();
	environment.configure(configuration);
	environment.initialize();
	environment.setCallManager(this);
  }
}
