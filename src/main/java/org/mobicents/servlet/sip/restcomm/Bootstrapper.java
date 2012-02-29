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
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.callmanager.mgcp.MgcpServerManager;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
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
    // Initialize the configuration interpolator.
    final ConfigurationStringLookup strings = new ConfigurationStringLookup();
    strings.addProperty("home", context.getRealPath("/"));
    ConfigurationInterpolator.registerGlobalLookup("restcomm", strings);
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
      services.set(Configuration.class, configuration.subset("runtime-settings"));
      services.set(InterpreterExecutor.class, new InterpreterExecutor());
      services.set(MgcpServerManager.class, getMgcpServerManager(configuration));
      final CallManager callManager = (CallManager)context.getAttribute("org.mobicents.servlet.sip.restcomm.callmanager.CallManager");
      services.set(CallManager.class, callManager);
      // services.set(ConferenceCenter.class, getConferenceCenter(configuration));
      services.set(DaoManager.class, getDaoManager(configuration));
      services.set(SmsAggregator.class, getSmsAggregator(configuration));
      services.set(SpeechSynthesizer.class, getSpeechSynthesizer(configuration));
    } catch(final ObjectInstantiationException exception) {
      logger.error("The RestComm environment could not be bootstrapped.", exception);
      throw new BootstrapException(exception);
    }
  }
  
  private static MgcpServerManager getMgcpServerManager(final Configuration configuration) throws ObjectInstantiationException {
    final MgcpServerManager mgcpServerManager = new MgcpServerManager();
	mgcpServerManager.configure(configuration.subset("media-server-manager"));
	mgcpServerManager.start();
	return mgcpServerManager;
  }
  
  private static ConferenceCenter getConferenceCenter(final Configuration configuration) {
    return null;
  }
  
  private static DaoManager getDaoManager(final Configuration configuration) throws ObjectInstantiationException {
    final String classpath = configuration.getString("dao-manager[@class]");
    final DaoManager daoManager = (DaoManager)ObjectFactory.getInstance().getObjectInstance(classpath);
    daoManager.configure(configuration.subset("dao-manager"));
    daoManager.start();
    return daoManager;
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
    speechSynthesizer.configure(configuration.subset("speech-synthesizer"));
    speechSynthesizer.start();
    return speechSynthesizer;
  }
}
