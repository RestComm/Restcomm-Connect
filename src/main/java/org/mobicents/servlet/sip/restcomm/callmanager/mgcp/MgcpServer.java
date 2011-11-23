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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.callmanager.MediaSession;

public final class MgcpServer implements JainMgcpListener, LifeCycle {
  private static final Logger LOGGER = Logger.getLogger(MgcpServer.class);
  
  private final InetAddress localIp;
  private final int localPort;
  private final InetAddress remoteIp;
  private final int remotePort;
  
  private NotifiedEntity callAgent;
  private String domainName;
  
  private JainMgcpProvider mgcpProvider;
  private JainMgcpStack mgcpStack;
  
  private int mediaSessionCounter;
  private ReentrantLock mediaSessionCounterLock;
  
  private int requestIdCounter;
  private ReentrantLock requestIdCounterLock;
  
  private int transactionIdCounter;
  private ReentrantLock transactionIdCounterLock;
  
  private Thread requestProcessingThread;
  private volatile boolean requestProcessingIsRunning;
  private List<JainMgcpListener> requestListeners;
  private BlockingQueue<JainMgcpCommandEvent> requestQueue;
  private Thread responseProcessingThread;
  private volatile boolean responseProcessingIsRunning;
  private Map<Integer, JainMgcpListener> responseListeners;
  private BlockingQueue<JainMgcpResponseEvent> responseQueue;

  public MgcpServer(final InetAddress localIp, final int localPort, final InetAddress remoteIp,
      final int remotePort) {
    super();
    this.localIp = localIp;
    this.localPort = localPort;
    this.remoteIp = remoteIp;
    this.remotePort = remotePort;
  }
  
  public void addCommandEventListener(final JainMgcpListener listener) {
	requestListeners.add(listener);
  }
  
  public MediaSession createMediaSession() {
    return null;
  }
  
  public void destroyMediaSession(final MediaSession mediaSession) {
    
  }
  
  public RequestIdentifier generateRequestIdentifier() {
	int id;
	requestIdCounterLock.lock();
	try {
	  requestIdCounter++;
	  id = requestIdCounter;
	  // Make sure our counter doesn't overflow.
	  if(requestIdCounter == Integer.MAX_VALUE) {
	    requestIdCounter = 0;
	  }
	} finally {
	  requestIdCounterLock.unlock();
	}
	return new RequestIdentifier(Integer.toString(id));
  }
  
  public int generateTransactionId() {
    int id;
    transactionIdCounterLock.lock();
    try {
      transactionIdCounter++;
      id = transactionIdCounter;
      // Make sure our counter doesn't overflow.
      if(transactionIdCounter == Integer.MAX_VALUE) {
        transactionIdCounter = 0;
      }
    } finally {
      transactionIdCounterLock.unlock();
    }
    return id;
  }
  
  public NotifiedEntity getCallAgent() {
    return callAgent;
  }
  
  public String getDomainName() {
    return domainName;
  }
  
  @Override public void initialize() throws RuntimeException {
	// Initialize constants.
    callAgent = new NotifiedEntity("restcomm", localIp.getHostAddress(), localPort);
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
	// Finish initialization.
	mediaSessionCounter = 0;
	mediaSessionCounterLock = new ReentrantLock();
	requestIdCounter = 0;
	requestIdCounterLock = new ReentrantLock();
	transactionIdCounter = 0;
	transactionIdCounterLock = new ReentrantLock();
    requestListeners = Collections.synchronizedList(new LinkedList<JainMgcpListener>());
    requestQueue = new LinkedBlockingQueue<JainMgcpCommandEvent>();
    responseListeners = new ConcurrentHashMap<Integer, JainMgcpListener>();
    responseQueue = new LinkedBlockingQueue<JainMgcpResponseEvent>();
    // Start the message processing threads.
    initializeRequestProcessingThread();
    initializeResponseProcessingThread();
  }
  
  private void initializeRequestProcessingThread() {
    requestProcessingThread = new Thread(new Runnable() {
      public void run() {
        while(requestProcessingIsRunning) {
          // Take an event from the head for processing or block.
          JainMgcpCommandEvent event = null;
          try {
            event = requestQueue.take();
          } catch(final InterruptedException ignored) { }
          // If the event is not null dispatch it to the listeners.
          if(event != null) {
    	    for(final JainMgcpListener listener : requestListeners) {
    	      listener.processMgcpCommandEvent(event);
    	    }
          }
        }
      }
    });
    requestProcessingThread.setName("MGCP Server Request Processing Thread");
    requestProcessingThread.start();
  }
  
  private void initializeResponseProcessingThread() {
    responseProcessingThread = new Thread(new Runnable() {
      public void run() {
        while(responseProcessingIsRunning) {
          // Take a response from the head for processing or block.
          JainMgcpResponseEvent event = null;
          try {
            event = responseQueue.take();
          } catch(final InterruptedException ignored) { }
          // If the event is not null dispatch it the listener.
          if(event != null) {
        	// Find the listener for this response.
    	    final JainMgcpListener listener = responseListeners.remove(event.getTransactionHandle());
    	    // Dispatch the response to the listener.
    	    listener.processMgcpResponseEvent(event);
          }
        }
      }
    });
    responseProcessingThread.setName("MGCP Server Response Processing Thread");
    responseProcessingThread.start();
  }

  @Override public void processMgcpCommandEvent(final JainMgcpCommandEvent event) {
    try { requestQueue.put(event); } catch(final InterruptedException ignored) { }
  }

  @Override public void processMgcpResponseEvent(final JainMgcpResponseEvent event) {
	try { responseQueue.put(event); } catch(final InterruptedException ignored) { }
  }
  
  public void removeCommandEventListener(final JainMgcpListener listener) {
    requestListeners.remove(listener);
  }
  
  public void sendEvent(final JainMgcpCommandEvent event, final JainMgcpListener listener) {
	// Register the listener to listen for a response to the event being sent.
	responseListeners.put(event.getTransactionHandle(), listener);
	// Try to send the event.
	try {
      mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { event });
	} catch(final IllegalArgumentException exception) {
	  // Make sure we don't start a memory leak.
	  responseListeners.remove(event.getTransactionHandle());
	  // Log the exception.
	  LOGGER.error(exception);
	}
  }
  
  public void sendEvent(final JainMgcpResponseEvent event) {
    try {
      mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { event });
    } catch(final IllegalArgumentException exception) {
      LOGGER.error(exception);
    }
  }

  @Override public void shutdown() {
	// Shutdown the message processing threads.
	  
	// Shutdown the MGCP stack.
    try { 
      mgcpProvider.removeJainMgcpListener(this);
      mgcpStack.deleteProvider(mgcpProvider);
    } catch(final DeleteProviderException exception) {
      // There is nothing we can do except log the exception.
      LOGGER.error(exception);
    }
    // Wrap up.
    callAgent = null;
    domainName = null;
    mgcpProvider = null;
    mgcpStack = null;
    mediaSessionCounter = -1;
    mediaSessionCounterLock = null;
    requestIdCounter = -1;
    requestIdCounterLock = null;
    transactionIdCounter = -1;
    requestIdCounterLock = null;
    requestListeners = null;
    responseListeners = null;
  }
}
