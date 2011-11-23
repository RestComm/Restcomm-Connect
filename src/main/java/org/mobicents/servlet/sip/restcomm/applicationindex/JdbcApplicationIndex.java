package org.mobicents.servlet.sip.restcomm.applicationindex;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.sip.restcomm.Application;

public final class JdbcApplicationIndex implements ApplicationIndex {
  public JdbcApplicationIndex() {
    super();
  }

  @Override public void configure(final Configuration configuration) {
    
  }

  @Override public void initialize() throws RuntimeException {
    
  }

  @Override public void shutdown() {
    
  }

  @Override public Application locate(final String locator) throws ApplicationIndexException {
    return null;
  }
}
