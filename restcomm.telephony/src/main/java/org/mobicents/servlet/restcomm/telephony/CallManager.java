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
package org.mobicents.servlet.restcomm.telephony;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import static javax.servlet.sip.SipServletResponse.*;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;

import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.util.DigestAuthentication;
import static org.mobicents.servlet.restcomm.util.HexadecimalUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class CallManager extends UntypedActor {
  private final ActorSystem system;
  private final ActorRef gateway;
  private final SipFactory factory;
  private final DaoManager storage;
  
  private boolean useTo;
  
  public CallManager(final Configuration configuration, final ActorSystem system,
      final ActorRef gateway, final SipFactory factory, final DaoManager storage) {
    super();
    this.system = system;
    this.gateway = gateway;
    this.factory = factory;
    this.storage = storage;
    this.useTo = configuration.getBoolean("use-to");
  }
  
  private void authenticate(final Object message) throws IOException {
    final SipServletRequest request = (SipServletRequest)message;
    final SipServletResponse response = request.createResponse(SC_PROXY_AUTHENTICATION_REQUIRED);
    final String nonce = nonce();
    final SipURI uri = (SipURI)request.getTo().getURI();
    final String realm = uri.getHost();
    final String header = header(nonce, realm, "Digest");
    response.addHeader("Proxy-Authenticate", header);
    response.send();
  }
  
  private ActorRef call(final SipFactory factory, final ActorRef gateway) {
    return system.actorOf(new Props(new UntypedActorFactory() {
		private static final long serialVersionUID = 1L;
		@Override public UntypedActor create() throws Exception {
          return new Call(factory, gateway);
		}
    }));
  }
  
  private void check(final Object message) throws IOException {
	final SipServletRequest request = (SipServletRequest)message;
    if(request.getContentLength() == 0 || !"application/sdp".equals(request.getContentType())) {
      final SipServletResponse response = request.createResponse(SC_BAD_REQUEST);
      response.send();
    }
  }
  
  private String header(final String nonce, final String realm, final String scheme) {
	final StringBuilder buffer = new StringBuilder();
	buffer.append(scheme).append(" ");
	buffer.append("realm=\"").append(realm).append("\", ");
	buffer.append("nonce=\"").append(nonce).append("\"");
    return buffer.toString();
  }
  
  private void invite(final Object message) throws IOException, NumberParseException {
    final ActorRef self = self();
    final SipServletRequest request = (SipServletRequest)message;
    final ApplicationsDao applications = storage.getApplicationsDao();
    // Try to find an application defined for the client.
    SipURI uri = (SipURI)request.getFrom().getURI();
    String id = uri.getUser();
    final ClientsDao clients = storage.getClientsDao();
    final Client client = clients.getClient(id);
    if(client != null) {
      // Make sure we force clients to authenticate.
      final String authorization = request.getHeader("Proxy-Authorization");
      final String method = request.getMethod();
      if(authorization == null || !permitted(authorization, method)) {
        authenticate(request);
      } else {
        final Sid sid = client.getVoiceApplicationSid();
        if(sid != null) {
          final Application application = applications.getApplication(sid);
        }
        final ActorRef call = call(factory, gateway);
        call.tell(request, self);
        return;
      }
    }
    // Try to see if the request is destined for an application we are hosting.
    if(useTo) {
      uri = (SipURI)request.getTo().getURI();
      id = uri.getUser();
    } else {
      uri = (SipURI)request.getRequestURI();
      id = uri.getUser();
    }
    try {
      // Format the destination to an E.164 phone number.
      final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
      final String phone = phoneNumberUtil.format(phoneNumberUtil.parse(id, "US"),
          PhoneNumberFormat.E164);
      // Try to find an application defined for the phone number.
      final IncomingPhoneNumbersDao numbers = storage.getIncomingPhoneNumbersDao();
      final IncomingPhoneNumber number = numbers.getIncomingPhoneNumber(phone);
      if(number != null) {
        final Sid sid = number.getVoiceApplicationSid();
        if(sid != null) {
          final Application application = applications.getApplication(sid);
        }
        final ActorRef call = call(factory, gateway);
        call.tell(request, self);
        return;
      }
    } catch(final NumberParseException ignored) { }
    // We didn't find anyway to handle the call.
    final SipServletResponse response = request.createResponse(SC_NOT_FOUND);
    response.send();
  }
  
  private boolean permitted(final String authorization, final String method) {
  	final Map<String, String> map = toMap(authorization);
  	final String user = map.get("username");
    final String algorithm = map.get("algorithm");
    final String realm = map.get("realm");
    final String uri = map.get("uri");
    final String nonce = map.get("nonce");
    final String nc = map.get("nc");
    final String cnonce = map.get("cnonce");
    final String qop = map.get("qop");
    final String response = map.get("response");
    final ClientsDao clients = storage.getClientsDao();
    final Client client = clients.getClient(user);
    if(client != null && Client.ENABLED == client.getStatus()) {
      final String password = client.getPassword();
      final String result =  DigestAuthentication.response(algorithm, user, realm, password, nonce, nc,
          cnonce, method, uri, null, qop);
      return result.equals(response);
    } else {
      return false;
    }
  }
  
  private String nonce() {
    final byte[] uuid = UUID.randomUUID().toString().getBytes();
    final char[] hex = toHex(uuid);
	return new String(hex).substring(0, 31);
  }
  
  private void pong(final Object message) throws IOException {
    final SipServletRequest request = (SipServletRequest)message;
    final SipServletResponse response = request.createResponse(SC_OK);
    response.send();
  }

  @Override public void onReceive(final Object message) throws Exception {
    if(message instanceof SipServletRequest) {
      final SipServletRequest request = (SipServletRequest)message;
      check(request);
      final String method = request.getMethod();
      if("INVITE".equals(method)) {
        invite(request);
      } else if("OPTIONS".equals(method)) {
        pong(request);
      }
    } else if(message instanceof SipServletResponse) {
      response(message);
    } else if(message instanceof SipApplicationSessionEvent) {
      timeout(message);
    }
  }
  
  public void response(final Object message) {
	final ActorRef self = self();
	final SipServletResponse response = (SipServletResponse)message;
    final SipApplicationSession application = response.getApplicationSession();
    final ActorRef call = (ActorRef)application.getAttribute(Call.class.getName());
    call.tell(response, self);
  }
  
  public void timeout(final Object message) {
	final ActorRef self = self();
	final SipApplicationSessionEvent event = (SipApplicationSessionEvent)message;
    final SipApplicationSession application = event.getApplicationSession();
    final ActorRef call = (ActorRef)application.getAttribute(Call.class.getName());
    final ReceiveTimeout timeout = ReceiveTimeout.getInstance();
    call.tell(timeout, self);
  }
  
  private Map<String, String> toMap(final String header) {
	final Map<String, String> map = new HashMap<String, String>();
	final int endOfScheme = header.indexOf(" ");
	map.put("scheme", header.substring(0, endOfScheme).trim());
	final String[] tokens = header.substring(endOfScheme + 1).split(",");
	for(final String token : tokens) {
	  final String[] values = token.trim().split("=");
	  map.put(values[0].toLowerCase(), values[1].replace("\"", ""));
	}
    return map;
  }
}
