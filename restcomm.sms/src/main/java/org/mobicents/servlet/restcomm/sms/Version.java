package org.mobicents.servlet.restcomm.sms;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Version {
  private static final class Singleton {
    private static final Version instance = new Version();
  }
  private final String version;
  
  private Version() {
    final Properties properties = new Properties();
    final InputStream input = this.getClass().getResourceAsStream("/org/mobicents/servlet/restcomm/sms/version.properties");
    try {
      if(input != null) {
        properties.load(input);
      } else {
        throw new NullPointerException("Could not load the version.properties file.");
      }
    } catch(final IOException exception) {
      exception.printStackTrace();
    }
    version = properties.getProperty("restcomm.version");
  }

  public static Version getInstance(){
    return Singleton.instance;
  }

  public String getRestCommVersion() {
    return version;
  }
}