package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;

public final class MgcpCall extends FiniteStateMachine implements Call, MgcpConnectionObserver, MgcpIvrEndpointObserver, MgcpLinkObserver {
  private static final Logger LOGGER = Logger.getLogger(MgcpCall.class);
  // Call Directions.
  private static final String INBOUND = "inbound";
  private static final String OUTBOUND_DIAL = "outbound-dial";
  // Call states.
  private static final State IDLE = new State("idle");
  private static final State QUEUED = new State("queued");
  private static final State RINGING = new State("ringing");
  private static final State IN_PROGRESS = new State("in-progress");
  private static final State COMPLETED = new State("completed");
  private static final State BUSY = new State("busy");
  private static final State FAILED = new State("failed");
  private static final State NO_ANSWER = new State("no-answer");
  private static final State CANCELLED = new State("cancelled");
  static {
    IDLE.addTransition(RINGING);
    IDLE.addTransition(QUEUED);
    QUEUED.addTransition(IN_PROGRESS);
    QUEUED.addTransition(FAILED);
    RINGING.addTransition(IN_PROGRESS);
    RINGING.addTransition(FAILED);
    RINGING.addTransition(CANCELLED);
    IN_PROGRESS.addTransition(COMPLETED);
    IN_PROGRESS.addTransition(FAILED);
  }
  
  private SipServletRequest initialInvite;
  
  private MgcpServer server;
  private MgcpSession session;
  private MgcpConnection connection;
  private MgcpLink link;
  
  private String direction;
  
  public MgcpCall(final MgcpServer server) {
    super(IDLE);
    // Initialize the state machine.
    addState(IDLE);
    addState(QUEUED);
    addState(RINGING);
    addState(IN_PROGRESS);
    addState(COMPLETED);
    addState(BUSY);
    addState(FAILED);
    addState(NO_ANSWER);
    addState(CANCELLED);
    this.server = server;
    this.session = server.createMediaSession();
  }
  
  public synchronized void alert(final SipServletRequest request) throws IOException {
    assertState(IDLE);
	direction = INBOUND;
	final SipServletResponse ringing = request.createResponse(SipServletResponse.SC_RINGING);
	try {
	  ringing.send();
	  initialInvite = request;
	  setState(RINGING);
	} catch(final IOException exception) {
	  setState(FAILED);
	  cleanup();
	  throw exception;
	}
  }
  
  @Override public synchronized void answer() throws CallException {
    assertState(RINGING);
    try {
      // Try to negotiate a media connection with a packet relay end point.
      final byte[] offer = initialInvite.getRawContent();
      connection = new MgcpConnection(server, session, null, null);
      connection.connect(ConnectionMode.Confrnce);
      wait();
      // Send the response back to the caller.
      final byte[] answer = connection.getLocalDescriptor().toString().getBytes();
      final SipServletResponse ok = initialInvite.createResponse(SipServletResponse.SC_OK);
      ok.setContent(answer, "application/sdp");
      ok.send();
      // Wait for an acknowledgment that the call is established.
      wait();
      // Make sure the call was answered.
      if(!getState().equals(IN_PROGRESS)) {
    	final StringBuilder buffer = new StringBuilder();
    	buffer.append("The call to recipient ").append(getRecipient())
    	    .append(" from sender ").append(getOriginator())
    	    .append(" could not be completed.");
    	setState(FAILED);
        throw new CallException(buffer.toString());
      }
    } catch(final Exception exception) {
      fail(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
      throw new CallException(exception);
    }
  }
  
  public void bye(final SipServletRequest request) throws IOException {
    assertState(IN_PROGRESS);
    final SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
    try {
      ok.send();
      setState(COMPLETED);
    } finally {
      cleanup();
    }
  }
  
  public void cancel(final SipServletRequest request) throws IOException {
    assertState(RINGING);
    final SipServletResponse ok = request.createResponse(SipServletResponse.SC_OK);
    try {
      ok.send();
      setState(CANCELLED);
    } finally {
      cleanup();
    }
  }
  
  private void cleanup() {
	
	initialInvite.getSession().invalidate();	  
  }

  @Override public void dial() throws CallException {
    
  }
  
  public void established() {
    assertState(RINGING);
	setState(IN_PROGRESS);
    notify();
  }
  
  private void fail(int code) {
    setState(FAILED);
    final SipServletResponse fail = initialInvite.createResponse(code);
    try {
      fail.send();
    } catch(final IOException exception) {
      LOGGER.error(exception);
    }
    cleanup();
  }

  @Override public String getDirection() {
    return direction;
  }

  @Override public String getId() {
    return initialInvite.getApplicationSession().getId();
  }
  
  public SipServletRequest getInitialInvite() {
    return initialInvite;
  }

  @Override public String getOriginator() {
    final SipURI from = (SipURI)initialInvite.getFrom().getURI();
    return from.getUser();
  }

  @Override public String getRecipient() {
    final SipURI to = (SipURI)initialInvite.getTo().getURI();
    return to.getUser();
  }

  @Override public String getStatus() {
    return getState().getName();
  }

  @Override public void hangup() {
    assertState(IN_PROGRESS);
    terminate();
	setState(COMPLETED);
  }

  @Override public void join(final Conference conference) throws CallException {
    
  }

  @Override public void leave(final Conference conference) throws CallException {
    
  }

  @Override public void play(final List<URI> announcements, final int iterations) throws CallException {
    assertState(IN_PROGRESS);
    
  }

  @Override public void reject() {
    assertState(RINGING);
    final SipServletResponse busy = initialInvite.createResponse(SipServletResponse.SC_BUSY_HERE);
    try {
      busy.send();
    } catch(final IOException exception) {
      LOGGER.error(exception);
    }
    cleanup();
  }
  
  private void terminate() {
	final SipSession sipSession = initialInvite.getSession();
    final SipServletRequest bye = sipSession.createRequest("BYE");
    try {
      bye.send();
    } catch(final IOException exception) {
      LOGGER.error(exception);
    }
    cleanup();
  }

  @Override public void connected(final MgcpLink link) {
    
  }

  @Override public void disconnected(final MgcpLink link) {
    
  }

  @Override public void failed(final MgcpLink link) {
    
  }

  @Override public void operationCompleted(final MgcpIvrEndpoint endpoint) {
    
  }

  @Override public void operationFailed(final MgcpIvrEndpoint endpoint) {
    
  }

  @Override public void halfOpen(final MgcpConnection connection) {
    
  }

  @Override public void open(final MgcpConnection connection) {
    
  }

  @Override public void disconnected(final MgcpConnection connection) {
    
  }

  @Override public void failed(final MgcpConnection connection) {
    
  }
}
