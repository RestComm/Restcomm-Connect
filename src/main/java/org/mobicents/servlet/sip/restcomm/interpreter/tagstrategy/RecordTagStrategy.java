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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.http.RequestMethod;
import org.mobicents.servlet.sip.restcomm.http.client.ResourceDescriptor;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.Interpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterContext;
import org.mobicents.servlet.sip.restcomm.storage.Storage;
import org.mobicents.servlet.sip.restcomm.util.UriUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Action;
import org.mobicents.servlet.sip.restcomm.xml.rcml.MaxLength;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Method;

public final class RecordTagStrategy extends TwiMLTagStrategy {
  private static final Storage STORAGE = null;
  private static final String BASE_PATH = STORAGE.getPath() + "recordings/";
  private static final String BASE_HTTP_URI = STORAGE.getHttpUri() + "recordings/";
  private static final int ONE_SECOND = 1000;
  
  public RecordTagStrategy() {
    super();
  }
  
  @Override public void execute(final Interpreter interpreter,
      final InterpreterContext context, final Tag tag) throws TagStrategyException {
    // Try to answer the call if it hasn't been done so already.
    final Call call = context.getCall();
	answer(call);
	// Record some media.
	final int maxLength = Integer.parseInt(tag.getAttribute(MaxLength.NAME).getValue()) * ONE_SECOND;
	final String name = UUID.randomUUID().toString();
	final URI path = URI.create(BASE_PATH + name + ".wav");

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
    final URI recordingUri = URI.create(BASE_HTTP_URI + name + ".wav");
    final ResourceDescriptor descriptor = getResourceDescriptor(uri, method, recordingUri, maxLength);
    try {
	  interpreter.loadResource(descriptor);
	} catch(final InterpreterException exception) {
	  interpreter.failed();
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
      final String message = UriUtils.toQueryString(attributes);
      descriptor.setMessage(message.getBytes());
    }
    return descriptor;
  }
}
