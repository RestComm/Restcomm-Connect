package org.mobicents.servlet.sip.restcomm;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.callmanager.mgcp.MgcpServerManager;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizer;

public final class Janitor {
  public static final Logger logger = Logger.getLogger(Janitor.class);
  
  private Janitor() {
    super();
  }
  
  public static void cleanup() {
    final ServiceLocator services = ServiceLocator.getInstance();
    final InterpreterExecutor interpreterExecutor = services.get(InterpreterExecutor.class);
    interpreterExecutor.shutdown();
    final MgcpServerManager mgcpServerManager = services.get(MgcpServerManager.class);
    mgcpServerManager.shutdown();
    final SmsAggregator smsAggregator = services.get(SmsAggregator.class);
    smsAggregator.shutdown();
    final SpeechSynthesizer speechSynthesizer = services.get(SpeechSynthesizer.class);
    speechSynthesizer.shutdown();
  }
}
