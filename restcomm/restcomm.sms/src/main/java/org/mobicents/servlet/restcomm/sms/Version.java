/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.sms;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author (Pavel Slegr)
 */
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