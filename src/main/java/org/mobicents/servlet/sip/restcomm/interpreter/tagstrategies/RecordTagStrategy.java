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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategies;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.Environment;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.MediaRecorder;
import org.mobicents.servlet.sip.restcomm.http.RequestMethod;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreterContext;
import org.mobicents.servlet.sip.restcomm.resourceserver.ResourceDescriptor;
import org.mobicents.servlet.sip.restcomm.util.UrlUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Action;
import org.mobicents.servlet.sip.restcomm.xml.twiml.MaxLength;
import org.mobicents.servlet.sip.restcomm.xml.twiml.Method;

public final class RecordTagStrategy extends TwiMLTagStrategy {
  private static final Logger LOGGER = Logger.getLogger(RecordTagStrategy.class);
  private static final int ONE_SECOND = 1000;
  
  public RecordTagStrategy() {
    super();
  }
  
  @Override public void execute(final TwiMLInterpreter interpreter,
      final TwiMLInterpreterContext context, final Tag tag) throws TagStrategyException {
    // Try to answer the call if it hasn't been done so already.
    final Call call = context.getCall();
	answer(call);
	// Get a reference to the environment.
	final Environment environment = Environment.getInstance();
	// Record some media.
	final int maxLength = Integer.parseInt(tag.getAttribute(MaxLength.NAME).getValue()) * ONE_SECOND;
	final URI path = URI.create(environment.getRecordingsPath() + "/" + UUID.randomUUID().toString() + ".wav");
	final MediaRecorder recorder = call.getRecorder();
	recorder.record(path);
	synchronized(this) {
	  try {
	    wait(maxLength);
	  } catch(final InterruptedException exception) {
	    LOGGER.error(exception);
	  }
	}
	recorder.stop();
	// Do something with the recording.
    final Attribute action = tag.getAttribute(Action.NAME);
    final String method = tag.getAttribute(Method.NAME).getValue();
    final URI base = interpreter.getDescriptor().getUri();
    URI uri = null;
    if(action != null) {
      uri = base.resolve(action.getValue());
    } else {
      uri = base;
    }
    final URI recordingUri = environment.getRecordingsUri();
    final ResourceDescriptor descriptor = getResourceDescriptor(uri, method, recordingUri, maxLength);
    try {
	  interpreter.loadResource(descriptor);
	} catch(final InterpreterException exception) {
	  throw new TagStrategyException(exception);
	}
    interpreter.redirect();
  }
  
  private ResourceDescriptor getResourceDescriptor(final URI uri, String method, URI recordingUri,
      int recordingDuration) {
    final ResourceDescriptor descriptor = new ResourceDescriptor(uri);
    descriptor.setMethod(method);
    final Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("RecordingUrl", recordingUri);
    attributes.put("RecordingDuration", recordingDuration);
    attributes.put("Digits", "");
    if(method.equals(RequestMethod.GET)) {
      descriptor.setAttributes(attributes);
    } else if(method.equals(RequestMethod.POST)) {
      final String message = UrlUtils.toQueryString(attributes);
      descriptor.setMessage(message.getBytes());
    }
    return descriptor;
  }
}
