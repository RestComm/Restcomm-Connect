package org.mobicents.servlet.sip.restcomm.callmanager;

import java.util.Properties;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.DriverManager;

import org.mobicents.jsr309.mgcp.MgcpStackFactory;
import org.mobicents.servlet.sip.restcomm.LifeCycle;

public final class Jsr309MediaServer implements LifeCycle {
  private final Properties configuration;
  private MsControlFactory factory;
  
  public Jsr309MediaServer(final String stackName, final String stackAddress, final String stackPort,
      final String remoteAddress, final String remotePort) {
    super();
    this.configuration = new Properties();
    configuration.put("mgcp.stack.name", stackName);
    configuration.put("mgcp.stack.ip", stackAddress);
    configuration.put("mgcp.stack.port", stackPort);
    configuration.put("mgcp.stack.peer.ip", remoteAddress);
    configuration.put("mgcp.stack.peer.port", remotePort);
  }
  
  public MsControlFactory getMsControlFactory() {
    return factory;
  }

  @Override public void initialize() throws RuntimeException {
    try {
	  factory = DriverManager.getDrivers().next().getFactory(configuration);
	} catch(final MsControlException exception) {
	  throw new RuntimeException(exception);
	}
  }

  @Override public void shutdown() {
    // Clean up after MGCP stack.
    MgcpStackFactory.getInstance().clearMgcpStackProvider(configuration);
  }
}
