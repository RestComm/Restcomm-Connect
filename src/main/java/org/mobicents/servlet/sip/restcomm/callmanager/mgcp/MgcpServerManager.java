package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.Configurable;
import org.mobicents.servlet.sip.restcomm.FiniteStateMachine;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.State;
import org.mobicents.servlet.sip.restcomm.util.WrapAroundCounter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MgcpServerManager extends FiniteStateMachine implements Configurable, LifeCycle {
  // Initialize the logger.
  private static final Logger LOGGER = Logger.getLogger(MgcpServerManager.class);
  // Define our root configuration node.
  private static final String CONFIGURATION_PREFIX = "media-server-manager.mgcp-server";
  // Initialize the possible states and transitions.
  private static final State RUNNING = new State("RUNNING");
  private static final State SHUTDOWN = new State("SHUTDOWN");
  static {
    RUNNING.addTransition(SHUTDOWN);
    SHUTDOWN.addTransition(RUNNING);
  }
  
  // Our configuration.
  private Configuration configuration;
  // The servers we are managing.
  private List<MgcpServer> servers;
  // Index pointing to the next available server.
  private WrapAroundCounter index;
  
  public MgcpServerManager() {
	// Initialize the finite state machine.
    super(SHUTDOWN);
    addState(RUNNING);
    addState(SHUTDOWN);
  }

  @Override public void configure(final Configuration configuration) {
    assertState(SHUTDOWN);
    this.configuration = configuration;
  }
  
  public MgcpServer getMediaServer() {
	assertState(RUNNING);
    return servers.get((int)index.getAndIncrement());
  }

  @Override public synchronized void start() throws RuntimeException {
	assertState(SHUTDOWN);
    @SuppressWarnings("unchecked")
    final List<String> names = (List<String>)configuration.getList(CONFIGURATION_PREFIX + "[@name]");
    final int numberOfServers = names.size();
    if(LOGGER.isInfoEnabled()) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("Initializing ").append(numberOfServers).append(" servers.");
      LOGGER.info(buffer.toString());
    }
    if(numberOfServers > 0) {
      // Initialize the list of servers.
      servers = new ArrayList<MgcpServer>();
      for(int index = 0; index < numberOfServers; index++) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(CONFIGURATION_PREFIX).append("(").append(index).append(")").toString();
    	final String prefix = buffer.toString();
    	try {
          final String name = configuration.getString(prefix + "[@name]");
          final InetAddress localAddress = InetAddress.getByName(configuration.getString(prefix + ".local-address"));
          final int localPort = Integer.parseInt(configuration.getString(prefix + ".local-port"));
          final InetAddress remoteAddress = InetAddress.getByName(configuration.getString(prefix + ".remote-address"));
          final int remotePort = Integer.parseInt(configuration.getString(prefix + ".remote-port"));
          final MgcpServer server = new MgcpServer(name, localAddress, localPort, remoteAddress, remotePort);
          server.start();
          servers.add(server);
    	} catch(final UnknownHostException exception) {
    	  LOGGER.error(exception);
    	  throw new RuntimeException(exception);
    	}
      }
      index = new WrapAroundCounter(servers.size());
      setState(RUNNING);
    }
  }

  @Override public synchronized void shutdown() {
    assertState(RUNNING);
    for(final MgcpServer server : servers) {
      server.shutdown();
    }
    servers.clear();
    servers = null;
    index = null;
    configuration = null;
    setState(SHUTDOWN);
  }
}
