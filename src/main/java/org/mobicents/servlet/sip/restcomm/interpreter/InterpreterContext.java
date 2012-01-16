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
import java.util.HashMap;
import java.util.Map;

import org.mobicents.servlet.sip.restcomm.Application;
import org.mobicents.servlet.sip.restcomm.Environment;
import org.mobicents.servlet.sip.restcomm.applicationindex.ApplicationIndex;
import org.mobicents.servlet.sip.restcomm.applicationindex.ApplicationIndexException;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.http.RequestMethod;
import org.mobicents.servlet.sip.restcomm.http.client.ResourceDescriptor;
import org.mobicents.servlet.sip.restcomm.http.client.ResourceServer;
import org.mobicents.servlet.sip.restcomm.http.client.ResourceServerFactory;
import org.mobicents.servlet.sip.restcomm.util.UriUtils;

public final class InterpreterContext {
  private final Call call;
  
  public InterpreterContext(final Call call) {
    super();
    this.call = call;
  }
  
  private Application getApplication() throws InterpreterContextException {
	final String endpoint = call.getRecipient();
    try {
      final Environment environment = Environment.getInstance();
      final ApplicationIndex index = environment.getApplicationIndex();
	  return index.locate(endpoint);
	} catch(final ApplicationIndexException exception) {
	  throw new InterpreterContextException(exception);
	}
  }
  
  public Call getCall() {
    return call;
  }
  
  public ResourceDescriptor getEntryPointDescriptor() throws InterpreterContextException {
    // Create a resource descriptor for the application's entry point.
    final Application application = getApplication();
    final String method = application.getRequestMethod();
    final URI uri = application.getUri();
	final ResourceDescriptor descriptor = new ResourceDescriptor(uri);
	descriptor.setMethod(method);
	// Append the attributes to the request.
    final Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("CallSid", call.getId());
    attributes.put("From", call.getOriginator());
    attributes.put("To", call.getRecipient());
    attributes.put("CallStatus", call.getStatus());
    attributes.put("ApiVersion", "2010-04-01");
    attributes.put("Direction", call.getDirection());
	if(method.equals(RequestMethod.GET)) {
	  descriptor.setAttributes(attributes);
	} else if(method.equals(RequestMethod.POST)) {
	  final String message = UriUtils.toQueryString(attributes);
	  descriptor.setMessage(message.getBytes());
	}
    return descriptor;
  }
  
  public ResourceServer getResourceServer() {
    return ResourceServerFactory.getInstance().getResourceServerInstance();
  }
}
