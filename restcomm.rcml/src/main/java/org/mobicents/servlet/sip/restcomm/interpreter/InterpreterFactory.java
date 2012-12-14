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
package org.mobicents.servlet.sip.restcomm.interpreter;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class InterpreterFactory implements LifeCycle {
  private final ExecutorService executor;
  private final Map<Sid, RcmlInterpreter> interpreters;

  public InterpreterFactory() {
    super();
    executor = Executors.newCachedThreadPool();
    interpreters = new ConcurrentHashMap<Sid, RcmlInterpreter>();
  }

  @Override public void start() throws RuntimeException {
    // Nothing to do.
  }

  @Override public void shutdown() {
    if(!executor.isShutdown()) {
      executor.shutdown();
      try {
		executor.awaitTermination(60, TimeUnit.SECONDS);
	  } catch(final InterruptedException ignored) { }
    }
  }
  
  public RcmlInterpreter remove(final Sid sid) {
    if(interpreters.containsKey(sid)) {
      final RcmlInterpreter interpreter = interpreters.remove(sid);
      interpreter.finishAndInterrupt();
      return interpreter;
    } else {
      return null;
    }
  }
  
  public BridgeRcmlInterpreter create(final Sid accountSid, final String apiVersion, final URI url,
      final String method, final Call call) {
	  final VoiceRcmlInterpreterContext context = new VoiceRcmlInterpreterContext(accountSid,
	      apiVersion, url, method, null, "POST", null, "POST", call);
	  final BridgeRcmlInterpreter interpreter = new BridgeRcmlInterpreter(context, this);
	  executor.submit(interpreter);
	  interpreters.put(call.getSid(), interpreter);
	  return interpreter;
  }
  
  public ConferenceRcmlInterpreter create(final Sid accountSid, final String apiVersion, final URI waitUrl,
      final String waitMethod, final Conference conference) {
	  final ConferenceRcmlInterpreterContext context = new ConferenceRcmlInterpreterContext(accountSid,
	      apiVersion, waitUrl, waitMethod, conference);
	  final ConferenceRcmlInterpreter interpreter = new ConferenceRcmlInterpreter(context, this);
	  executor.submit(interpreter);
	  interpreters.put(conference.getSid(), interpreter);
	  return interpreter;
  }
  
  public VoiceRcmlInterpreter create(final Sid accountSid, final String apiVersion, final URI voiceUrl,
      final String voiceMethod, final URI voiceFallbackUrl, final String voiceFallbackMethod, 
      final URI statusCallback, final String statusCallbackMethod, final Call call) {
    final VoiceRcmlInterpreterContext context = new VoiceRcmlInterpreterContext(accountSid, apiVersion, voiceUrl,
        voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, call);
    final VoiceRcmlInterpreter interpreter = new VoiceRcmlInterpreter(context, this);
    executor.submit(interpreter);
    interpreters.put(call.getSid(), interpreter);
    return interpreter;
  }
  
  public VoiceRcmlInterpreter create(final Sid accountSid, final String apiVersion, final URI voiceUrl,
      final String voiceMethod, final URI voiceFallbackUrl, final String voiceFallbackMethod, 
      final URI statusCallback, final String statusCallbackMethod, final int timeout,
      final Call call) {
    final VoiceRcmlInterpreterContext context = new VoiceRcmlInterpreterContext(accountSid, apiVersion, voiceUrl,
	    voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, timeout,
	    call);
    final VoiceRcmlInterpreter interpreter = new VoiceRcmlInterpreter(context, this);
	executor.submit(interpreter);
	interpreters.put(call.getSid(), interpreter);
	return interpreter;
  }
}
