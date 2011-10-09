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
package org.mobicents.servlet.sip.restcomm.callmanager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreterContext;

public final class SipCallManager extends SipServlet implements CallManager {
  private static final long serialVersionUID = 1L;
  // Thread pool for executing interpreters.
  private static final ExecutorService executor = Executors.newCachedThreadPool();
  private static MsControlFactory msControlFactory;
  private static SipFactory sipFactory;
  
  public SipCallManager() {
    super();
  }
  
  public Call createCall(final String from, final String to) throws CallManagerException {
	try {
	  final SipGatewayManager sipGatewayManager = SipGatewayManager.getInstance();
	  final SipGateway sipGateway = sipGatewayManager.getGateway();
	  final String fromAddress = new StringBuilder().append("sip:").append(from).append("@")
	      .append(sipGateway.getProxy()).toString();
	  final String toAddress = new StringBuilder().append("sip:").append(to).append("@")
	      .append(sipGateway.getProxy()).toString();
	  // Create new SIP request.
	  final SipApplicationSession application = sipFactory.createApplicationSession();
	  final SipServletRequest request = sipFactory.createRequest(application, "INVITE", fromAddress, toAddress);
	  final MediaSession session = msControlFactory.createMediaSession();
	  final SipCall call = new SipCall(request, session, this);
	  // Bind the call to the SIP session.
	  request.getSession().setAttribute("CALL", call);
	  return call;
	} catch(final Exception exception) {
	  throw new CallManagerException(exception);
	}
  }
  
  @Override protected final void doAck(final SipServletRequest request) throws ServletException, IOException {
    final SipSession session = request.getSession();
    final SipCall call = (SipCall)session.getAttribute("CALL");
    try {
      call.connected();
    } catch(final CallException exception) {
      throw new ServletException(exception);
    }
  }
  
  @Override protected final void doBye(final SipServletRequest request) throws ServletException, IOException {
    final SipSession session = request.getSession();
    final SipCall call = (SipCall)session.getAttribute("CALL");
    try {
      call.bye(request);
    } catch(final CallException exception) {
      throw new ServletException(exception);
    }
  }

  @Override protected final void doCancel(final SipServletRequest request) throws ServletException, IOException {
    final SipSession session = request.getSession();
    final SipCall call = (SipCall)session.getAttribute("CALL");
    try {
      call.cancel(request);
    } catch(final CallException exception) {
      throw new ServletException(exception);
    }
  }

  @Override protected final void doInvite(final SipServletRequest request) throws ServletException, IOException {
	try {
      // Create a call.
	  final MediaSession session = msControlFactory.createMediaSession();
	  final SipCall call = new SipCall(request, session, this);
	  // Bind the call to the SIP session.
	  request.getSession().setAttribute("CALL", call);
	  try {
	    call.alert(request);
	  } catch(final CallException exception) {
	    throw new ServletException(exception);
	  }
	  // Hand the call to the interpreter for processing.
	  final TwiMLInterpreterContext context = new TwiMLInterpreterContext(call);
	  final TwiMLInterpreter interpreter = new TwiMLInterpreter(context);
	  interpreter.initialize();
	  executor.submit(interpreter);
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
	  final SipCall call = (SipCall)session.getAttribute("CALL");
	  try {
	    call.answered(response);
	  } catch(final CallException exception) {
	    throw new ServletException(exception);
	  }
	}
  }

  @Override public final void destroy() {
	// Clean up.
	executor.shutdownNow();
  }

  @Override public final void init(final ServletConfig config) throws ServletException {
    final Jsr309MediaServerManager mediaServerManager = Jsr309MediaServerManager.getInstance();
    final Jsr309MediaServer mediaServer = mediaServerManager.getMediaServer();
    msControlFactory = mediaServer.getMsControlFactory();
	sipFactory = (SipFactory)config.getServletContext().getAttribute(SIP_FACTORY);
  }
}
