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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.callmanager.mgcp.MgcpServerManager;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizer;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class Bootstrapper {
  public static final Logger logger = Logger.getLogger(Bootstrapper.class);
  
  private Bootstrapper() {
    super();
  }
  
  public static void bootstrap(final ServletConfig config) throws BootstrapException {
    final ServletContext context = config.getServletContext();
    final String path = context.getRealPath("WEB-INF/conf/restcomm.xml");
    if(logger.isInfoEnabled()) {
      logger.info("loading configuration file located at " + path);
    }
    // Load the restcomm configuration.
    XMLConfiguration configuration = null;
    try {
	  configuration = new XMLConfiguration(path);
	} catch(final ConfigurationException exception) {
      logger.error("The RestComm environment could not be bootstrapped.", exception);
      throw new BootstrapException(exception);
	}
    // Register the services with the service locator.
    final ServiceLocator services = ServiceLocator.getInstance();
    try {
      services.set(InterpreterExecutor.class, new InterpreterExecutor());
      final CallManager callManager = (CallManager)context.getAttribute("org.mobicents.servlet.sip.restcomm.callmanager.CallManager");
      services.set(CallManager.class, callManager);
      final ConferenceCenter conferenceCenter = (ConferenceCenter)context.getAttribute("org.mobicents.servlet.sip.restcomm.callmanager.ConferenceCenter");
      services.set(ConferenceCenter.class, conferenceCenter);
      services.set(MgcpServerManager.class, getMgcpServerManager(configuration));
      services.set(SmsAggregator.class, getSmsAggregator(configuration));
      services.set(SpeechSynthesizer.class, getSpeechSynthesizer(configuration));
    } catch(final ObjectInstantiationException exception) {
      logger.error("The RestComm environment could not be bootstrapped.", exception);
      throw new BootstrapException(exception);
    }
  }
  
  private static MgcpServerManager getMgcpServerManager(final Configuration configuration) throws ObjectInstantiationException {
    final MgcpServerManager mgcpServerManager = new MgcpServerManager();
	mgcpServerManager.configure(configuration);
	mgcpServerManager.start();
	return mgcpServerManager;
  }
  
  private static SmsAggregator getSmsAggregator(final Configuration configuration) throws ObjectInstantiationException {
    final String classpath = configuration.getString("sms-aggregator[@class]");
	final SmsAggregator smsAggregator = (SmsAggregator)ObjectFactory.getInstance().getObjectInstance(classpath);
	smsAggregator.configure(configuration.subset("sms-aggregator"));
	smsAggregator.start();
	return smsAggregator;
  }
  
  private static SpeechSynthesizer getSpeechSynthesizer(final Configuration configuration) throws ObjectInstantiationException {
    final String classpath = configuration.getString("speech-synthesizer[@class]");
    final SpeechSynthesizer speechSynthesizer = (SpeechSynthesizer)ObjectFactory.getInstance().getObjectInstance(classpath);
    speechSynthesizer.configure(configuration);
    speechSynthesizer.start();
    return speechSynthesizer;
  }
}
