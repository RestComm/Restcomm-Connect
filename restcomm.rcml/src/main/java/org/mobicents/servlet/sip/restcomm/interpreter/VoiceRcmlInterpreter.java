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

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.media.api.Call;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public class VoiceRcmlInterpreter extends RcmlInterpreter {
  public static final Logger logger = Logger.getLogger(VoiceRcmlInterpreter.class);
  
  protected final VoiceRcmlInterpreterContext context;
  protected final InterpreterFactory factory;

  public VoiceRcmlInterpreter(VoiceRcmlInterpreterContext context,
      final InterpreterFactory factory) {
    super(context, new VoiceTagStrategyFactory());
    this.context = context;
    this.factory = factory;
  }
  
  protected VoiceRcmlInterpreter(final VoiceRcmlInterpreterContext context,
      final TagStrategyFactory strategies, final InterpreterFactory factory) {
    super(context, strategies);
    this.context = context;
    this.factory = factory;
  }
  
  protected void cleanup() {
    final Call call = context.getCall();
	if(Call.Status.IN_PROGRESS == call.getStatus()) {
	  call.hangup();
	}
	sendStatusCallback();
	factory.remove(call.getSid());
  }

  @Override protected void initialize() {
    URI url = context.getVoiceUrl();
    String method = context.getVoiceMethod();
    if(url != null && method != null && !method.isEmpty()) {
      try {
	    load(url, method, context.getRcmlRequestParameters());
	    setState(READY);
	  } catch(final InterpreterException exception) {
	    logger.warn(exception);
	    url = context.getVoiceFallbackUrl();
	    method = context.getVoiceFallbackMethod();
	    if(url != null && method != null && !method.isEmpty()) {
	      try {
	        load(url, method, context.getRcmlRequestParameters());
		    setState(READY);
	      } catch(final InterpreterException fallbackException) {
	        logger.warn(exception);
	      }
	    }
	  }
    }
  }
  
  @Override public Notification notify(final RcmlInterpreterContext context, final int log, final int errorCode,
      final URI resourceUri, final String requestMethod, final String requestVariables, final String responseBody,
      final String responseHeaders) {
    final Notification notification = super.notify(context, log, errorCode, resourceUri, requestMethod, requestVariables,
        responseBody, responseHeaders);
    final Notification.Builder builder = Notification.builder();
    builder.setSid(notification.getSid());
	builder.setAccountSid(notification.getAccountSid());
	builder.setApiVersion(notification.getApiVersion());
	final VoiceRcmlInterpreterContext voiceContext = (VoiceRcmlInterpreterContext)context;
	final Call call = voiceContext.getCall();
	builder.setCallSid(call.getSid());
	builder.setLog(notification.getLog());
	builder.setErrorCode(notification.getErrorCode());
	builder.setMoreInfo(notification.getMoreInfo());
	builder.setMessageText(notification.getMessageText());
	builder.setMessageDate(notification.getMessageDate());
	builder.setRequestUrl(notification.getRequestUrl());
	builder.setRequestMethod(notification.getRequestMethod());
	builder.setRequestVariables(notification.getRequestVariables());
	builder.setResponseBody(notification.getResponseBody());
	builder.setResponseHeaders(notification.getResponseHeaders());
	builder.setUri(notification.getUri());
	return builder.build();
  }
}
