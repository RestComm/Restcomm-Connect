package org.mobicents.servlet.sip.restcomm.applicationindex;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.sip.restcomm.Application;

public final class MongoDbApplicationIndex implements ApplicationIndex {
  public MongoDbApplicationIndex() {
    super();
  }

  @Override public void configure(final Configuration configuration) {
    
  }

  @Override public void start() throws RuntimeException {
    
  }

  @Override public void shutdown() {
    
  }

  @Override public Application locate(final String locator) throws ApplicationIndexException {
    return null;
  }
}
