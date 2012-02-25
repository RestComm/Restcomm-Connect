
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
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.BootstrapException;
import org.mobicents.servlet.sip.restcomm.Bootstrapper;
import org.mobicents.servlet.sip.restcomm.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.Janitor;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManagerException;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MgcpCallManager extends SipServlet implements CallManager {
  private static final long serialVersionUID = 4758133818077979879L;
  private static final Logger LOGGER = Logger.getLogger(MgcpCallManager.class);
  
  private static MgcpServerManager servers;
  private static SipFactory sipFactory;
  
  public MgcpCallManager() {
    super();
  }
  
  @Override public Call createCall(final String from, final String to) throws CallManagerException {
	return null;
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
	  final IncomingPhoneNumber incomingPhoneNumber = getIncomingPhoneNumber(request);
	  if(incomingPhoneNumber != null) {
		// Initialize the call.
	    final MgcpServer server = servers.getMediaServer();
        final MgcpCall call = new MgcpCall(server);
	    request.getSession().setAttribute("CALL", call);
	    call.alert(request);
	    // Hand the call to the interpreter for processing.
	    final InterpreterExecutor executor = ServiceLocator.getInstance().get(InterpreterExecutor.class);
	    final RcmlInterpreterContext context = new RcmlInterpreterContext(call);
	    executor.submit(context);
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
	  
	}
  }

  @Override public final void destroy() {
	Janitor.cleanup();
  }
  
  private IncomingPhoneNumber getIncomingPhoneNumber(final SipServletRequest invite) {
    final ServiceLocator services = ServiceLocator.getInstance();
    final DaoManager daos = services.get(DaoManager.class);
    final IncomingPhoneNumbersDao dao = daos.getIncomingPhoneNumbersDao();
    final SipURI uri = (SipURI)invite.getTo().getURI();
    final String phoneNumber = uri.getUser();
    return dao.getIncomingPhoneNumber(phoneNumber);
  }

  @Override public final void init(final ServletConfig config) throws ServletException {
	final ServletContext context = config.getServletContext();
	context.setAttribute("org.mobicents.servlet.sip.restcomm.callmanager.CallManager", this);
    try {
	  Bootstrapper.bootstrap(config);
    } catch(final BootstrapException exception) {
      throw new ServletException(exception);
    }
    servers = ServiceLocator.getInstance().get(MgcpServerManager.class);
    sipFactory = (SipFactory)config.getServletContext().getAttribute(SIP_FACTORY);
  }
}
