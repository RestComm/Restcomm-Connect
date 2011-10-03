package org.mobicents.servlet.sip.restcomm.callmanager;

import java.util.Iterator;
import java.util.Properties;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import org.mobicents.javax.media.mscontrol.spi.DriverImpl;
import org.mobicents.servlet.sip.restcomm.Configurable;

public final class Jsr309MediaServerManager implements Configurable {
  private static final Logger logger = Logger.getLogger(Jsr309MediaServerManager.class);
  private static final class SingletonHolder {
    private static final Jsr309MediaServerManager INSTANCE = new Jsr309MediaServerManager();
  }
  private static final String CONFIGURATION_PREFIX = "media-server-manager.mgcp-stack";
  private Jsr309MediaServer server;
  
  private Jsr309MediaServerManager() {
    super();
    this.server = null;
  }
  
  private MsControlFactory createMsControlFactory(final String stackName, final String stackAddress, final String stackPort,
      final String remoteAddress, final String remotePort) {
    final Properties configuration = new Properties();
    configuration.put("mgcp.stack.name", stackName);
    configuration.put("mgcp.bind.address", stackAddress);
    configuration.put("mgcp.local.port", stackPort);
    configuration.put("mgcp.server.address", remoteAddress);
    configuration.put("mgcp.server.port", remotePort);
    MsControlFactory factory = null;
    try {
	  factory = new DriverImpl().getFactory(configuration);
	} catch(final MsControlException exception) {
	  logger.error(exception);
	}
    return factory;
  }
  
  @Override public void configure(final Configuration configuration) {
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
    final MsControlFactory factory = createMsControlFactory(stackName, stackAddress, stackPort, remoteAddress, remotePort);
    server = new Jsr309MediaServer(factory);
  }
  
  public static Jsr309MediaServerManager getInstance() {
    return SingletonHolder.INSTANCE;
  }
  
  public Jsr309MediaServer getMediaServer() {
    return server;
  }
  
  public void shutdown() {
	// Clean up after MGCP stack.
    Iterator<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasNext()) {
      Driver driver = drivers.next();
	  DriverManager.deregisterDriver(driver);
	  DriverImpl impl = (DriverImpl) driver;
	  impl.shutdown();
	} 
  }
}
