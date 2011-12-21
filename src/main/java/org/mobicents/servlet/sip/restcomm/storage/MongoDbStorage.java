package org.mobicents.servlet.sip.restcomm.storage;

import org.apache.commons.configuration.Configuration;

public final class MongoDbStorage implements Storage {
  public MongoDbStorage() {
    super();
  }
  
  @Override public void configure(final Configuration configuration) {
    
  }

  @Override public void start() throws RuntimeException {
    
  }

  @Override public void shutdown() {
    
  }

  @Override public String getHttpUri() {
    return null;
  }

  @Override public String getPath() {
    return null;
  }

  @Override public byte[] readObject(final String path) {
    return null;
  }

  @Override public void removeObject(final String path) {
		// TODO Auto-generated method stub

	}

  @Override public void writeObject(final String path, final byte[] object) {
    
  }
}
