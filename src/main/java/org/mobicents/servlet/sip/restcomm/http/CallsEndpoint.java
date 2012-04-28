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
package org.mobicents.servlet.sip.restcomm.http;

import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import java.net.URI;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.AuthorizationException;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterExecutor;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Path("/Accounts/{accountSid}/Calls")
@ThreadSafe public final class CallsEndpoint extends AbstractEndpoint {
  private final CallManager callManager;
  private final InterpreterExecutor executor;

  public CallsEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    callManager = services.get(CallManager.class);
    executor = services.get(InterpreterExecutor.class);
  }
  
  @POST public Response putClient(@PathParam("accountSid") String accountSid, final MultivaluedMap<String, String> data) {
    try { secure(new Sid(accountSid), "RestComm:Create:Calls"); }
	catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
    try {
      validate(data);
      final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
      final String from = phoneNumberUtil.format(phoneNumberUtil.parse(data.getFirst("From"), "US"), PhoneNumberFormat.E164);
      final String to = phoneNumberUtil.format(phoneNumberUtil.parse(data.getFirst("To"), "US"), PhoneNumberFormat.E164);
      final Call call = callManager.createExternalCall(from, to);
      final URI url = URI.create(data.getFirst("Url"));
      executor.submit(new Sid(accountSid), getApiVersion(data), url, getMethod("VoiceMethod", data), null, null, call);
      return ok().build();
    } catch(final Exception exception) { 
      return status(BAD_REQUEST).entity(exception.getMessage()).build();
    }
  }
  
  private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
    if(!data.containsKey("From")) {
      throw new NullPointerException("From can not be null.");
    } else if(!data.containsKey("To")) {
      throw new NullPointerException("To can not be null.");
    } else if(!data.containsKey("Url")) {
      throw new NullPointerException("Url can not be null.");
    }
  }
}
