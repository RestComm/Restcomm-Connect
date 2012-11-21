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
import org.mobicents.servlet.sip.restcomm.asr.SpeechRecognizer;
import org.mobicents.servlet.sip.restcomm.callmanager.mgcp.MgcpConferenceCenter;
import org.mobicents.servlet.sip.restcomm.callmanager.mgcp.MgcpServerManager;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.fax.FaxService;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;
import org.mobicents.servlet.sip.restcomm.media.api.CallManager;
import org.mobicents.servlet.sip.restcomm.media.api.ConferenceCenter;
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
  
  private static String getRestCommPath(final ServletConfig config) {
	final ServletContext context = config.getServletContext();
    final String path = context.getRealPath("/");
    if(path.endsWith("/")) {
      return path.substring(0, path.length() - 1);
    } else {
      return path;
    }
  }
  
  private static String getRestCommUri(final ServletConfig config) {
	return config.getServletContext().getContextPath();
  }
  
  public static void bootstrap(final ServletConfig config) throws BootstrapException {
    final ServletContext context = config.getServletContext();
    final String path = context.getRealPath("WEB-INF/conf/restcomm.xml");
    if(logger.isInfoEnabled()) {
      logger.info("loading configuration file located at " + path);
    }
    // Initialize the configuration interpolator.
    final ConfigurationStringLookup strings = new ConfigurationStringLookup();
    strings.addProperty("home", getRestCommPath(config));
    strings.addProperty("uri", getRestCommUri(config));
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
      final Configuration runtimeConfiguration = configuration.subset("runtime-settings");
      runtimeConfiguration.setProperty("home-directory", getRestCommPath(config));
      runtimeConfiguration.setProperty("root-uri", getRestCommUri(config));
      services.set(Configuration.class, runtimeConfiguration);
      services.set(TimerManager.class, new TimerManager());
      services.set(InterpreterExecutor.class, new InterpreterExecutor());
      final MgcpServerManager serverManager = getMgcpServerManager(configuration);
      services.set(MgcpServerManager.class, serverManager);
      final CallManager callManager = (CallManager)context.getAttribute("org.mobicents.servlet.sip.restcomm.callmanager.CallManager");
      services.set(CallManager.class, callManager);
      services.set(ConferenceCenter.class, getConferenceCenter(serverManager));
      services.set(DaoManager.class, getDaoManager(configuration));
      services.set(FaxService.class, getFaxService(configuration));
      services.set(SmsAggregator.class, getSmsAggregator(configuration));
      services.set(SpeechRecognizer.class, getSpeechRecognizer(configuration));
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
  
  private static ConferenceCenter getConferenceCenter(final MgcpServerManager serverManager) {
    return new MgcpConferenceCenter(serverManager);
  }
  
  private static DaoManager getDaoManager(final Configuration configuration) throws ObjectInstantiationException {
    final String classpath = configuration.getString("dao-manager[@class]");
    final DaoManager daoManager = (DaoManager)ObjectFactory.getInstance().getObjectInstance(classpath);
    daoManager.configure(configuration.subset("dao-manager"));
    daoManager.start();
    return daoManager;
  }
  
  private static FaxService getFaxService(final Configuration configuration) throws ObjectInstantiationException {
    final String classpath = configuration.getString("fax-service[@class]");
    final FaxService faxService = (FaxService)ObjectFactory.getInstance().getObjectInstance(classpath);
    faxService.configure(configuration.subset("fax-service"));
    faxService.start();
    return faxService;
  }
  
  private static SmsAggregator getSmsAggregator(final Configuration configuration) throws ObjectInstantiationException {
    final String classpath = configuration.getString("sms-aggregator[@class]");
	final SmsAggregator smsAggregator = (SmsAggregator)ObjectFactory.getInstance().getObjectInstance(classpath);
	smsAggregator.configure(configuration.subset("sms-aggregator"));
	smsAggregator.start();
	return smsAggregator;
  }
  
  private static SpeechRecognizer getSpeechRecognizer(final Configuration configuration) throws ObjectInstantiationException {
    final String classpath = configuration.getString("speech-recognizer[@class]");
    final SpeechRecognizer speechRecognizer = (SpeechRecognizer)ObjectFactory.getInstance().getObjectInstance(classpath);
    speechRecognizer.configure(configuration.subset("speech-recognizer"));
    speechRecognizer.start();
    return speechRecognizer;
  }
  
  private static SpeechSynthesizer getSpeechSynthesizer(final Configuration configuration) throws ObjectInstantiationException {
    final String classpath = configuration.getString("speech-synthesizer[@class]");
    final SpeechSynthesizer speechSynthesizer = (SpeechSynthesizer)ObjectFactory.getInstance().getObjectInstance(classpath);
    speechSynthesizer.configure(configuration.subset("speech-synthesizer"));
    speechSynthesizer.start();
    return speechSynthesizer;
  }
}
