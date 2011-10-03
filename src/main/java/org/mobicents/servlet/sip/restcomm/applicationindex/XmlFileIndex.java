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
package org.mobicents.servlet.sip.restcomm.applicationindex;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.Application;

public final class XmlFileIndex implements ApplicationIndex {
  private static final Logger LOGGER = Logger.getLogger(XmlFileIndex.class);
  private static final String CONFIGURATION_PREFIX = "applications.application";
  private final Map<String, Application> applicationIndex;
  
  public XmlFileIndex() {
    super();
    applicationIndex = new HashMap<String, Application>();
  }
  
  public Application locate(final String locator) throws ApplicationIndexException {
	return applicationIndex.get(locator);
  }

  @Override public void configure(final Configuration configuration) {
    // Get the number of applications to be loaded.
    @SuppressWarnings("unchecked")
    final List<String> applications = (List<String>)configuration.getList(CONFIGURATION_PREFIX + "[@name]");
    final int numberOfApplications = applications.size();
    if(LOGGER.isInfoEnabled()) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("Loading ").append(numberOfApplications).append(" RestComm applications.");
      LOGGER.info(buffer.toString());
    }
    if(numberOfApplications > 0) {
      if(LOGGER.isInfoEnabled()) {
        LOGGER.info("Loading RestComm Applications.");
      }
      for(int index = 0; index < numberOfApplications; index++) {
    	final Application application = new Application();
    	application.setName(configuration.getString(CONFIGURATION_PREFIX + "[@name]"));
        application.setEndPoint(configuration.getString(CONFIGURATION_PREFIX + ".endpoint"));
        application.setRequestMethod(configuration.getString(CONFIGURATION_PREFIX + ".request-method"));
        application.setUri(URI.create(configuration.getString(CONFIGURATION_PREFIX + ".uri")));
        applicationIndex.put(application.getEndPoint(), application);
        if(LOGGER.isInfoEnabled()) {
          LOGGER.info(application);
        }
      }
    }
  }
}
