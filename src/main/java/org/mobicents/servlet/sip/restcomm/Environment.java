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

import org.apache.commons.configuration.Configuration;

import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class Environment implements Configurable, LifeCycle {
  private static final class SingletonHolder {
    private static final Environment INSTANCE = new Environment();
  }
  
  private Configuration configuration;
  private CallManager callManager;
  private ConferenceCenter conferenceCenter;
  private InterpreterExecutor interpreterExecutor;
  private SmsAggregator smsAggregator;
  
  private Environment() {
    super();
  }
  
  @Override public void configure(final Configuration configuration) {
  	this.configuration = configuration;
  }
  
  public CallManager getCallManager() {
    return callManager;
  }
  
  public ConferenceCenter getConferenceCenter() {
    return conferenceCenter;
  }
  
  public Configuration getConfiguration() {
    return configuration;
  }
  
  public static Environment getInstance() {
    return SingletonHolder.INSTANCE;
  }
  
  public InterpreterExecutor getInterpreterExecutor() {
    return interpreterExecutor;
  }
  
  public SmsAggregator getSmsAggregator() {
    return smsAggregator;
  }

  @Override public void start() throws RuntimeException {
	try {
      loadSmsAggregator();
      initializeInterpreterExecutor();
	} catch(final ObjectInstantiationException exception) {
	  throw new RuntimeException(exception);
	}
  }
  
  private void initializeInterpreterExecutor() {
    interpreterExecutor = new InterpreterExecutor();
  }
  
  private void loadSmsAggregator() throws ObjectInstantiationException {
    final String classpath = configuration.getString("sms-aggregator[@class]");
    smsAggregator = (SmsAggregator)ObjectFactory.getInstance().getObjectInstance(classpath);
    smsAggregator.configure(configuration.subset("sms-aggregator"));
    smsAggregator.start();
  }
  
  public void setCallManager(final CallManager callManager) {
    this.callManager = callManager;
  }
  
  public void setConferenceCenter(final ConferenceCenter conferenceCenter) {
    this.conferenceCenter = conferenceCenter;
  }

  @Override public void shutdown() {
	if(smsAggregator != null) {
      smsAggregator.shutdown();
    }
  }
}
