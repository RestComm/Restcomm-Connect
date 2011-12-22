package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
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
import org.mobicents.servlet.sip.restcomm.callmanager.sip.SipGateway;
import org.mobicents.servlet.sip.restcomm.callmanager.sip.SipGatewayManager;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterContext;

public final class MgcpCallManager extends SipServlet implements CallManager {
  private static final long serialVersionUID = 4758133818077979879L;
  private static final Logger LOGGER = Logger.getLogger(MgcpCallManager.class);
  
  private static Environment environment;
  private static MgcpServerManager servers;
  
  private static SipGatewayManager sipGatewayManager;
  private static SipFactory sipFactory;
  
  public MgcpCallManager() {
    super();
  }
  
  @Override public Call createCall(final String from, final String to) throws CallManagerException {
	try {
	  final SipGateway sipGateway = sipGatewayManager.getGateway();
	  final String fromAddress = new StringBuilder().append("sip:").append(from).append("@")
	      .append(sipGateway.getProxy()).toString();
	  final String toAddress = new StringBuilder().append("sip:").append(to).append("@")
	      .append(sipGateway.getProxy()).toString();
	  // Create new SIP request.
	  final SipApplicationSession application = sipFactory.createApplicationSession();
	  final SipServletRequest request = sipFactory.createRequest(application, "INVITE", fromAddress, toAddress);
	  // Request a media server to handle the new out bound call.
	  final MgcpServer server = servers.getMediaServer();
	  // Create new call.
	  final MgcpCall call = new MgcpCall(server);
	  request.getSession().setAttribute("CALL", call);
	  return call;
	} catch(final Exception exception) {
	  throw new CallManagerException(exception);
	}
  }
  
  @Override protected final void doAck(final SipServletRequest request) throws ServletException, IOException {
    final SipSession session = request.getSession();
    final MgcpCall call = (MgcpCall)session.getAttribute("CALL");
    call.established();
  }
  
  @Override protected final void doBye(final SipServletRequest request) throws ServletException, IOException {
    final SipSession session = request.getSession();
    final MgcpCall call = (MgcpCall)session.getAttribute("CALL");
    call.bye(request);
  }

  @Override protected final void doCancel(final SipServletRequest request) throws ServletException, IOException {
    final SipSession session = request.getSession();
    final MgcpCall call = (MgcpCall)session.getAttribute("CALL");
    call.cancel(request);
  }

  @Override protected final void doInvite(final SipServletRequest request) throws ServletException, IOException {
	try {
	  // Request a media server to handle the new incoming call.
	  final MgcpServer server = servers.getMediaServer();
      // Create a call.
	  final MgcpCall call = new MgcpCall(server);
	  // Bind the call to the SIP session.
	  request.getSession().setAttribute("CALL", call);
	  // Alert!
	  call.alert(request);
	  // Hand the call to an interpreter for processing.
	  final InterpreterExecutor executor = environment.getInterpreterExecutor();
	  final InterpreterContext context = new InterpreterContext(call);
	  executor.submit(context);
	} catch(final InterpreterException exception) {
	  throw new ServletException(exception);
	}
  }

  @Override protected void doSuccessResponse(final SipServletResponse response) throws ServletException, IOException {
	final SipServletRequest request = response.getRequest();
	final SipSession session = response.getSession();
	if(request.getMethod().equals("INVITE") && response.getStatus() == SipServletResponse.SC_OK) {
	  final MgcpCall call = (MgcpCall)session.getAttribute("CALL");
	  
	}
  }

  @Override public final void destroy() {
	// Clean up.
	environment.shutdown();
	servers.shutdown();
  }

  @Override public final void init(final ServletConfig config) throws ServletException {
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
    servers = new MgcpServerManager();
    servers.configure(configuration);
    servers.start();
    // Initialize the conference center.
  	// final Jsr309ConferenceCenter conferenceCenter = new Jsr309ConferenceCenter(mediaServerManager);
    // Initialize the SIP gateway manager.
 	// sipGatewayManager = SipGatewayManager.getInstance();
 	// sipGatewayManager.configure(configuration);
 	// sipGatewayManager.initialize();
    // Initialize the SIP factory.
	sipFactory = (SipFactory)config.getServletContext().getAttribute(SIP_FACTORY);
	// Bootstrap the environment.
 	environment = Environment.getInstance();
	environment.configure(configuration);
	environment.start();
	environment.setCallManager(this);
	// environment.setConferenceCenter(conferenceCenter);
  }
}
