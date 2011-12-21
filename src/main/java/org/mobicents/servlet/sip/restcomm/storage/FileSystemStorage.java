package org.mobicents.servlet.sip.restcomm.storage;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

public class FileSystemStorage implements Storage {
  private static final Logger LOGGER = Logger.getLogger(FileSystemStorage.class);
  
  private String path;
  private String uri;
  
  public FileSystemStorage() {
    super();
  }

  @Override public void configure(final Configuration configuration) {
    path = configuration.getString("path");
    uri = configuration.getString("uri");
  }

  @Override public String getHttpUri() {
    return uri;
  }

  @Override public String getPath() {
    return path;
  }
  
  @Override public void start() throws RuntimeException {
    // Nothing to do.
  }

  @Override public byte[] readObject(final String path) {
    throw new UnsupportedOperationException();
  }

  @Override public void removeObject(final String path) {
    throw new UnsupportedOperationException();
  }
  
  @Override public void shutdown() {
    // Nothing to do.
  }

  @Override public void writeObject(final String path, final byte[] object) {
    throw new UnsupportedOperationException();
  }
}
