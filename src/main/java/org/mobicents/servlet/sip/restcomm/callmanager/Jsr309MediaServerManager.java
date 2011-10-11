package org.mobicents.servlet.sip.restcomm.callmanager;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.Configurable;
import org.mobicents.servlet.sip.restcomm.LifeCycle;

public final class Jsr309MediaServerManager implements Configurable, LifeCycle {
  private static final Logger logger = Logger.getLogger(Jsr309MediaServerManager.class);
  private static final class SingletonHolder {
    private static final Jsr309MediaServerManager INSTANCE = new Jsr309MediaServerManager();
  }
  private static final String CONFIGURATION_PREFIX = "media-server-manager.mgcp-stack";
  
  private Configuration configuration;
  private Jsr309MediaServer server;
  
  private Jsr309MediaServerManager() {
    super();
    this.server = null;
  }
  
  @Override public void configure(final Configuration configuration) {
	this.configuration = configuration;
  }
  
  public static Jsr309MediaServerManager getInstance() {
    return SingletonHolder.INSTANCE;
  }
  
  public Jsr309MediaServer getMediaServer() {
    return server;
  }
  
  @Override public void initialize() throws RuntimeException {
    final String stackName = configuration.getString(CONFIGURATION_PREFIX + "[@name]");
    final String stackAddress = configuration.getString(CONFIGURATION_PREFIX + ".stack-address");
    final String stackPort = configuration.getString(CONFIGURATION_PREFIX + ".stack-port");
    final String remoteAddress = configuration.getString(CONFIGURATION_PREFIX + ".remote-address");
    final String remotePort = configuration.getString(CONFIGURATION_PREFIX + ".remote-port");
    if(logger.isInfoEnabled()) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("Initializing JSR-309 Stack.\n");
      buffer.append("Stack Name: ").append(stackName).append("\n");
      buffer.append("Stack Address: ").append(stackAddress).append("\n");
      buffer.append("Stack Port: ").append(stackPort).append("\n");
      buffer.append("Remote Address: ").append(remoteAddress).append("\n");
      buffer.append("Remote Port: ").append(remotePort).append("\n");
      logger.info(buffer.toString());
    }
    server = new Jsr309MediaServer(stackName, stackAddress, stackPort, remoteAddress, remotePort);
    server.initialize();
  }
  
  public void shutdown() {
	server.shutdown();
  }
}
