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
 */package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.apache.commons.configuration.Configuration;

import org.mobicents.servlet.sip.restcomm.Application;
import org.mobicents.servlet.sip.restcomm.BootstrapException;
import org.mobicents.servlet.sip.restcomm.Bootstrapper;
import org.mobicents.servlet.sip.restcomm.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.Janitor;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManagerException;
import org.mobicents.servlet.sip.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MgcpCallManager extends SipServlet implements CallManager {
  private static final long serialVersionUID = 4758133818077979879L;
  
  private static SipFactory sipFactory;
  
  private static String proxyUser;
  private static String proxyPassword;
  private static SipURI proxyUri;
  
  private static MgcpServerManager servers;
  
  private static InterpreterExecutor executor;
  
  private static ApplicationsDao applicationsDao;
  private static IncomingPhoneNumbersDao incomingPhoneNumbersDao;
  
  public MgcpCallManager() {
    super();
  }
  
  @Override public Call createCall(final String from, final String to) throws CallManagerException {
    final SipURI fromUri = sipFactory.createSipURI(from, proxyUri.getHost());
    final SipURI toUri = sipFactory.createSipURI(to, proxyUri.getHost());
    return createCall(fromUri, toUri);
  }
  
  @Override public Call createCall(URI from, URI to) throws CallManagerException {
    final SipServletRequest invite = invite(from, to);
    final MgcpServer server = servers.getMediaServer();
    final MgcpCall call = new MgcpCall(invite, server);
	invite.getSession().setAttribute("CALL", call);
	return call;
  }
  
  private SipServletRequest invite(final URI from, final URI to) {
    final SipApplicationSession application = sipFactory.createApplicationSession();
    final SipServletRequest invite = sipFactory.createRequest(application, "INVITE", from, to);
    if(proxyUri != null) {
      invite.pushRoute(proxyUri);
    }
    return invite;
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

  @Override protected void doErrorResponse(final SipServletResponse response) throws ServletException, IOException {
    final SipServletRequest request = response.getRequest();
    final MgcpCall call = (MgcpCall)request.getSession().getAttribute("CALL");
    final String method = request.getMethod();
    if("INVITE".equalsIgnoreCase(method)) {
      final int status = response.getStatus();
      if(SipServletResponse.SC_UNAUTHORIZED == status || SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED == status) {
        final SipServletRequest invite = invite(request.getFrom().getURI(), request.getTo().getURI());
        final AuthInfo authorization = sipFactory.createAuthInfo();
        final String realm = response.getChallengeRealms().next(); 
        authorization.addAuthInfo(status, realm, proxyUser, proxyPassword);
        invite.addAuthHeader(response, authorization);
        if(request.getContentLength() > 0) {
          invite.setContent(request.getContent(), request.getContentType());
        }
        invite.getSession().setAttribute("CALL", call);
        invite.send();
      } else if(SipServletResponse.SC_BUSY_HERE == status || SipServletResponse.SC_BUSY_EVERYWHERE == status) {
        call.busy();
      } else {
        call.failed();
      }
    }
  }

  @Override protected final void doInvite(final SipServletRequest request) throws ServletException, IOException {
	try {
	  final IncomingPhoneNumber incomingPhoneNumber = getIncomingPhoneNumber(request);
	  if(incomingPhoneNumber != null) {
	    final Application application = getVoiceApplication(incomingPhoneNumber);
		// Initialize the call.
	    final MgcpServer server = servers.getMediaServer();
        final MgcpCall call = new MgcpCall(server);
	    request.getSession().setAttribute("CALL", call);
	    call.alert(request);
	    // Hand the call to the interpreter for processing.
	    executor.submit(application, incomingPhoneNumber, call);
	  } else {
	    final SipServletResponse notFound = request.createResponse(SipServletResponse.SC_NOT_FOUND);
	    notFound.send();
	  }
	} catch(final InterpreterException exception) {
	  throw new ServletException(exception);
	}
  }

  @Override protected void doSuccessResponse(final SipServletResponse response) throws ServletException, IOException {
	final SipServletRequest request = response.getRequest();
	final SipSession session = response.getSession();
	if(request.getMethod().equals("INVITE") && response.getStatus() == SipServletResponse.SC_OK) {
	  final MgcpCall call = (MgcpCall)session.getAttribute("CALL");
	  call.established(response.getRequest(), response);
	}
  }

  @Override public final void destroy() {
	Janitor.cleanup();
  }
  
  private IncomingPhoneNumber getIncomingPhoneNumber(final SipServletRequest invite) {
    final SipURI uri = (SipURI)invite.getTo().getURI();
    final String phoneNumber = uri.getUser();
    return incomingPhoneNumbersDao.getIncomingPhoneNumber(phoneNumber);
  }
  
  private Application getVoiceApplication(final IncomingPhoneNumber incomingPhoneNumber) {
	final Sid applicationSid = incomingPhoneNumber.getVoiceApplicationSid();
	if(applicationSid != null) {
	  return applicationsDao.getApplication(applicationSid);
	} else {
	  return null;
	}
  }

  @Override public final void init(final ServletConfig config) throws ServletException {
	final ServletContext context = config.getServletContext();
	context.setAttribute("org.mobicents.servlet.sip.restcomm.callmanager.CallManager", this);
    try {
	  Bootstrapper.bootstrap(config);
    } catch(final BootstrapException exception) {
      throw new ServletException(exception);
    }
    sipFactory = (SipFactory)config.getServletContext().getAttribute(SIP_FACTORY);
    final ServiceLocator services = ServiceLocator.getInstance();
    executor = services.get(InterpreterExecutor.class);
    servers = services.get(MgcpServerManager.class);
    final DaoManager daos = services.get(DaoManager.class);
    applicationsDao = daos.getApplicationsDao();
    incomingPhoneNumbersDao = daos.getIncomingPhoneNumbersDao();
    final Configuration configuration = services.get(Configuration.class);
    proxyUser = configuration.getString("outbound-proxy-user");
    proxyPassword = configuration.getString("outbound-proxy-password");
    final String uri = configuration.getString("outbound-proxy-uri");
    if(uri != null && !uri.isEmpty()) {
      proxyUri = sipFactory.createSipURI(null, uri);
    }
  }
}
