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
package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.applicationindex.ApplicationIndex;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;

public final class Environment implements Configurable {
  private static final class SingletonHolder {
    private static final Environment INSTANCE = new Environment();
  }
  private static final Logger logger = Logger.getLogger(Environment.class);
  private Configuration configuration;
  private volatile ApplicationIndex applicationIndex;
  private volatile SmsAggregator smsAggregator;
  
  private Environment() {
    super();
  }
  
  @Override public void configure(final Configuration configuration) {
  	this.configuration = configuration;
  }
  
  public static Environment getInstance() {
    return SingletonHolder.INSTANCE;
  }
  
  public ApplicationIndex getApplicationIndex() {
	if(applicationIndex != null) {
	  return applicationIndex;
	} else {
	  synchronized(this) {
	    if(applicationIndex == null) {
	      try {
	    	final String classpath = configuration.getString("application-index[@class]");
			applicationIndex = (ApplicationIndex)ObjectFactory.getInstance().getObjectInstance(classpath);
			applicationIndex.configure(configuration.subset("application-index"));
		  } catch(final ObjectInstantiationException exception) {
			logger.error(exception);
		  }
	    }
	  }
	  return applicationIndex;
	}
  }
  
  public String getRecordingsPath() {
    return configuration.getString("recorder-settings.recordings-location");
  }
  
  public URI getRecordingsUri() {
    return URI.create(configuration.getString("recorder-settings.recordings-uri"));
  }
  
  public SmsAggregator getSmsAggregator() {
    if(smsAggregator != null) {
      return smsAggregator;
    } else {
      synchronized(this) {
        if(smsAggregator == null) {
          try {
            final String classpath = configuration.getString("sms-aggregator[@class]");
            smsAggregator = (SmsAggregator)ObjectFactory.getInstance().getObjectInstance(classpath);
            smsAggregator.configure(configuration.subset("sms-aggregator"));
          } catch(final ObjectInstantiationException exception) {
            logger.error(exception);
          }
        }
      }
      return smsAggregator;
    }
  }
}
