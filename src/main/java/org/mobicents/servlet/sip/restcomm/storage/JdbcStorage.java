package org.mobicents.servlet.sip.restcomm.storage;

import org.apache.commons.configuration.Configuration;

public final class JdbcStorage implements Storage {
  public JdbcStorage() {
    super();
  }
  
  @Override public void configure(final Configuration configuration) {
    
  }

  @Override public void initialize() throws RuntimeException {
    
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
    
  }

  @Override public void writeObject(final String path, final byte[] object) {
    
  }
}
