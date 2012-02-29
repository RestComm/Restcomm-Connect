package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.callmanager.CallObserver;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;

public final class MgcpCall extends FiniteStateMachine implements Call, MgcpConnectionObserver, MgcpIvrEndpointObserver, MgcpLinkObserver {
  private static final Logger LOGGER = Logger.getLogger(MgcpCall.class);
  // Call states.
  private static final State IDLE = new State(Status.IDLE.toString());
  private static final State QUEUED = new State(Status.QUEUED.toString());
  private static final State RINGING = new State(Status.RINGING.toString());
  private static final State IN_PROGRESS = new State(Status.IN_PROGRESS.toString());
  private static final State COMPLETED = new State(Status.COMPLETED.toString());
  private static final State BUSY = new State(Status.BUSY.toString());
  private static final State FAILED = new State(Status.FAILED.toString());
  private static final State NO_ANSWER = new State(Status.NO_ANSWER.toString());
  private static final State CANCELLED = new State(Status.CANCELLED.toString());
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
  private MgcpEndpoint localEndpoint;
  private MgcpEndpoint remoteEndpoint;
  
  private Sid sid;
  private Direction direction;
  
  private List<CallObserver> observers;
  
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
    this.sid = Sid.generate(Sid.Type.CALL);
    this.observers = new ArrayList<CallObserver>();
  }
  
  @Override public void addObserver(final CallObserver observer) {
    
  }
  
  public synchronized void alert(final SipServletRequest request) throws IOException {
    assertState(IDLE);
	direction = Direction.INBOUND;
	final SipServletResponse ringing = request.createResponse(SipServletResponse.SC_RINGING);
	try {
	  ringing.send();
	  initialInvite = request;
	  setState(RINGING);
	} catch(final IOException exception) {
	  setState(FAILED);
	  cleanup();
	  LOGGER.error(exception);
	  throw exception;
	}
  }
  
  @Override public synchronized void answer() throws CallException {
    assertState(RINGING);
    try {
      // Try to negotiate a media connection with a packet relay end point.
      localEndpoint = session.getIvrEndpoint();
      ((MgcpIvrEndpoint)localEndpoint).addObserver(this);
      final byte[] offer = initialInvite.getRawContent();
      final ConnectionDescriptor remoteDescriptor = new ConnectionDescriptor(new String(offer));
      connection = session.createConnection(localEndpoint, remoteDescriptor);
      connection.addObserver(this);
      connection.connect(ConnectionMode.SendRecv);
      wait();
      // Send the response back to the caller.
      final byte[] answer = connection.getLocalDescriptor().toString().getBytes();
      final SipServletResponse ok = initialInvite.createResponse(SipServletResponse.SC_OK);
      ok.setContent(answer, "application/sdp");
      ok.send();
      // Wait for an acknowledgment that the call is established.
      wait();
    } catch(final Exception exception) {
      fail(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
      LOGGER.error(exception);
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
	server.destroyMediaSession(session);
	initialInvite.getSession().invalidate();	  
  }

  @Override public void dial() throws CallException {
    
  }
  
  public synchronized void established() {
    assertState(RINGING);
    setState(IN_PROGRESS);
    initialInvite.setExpires(240);
    notify();
    /*
    final MgcpIvrEndpoint endpoint = session.getIvrEndpoint();
    endpoint.addObserver(this);
    remoteEndpoint = endpoint;
    link = session.createLink(localEndpoint, remoteEndpoint);
    link.addObserver(this);
	link.connect(ConnectionMode.Inactive);
	*/
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

  @Override public Direction getDirection() {
    return direction;
  }
  
  @Override public String getForwardedFrom() {
    return null;
  }

  @Override public Sid getSid() {
    return sid;
  }
  
  public SipServletRequest getInitialInvite() {
    return initialInvite;
  }

  @Override public String getOriginator() {
    final SipURI from = (SipURI)initialInvite.getFrom().getURI();
    return from.getUser();
  }
  
  @Override public String getOriginatorName() {
    return initialInvite.getFrom().getDisplayName();
  }

  @Override public String getRecipient() {
    final SipURI to = (SipURI)initialInvite.getTo().getURI();
    return to.getUser();
  }

  @Override public Status getStatus() {
    return Status.getValueOf(getState().getName());
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

  @Override public synchronized void play(final List<URI> announcements, final int iterations) throws CallException {
    assertState(IN_PROGRESS);
    final MgcpIvrEndpoint ivr = (MgcpIvrEndpoint)localEndpoint;
    ivr.play(announcements, iterations);
    try {
      wait();
    } catch(final InterruptedException ignored) { }
  }
  
  @Override public synchronized String playAndCollect(final List<URI> announcements, final String endInputKey, final int maxNumberOfDigits,
      int timeout) {
    assertState(IN_PROGRESS);
    final MgcpIvrEndpoint ivr = (MgcpIvrEndpoint)localEndpoint;
    ivr.playCollect(announcements, maxNumberOfDigits, maxNumberOfDigits, timeout, timeout, endInputKey);
    try {
      wait();
    } catch(final InterruptedException ignored) { }
    return ivr.getDigits();
  }
  
  @Override public URI playAndRecord(final List<URI> prompts, final long preSpeechTimer, final long recordingLength,
      String endInputKey) throws CallException {
  	return null;
  }

  @Override public synchronized void reject() {
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

  @Override public synchronized void connected(final MgcpLink link) {
    
  }

  @Override public void disconnected(final MgcpLink link) {
    
  }

  @Override public synchronized void failed(final MgcpLink link) {
    notify();
  }

  @Override public synchronized void operationCompleted(final MgcpIvrEndpoint endpoint) {
    notify();
  }

  @Override public synchronized void operationFailed(final MgcpIvrEndpoint endpoint) {
    notify();
  }

  @Override public void halfOpen(final MgcpConnection connection) {
    
  }

  @Override public synchronized void open(final MgcpConnection connection) {
    notify();
  }

  @Override public void disconnected(final MgcpConnection connection) {
    
  }

  @Override public synchronized void failed(final MgcpConnection connection) {
	// The connection to the packet relay end point failed.
	setState(FAILED);
	final StringBuilder buffer = new StringBuilder();
  	buffer.append("The call to recipient ").append(getRecipient())
  	    .append(" from sender ").append(getOriginator())
  	    .append(" could not be completed.");
  	LOGGER.error(buffer.toString());
  	notify();
  }

  @Override public synchronized void modified(ConnectionDescriptor descriptor, MgcpLink link) {
    
  }

  @Override public synchronized void modified(final MgcpConnection connection) {
    
  }
}
