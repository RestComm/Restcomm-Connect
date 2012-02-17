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
package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.DeleteProviderException;
import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.JainMgcpStack;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.util.WrapAroundCounter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MgcpServer extends FiniteStateMachine implements JainMgcpListener, LifeCycle {
  // Initialize the logger.
  private static final Logger LOGGER = Logger.getLogger(MgcpServer.class);
  //Initialize the possible states and transitions.
  private static final State RUNNING = new State("RUNNING");
  private static final State SHUTDOWN = new State("SHUTDOWN");
  static {
    RUNNING.addTransition(SHUTDOWN);
    SHUTDOWN.addTransition(RUNNING);
  }
  // Server connection information.
  private final String name;
  private final InetAddress localIp;
  private final int localPort;
  private final InetAddress remoteIp;
  private final int remotePort;
  // JAIN MGCP stuff.
  private JainMgcpProvider mgcpProvider;
  private JainMgcpStack mgcpStack;
  private List<JainMgcpListener> requestListeners;
  private Object requestListenersLock;
  private Map<Integer, JainMgcpListener> responseListeners;
  private WrapAroundCounter requestId;
  private WrapAroundCounter transactionId;
  // Call agent.
  private NotifiedEntity callAgent;
  // Media gateway domain name.
  private String domainName;
  // Media Session stuff.
  private WrapAroundCounter mediaSessionId;
  private Map<Integer, MgcpSession> mediaSessions;

  public MgcpServer(final String name, final InetAddress localIp, final int localPort,
      final InetAddress remoteIp, final int remotePort) {
    // Initialize the finite state machine.
    super(SHUTDOWN);
    addState(RUNNING);
    addState(SHUTDOWN);
    // Remember the server connection information.
    this.name = name;
    this.localIp = localIp;
    this.localPort = localPort;
    this.remoteIp = remoteIp;
    this.remotePort = remotePort;
  }
  
  public void addNotifyListener(final JainMgcpListener listener) {
	assertState(RUNNING);
    synchronized(requestListenersLock) {
      requestListeners.add(listener);
    }
  }
  
  public void removeNotifyListener(final JainMgcpListener listener) {
	assertState(RUNNING);
    synchronized(requestListenersLock) {
      requestListeners.remove(listener);
    }
  }
  
  public RequestIdentifier generateRequestIdentifier() {
	assertState(RUNNING);
    return new RequestIdentifier(Integer.toString((int)requestId.getAndIncrement()));
  }

  @Override public synchronized void start() throws RuntimeException {
	assertState(SHUTDOWN);
	// Initialize the call agent.
    callAgent = new NotifiedEntity("restcomm", localIp.getHostAddress(), localPort);
    // Initialize the media gateway domain name.
    domainName = new StringBuilder().append(remoteIp.getHostAddress()).append(":")
        .append(remotePort).toString();
	// Start the MGCP stack.
	try {
      mgcpStack = new JainMgcpStackImpl(localIp, localPort);
      mgcpProvider = mgcpStack.createProvider();
      mgcpProvider.addJainMgcpListener(this);
	} catch(final Exception exception) {
	  throw new RuntimeException(exception);
	}
	requestListeners = new ArrayList<JainMgcpListener>();
	requestListenersLock = new Object();
	responseListeners = new HashMap<Integer, JainMgcpListener>();
	requestId = new WrapAroundCounter(Integer.MAX_VALUE);
	transactionId = new WrapAroundCounter(Integer.MAX_VALUE);
	mediaSessionId = new WrapAroundCounter(Integer.MAX_VALUE);
	mediaSessions = new HashMap<Integer, MgcpSession>();
	setState(RUNNING);
  }

  public MgcpSession createMediaSession() throws MgcpServerException {
    assertState(RUNNING);
    int id = -1;
    do {
      id = (int)mediaSessionId.getAndIncrement();
    } while(mediaSessions.containsKey(id));
    final MgcpSession session = new MgcpSession(id, this);
    mediaSessions.put(id, session);
    return session;
  }

  public void destroyMediaSession(final MgcpSession session) {
    assertState(RUNNING);
    session.release();
    mediaSessions.remove(session.getId());
  }
  
  public NotifiedEntity getCallAgent() {
	assertState(RUNNING);
    return callAgent;
  }
  
  public String getDomainName() {
	assertState(RUNNING);
    return domainName;
  }
  
  public String getName() {
	assertState(RUNNING);
    return name;
  }

  @Override public void processMgcpCommandEvent(final JainMgcpCommandEvent event) {
    if(getState() == RUNNING) {
      synchronized(requestListenersLock) {
	    for(final JainMgcpListener listener : requestListeners) {
          listener.processMgcpCommandEvent(event);
        }
      }
    }
  }

  @Override public void processMgcpResponseEvent(final JainMgcpResponseEvent event) {
    if(getState() == RUNNING) {
	  // Find the listener for this response.
      final JainMgcpListener listener = responseListeners.remove(event.getTransactionHandle());
      // Dispatch the response to the listener.
      listener.processMgcpResponseEvent(event);
    }
  }
  
  public void sendCommand(final JainMgcpCommandEvent command, final JainMgcpListener listener) throws MgcpServerException {
	assertState(RUNNING);
	// Register the listener to listen for a response to the command being sent.
	final int id = (int)transactionId.getAndIncrement();
	command.setTransactionHandle(id);
	responseListeners.put(id, listener);
	// Try to send the command.
	try {
      mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { command });
	} catch(final IllegalArgumentException exception) {
	  // Make sure we don't start a memory leak.
	  responseListeners.remove(id);
	  // Log and re-throw the exception.
	  LOGGER.error(exception);
	  throw new MgcpServerException(exception);
	}
  }
  
  public void sendResponse(final JainMgcpResponseEvent response) throws MgcpServerException {
	assertState(RUNNING);
    try {
      mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { response });
    } catch(final IllegalArgumentException exception) {
      LOGGER.error(exception);
      throw new MgcpServerException(exception);
    }
  }

  @Override public synchronized void shutdown() {
    assertState(RUNNING);
    // Stop all the media sessions.
    final List<MgcpSession> sessions = new ArrayList<MgcpSession>(mediaSessions.values());
    for(final MgcpSession session : sessions) {
      session.release();
    }
    sessions.clear();
    // Shutdown the MGCP stack.
    try { 
      mgcpProvider.removeJainMgcpListener(this);
      mgcpStack.deleteProvider(mgcpProvider);
    } catch(final DeleteProviderException exception) {
      // There is nothing we can do except log the exception.
      LOGGER.error(exception);
    }
    callAgent = null;
    domainName = null;
    mgcpProvider = null;
    mgcpStack = null;
    requestListeners = null;
	requestListenersLock = null;
	responseListeners = null;
	requestId = null;
	transactionId = null;
	mediaSessionId = null;
	mediaSessions = null;
	setState(SHUTDOWN);
  }
}
