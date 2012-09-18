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

import org.mobicents.servlet.sip.restcomm.BootstrapException;
import org.mobicents.servlet.sip.restcomm.Bootstrapper;
import org.mobicents.servlet.sip.restcomm.Janitor;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.entities.Application;
import org.mobicents.servlet.sip.restcomm.entities.Client;
import org.mobicents.servlet.sip.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallException;
import org.mobicents.servlet.sip.restcomm.media.api.CallManager;
import org.mobicents.servlet.sip.restcomm.media.api.CallManagerException;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

 /**
  * @author quintana.thomas@gmail.com (Thomas Quintana)
  */
 public final class MgcpCallManager extends SipServlet implements CallManager {
	 private static final long serialVersionUID = 4758133818077979879L;

	 private static SipFactory sipFactory;

	 private static Configuration configuration;
	 private static String proxyUser;
	 private static String proxyPassword;
	 private static SipURI proxyUri;

	 private static MgcpServerManager servers;

	 private static InterpreterExecutor executor;

	 private static DaoManager daos;

	 public MgcpCallManager() {
		 super();
	 }

	 @Override public Call createExternalCall(final String from, final String to) throws CallManagerException {
		 final SipURI fromUri = sipFactory.createSipURI(from, proxyUri.getHost());
		 final SipURI toUri = sipFactory.createSipURI(to, proxyUri.getHost());
		 return createCall(fromUri, toUri);
	 }
	 
	@Override public Call createUserAgentCall(final String from, final String to) throws CallManagerException {
	  try {
	    final SipURI fromUri = sipFactory.createSipURI(from, proxyUri.getHost());
        final URI toUri = sipFactory.createURI(to);
        return createCall(fromUri, toUri);
	   } catch(final Exception exception) {
   	     throw new CallManagerException(exception);
   	   }
	}

	 @Override public Call createCall(final String from, final String to) throws CallManagerException {
	   try {
		 final URI fromUri = sipFactory.createURI(from);
		 final URI toUri = sipFactory.createURI(to);
		 return createCall(fromUri, toUri);
	   } catch(final Exception exception) {
		 throw new CallManagerException(exception);
	   }
	 }

	 private Call createCall(URI from, URI to) throws CallManagerException {
	   SipServletRequest invite = null;
	   try { invite = invite(from, to); }
	   catch(final ServletException exception) {
	     throw new CallManagerException(exception);
	   }
	   final MgcpServer server = servers.getMediaServer();
	   final MgcpCall call = new MgcpCall(invite, server);
	   invite.getSession().setAttribute("CALL", call);
	   return call;
	 }

	 private SipServletRequest invite(final URI from, final URI to) throws ServletException {
		 final SipApplicationSession application = sipFactory.createApplicationSession();
		 final SipServletRequest invite = sipFactory.createRequest(application, "INVITE", from, to);
		 final StringBuilder buffer = new StringBuilder();
		 buffer.append(((SipURI)to).getHost());
		 final int port = ((SipURI)to).getPort();
		 if(port > -1) {
		   buffer.append(":");
		   buffer.append(port);
		 }
		 final SipURI destination = sipFactory.createSipURI(null, buffer.toString());
		 invite.pushRoute(destination);
		 final SipSession session = invite.getSession();
		 session.setHandler("SipCallManager");
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
	   final int status = response.getStatus();
	   if("INVITE".equalsIgnoreCase(method)) {
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
	  	   call.updateInitialInvite(invite);
	     } else if(SipServletResponse.SC_BUSY_HERE == status || SipServletResponse.SC_BUSY_EVERYWHERE == status) {
		   call.busy();
	     } else {
	       call.failed();
 	     }
	   } else if("CANCEL".equalsIgnoreCase(method)) {
	     if(SipServletResponse.SC_REQUEST_TERMINATED == status) {
		   final SipServletRequest ack = response.createAck();
		   ack.send();
	     }
	   }
	 }

	 @Override protected final void doInvite(final SipServletRequest request) throws ServletException, IOException {
		 try {
			 // Create the call.
			 final MgcpServer server = servers.getMediaServer();
			 final MgcpCall call = new MgcpCall(server);
			 request.getSession().setAttribute("CALL", call);
//			 call.alert(request);
			 call.trying(request);
			 // Schedule the RCML script to execute for this call.
			 Application application = null;
			 final SipURI from = (SipURI)request.getFrom().getURI();
			 final Client client = getClient(from.getUser());
			 if(client != null) {
		       final Sid applicationSid = client.getVoiceApplicationSid();
		       if(applicationSid != null) {
		         application = getVoiceApplication(applicationSid);
		         executor.submit(application.getAccountSid(), application.getApiVersion(), application.getVoiceUrl(),
		             application.getVoiceMethod(), application.getVoiceFallbackUrl(), application.getVoiceFallbackMethod(),
		             application.getStatusCallback(), application.getStatusCallbackMethod(), call);
		       } else {
		         executor.submit(client.getAccountSid(), client.getApiVersion(), client.getVoiceUrl(), client.getVoiceMethod(),
		             client.getVoiceFallbackUrl(), client.getVoiceFallbackMethod(), null, null, call);
		       }
			 } else {
			   final SipURI uri = (SipURI)request.getTo().getURI();
			   final IncomingPhoneNumber incomingPhoneNumber = getIncomingPhoneNumber(uri.getUser());
			   if(incomingPhoneNumber != null) {
			     final Sid applicationSid = incomingPhoneNumber.getVoiceApplicationSid();
			     if(applicationSid != null) {
			       application = getVoiceApplication(applicationSid);
			       executor.submit(application.getAccountSid(), application.getApiVersion(), application.getVoiceUrl(),
				       application.getVoiceMethod(), application.getVoiceFallbackUrl(), application.getVoiceFallbackMethod(),
				       application.getStatusCallback(), application.getStatusCallbackMethod(), call);
			     } else {
			       executor.submit(incomingPhoneNumber.getAccountSid(), incomingPhoneNumber.getApiVersion(),
			           incomingPhoneNumber.getVoiceUrl(), incomingPhoneNumber.getVoiceMethod(), incomingPhoneNumber.getVoiceFallbackUrl(),
			           incomingPhoneNumber.getVoiceFallbackMethod(), incomingPhoneNumber.getStatusCallback(),
			           incomingPhoneNumber.getStatusCallbackMethod(), call);
			     }
			   } else {
				 final SipServletResponse notFound = request.createResponse(SipServletResponse.SC_NOT_FOUND);
				 notFound.send();
			   }
			 }
		 } catch(final InterpreterException exception) {
			 throw new ServletException(exception);
		 } catch (InterruptedException e) {
			throw new ServletException(e);
		} catch (CallException e) {
			throw new ServletException(e);
		} 
	 }

	 @Override protected void doOptions(final SipServletRequest request)
	     throws ServletException, IOException {
		request.createResponse(SipServletResponse.SC_OK).send();
	}

	@Override protected void doSuccessResponse(final SipServletResponse response) throws ServletException, IOException {
		 final SipServletRequest request = response.getRequest();
		 final SipSession session = response.getSession();
		 if(request.getMethod().equals("INVITE") && response.getStatus() == SipServletResponse.SC_OK) {
			 final MgcpCall call = (MgcpCall)session.getAttribute("CALL");
			 call.established(response);
		 }
	 }

	 @Override public final void destroy() {
		 Janitor.cleanup();
	 }
	 
	 private Client getClient(final String name) {
       return daos.getClientsDao().getClient(name);
	 }

	 private IncomingPhoneNumber getIncomingPhoneNumber(final String phoneNumber) {
		 try {
			 final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
			 final String to = phoneNumberUtil.format(phoneNumberUtil.parse(phoneNumber, "US"),
					 PhoneNumberFormat.E164);
			 return daos.getIncomingPhoneNumbersDao().getIncomingPhoneNumber(to);
		 } catch(final NumberParseException ignored) {
			 return null;
		 }
	 }

	 private Application getVoiceApplication(final Sid applicationSid) {
	   return daos.getApplicationsDao().getApplication(applicationSid);
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
		 daos = services.get(DaoManager.class);
		 configuration = services.get(Configuration.class);
		 proxyUser = configuration.getString("outbound-proxy-user");
		 proxyPassword = configuration.getString("outbound-proxy-password");
		 final String uri = configuration.getString("outbound-proxy-uri");
		 if(uri != null && !uri.isEmpty()) {
			 proxyUri = sipFactory.createSipURI(null, uri);
		 }
	 }
 }
