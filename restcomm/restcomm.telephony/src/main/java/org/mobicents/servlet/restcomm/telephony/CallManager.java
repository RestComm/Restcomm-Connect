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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import static javax.servlet.sip.SipServletResponse.*;

import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Registration;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.interpreter.StartInterpreter;
import org.mobicents.servlet.restcomm.interpreter.VoiceInterpreterBuilder;
import org.mobicents.servlet.restcomm.util.DigestAuthentication;

import static org.mobicents.servlet.restcomm.util.HexadecimalUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class CallManager extends UntypedActor {
  private final ActorSystem system;
  private final Configuration configuration;
  private final ActorRef conferences;
  private final ActorRef gateway;
  private final ActorRef sms;
  private final SipFactory factory;
  private final DaoManager storage;
  
  private boolean useTo;
  
  public CallManager(final Configuration configuration, final ActorSystem system,
      final ActorRef gateway, final ActorRef conferences, final ActorRef sms,
      final SipFactory factory, final DaoManager storage) {
    super();
    this.system = system;
    this.configuration = configuration;
    this.gateway = gateway;
    this.conferences = conferences;
    this.sms = sms;
    this.factory = factory;
    this.storage = storage;
    final Configuration runtime = configuration.subset("runtime-settings");
    this.useTo = runtime.getBoolean("use-to");
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
  
  private ActorRef call() {
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
  
  private void destroy(final Object message) {
	final UntypedActorContext context = getContext();
    final DestroyCall request = (DestroyCall)message;
    context.stop(request.call());
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
    // Make sure we handle re-invites properly.
    if(!request.isInitial()) {
      final SipServletResponse okay = request.createResponse(SC_OK);
      okay.send();
      return;
    }
    // If it's a new invite lets try to handle it.
    final AccountsDao accounts = storage.getAccountsDao();
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
        return;
      } else {
    	final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
        builder.setConfiguration(configuration);
        builder.setStorage(storage);
        builder.setCallManager(self);
        builder.setConferenceManager(conferences);
        builder.setSmsService(sms);
        builder.setAccount(client.getAccountSid());
        builder.setVersion(client.getApiVersion());
        final Account account = accounts.getAccount(client.getAccountSid());
        builder.setEmailAddress(account.getEmailAddress());
        final Sid sid = client.getVoiceApplicationSid();
        if(sid != null) {
          final Application application = applications.getApplication(sid);
          builder.setUrl(application.getVoiceUrl());
          builder.setMethod(application.getVoiceMethod());
          builder.setFallbackUrl(application.getVoiceFallbackUrl());
          builder.setFallbackMethod(application.getVoiceFallbackMethod());
        } else {
          builder.setUrl(client.getVoiceUrl());
          builder.setMethod(client.getVoiceMethod());
          builder.setFallbackUrl(client.getVoiceFallbackUrl());
          builder.setFallbackMethod(client.getVoiceFallbackMethod());
        }
        final ActorRef interpreter = builder.build();
        final ActorRef call = call();
        final SipApplicationSession application = request.getApplicationSession();
        application.setAttribute(Call.class.getName(), call);
        call.tell(request, self);
        interpreter.tell(new StartInterpreter(call), self);
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
    	final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
        builder.setConfiguration(configuration);
        builder.setStorage(storage);
        builder.setCallManager(self);
        builder.setConferenceManager(conferences);
        builder.setSmsService(sms);
        builder.setAccount(number.getAccountSid());
        builder.setVersion(number.getApiVersion());
        final Account account = accounts.getAccount(number.getAccountSid());
        builder.setEmailAddress(account.getEmailAddress());
        final Sid sid = number.getVoiceApplicationSid();
        if(sid != null) {
          final Application application = applications.getApplication(sid);
          builder.setUrl(application.getVoiceUrl());
          builder.setMethod(application.getVoiceMethod());
          builder.setFallbackUrl(application.getVoiceFallbackUrl());
          builder.setFallbackMethod(application.getVoiceFallbackMethod());
          builder.setStatusCallback(application.getStatusCallback());
          builder.setStatusCallbackMethod(application.getStatusCallbackMethod());
        } else {
          builder.setUrl(number.getVoiceUrl());
          builder.setMethod(number.getVoiceMethod());
          builder.setFallbackUrl(number.getVoiceFallbackUrl());
          builder.setFallbackMethod(number.getVoiceFallbackMethod());
          builder.setStatusCallback(number.getStatusCallback());
          builder.setStatusCallbackMethod(number.getStatusCallbackMethod());
        }
        final ActorRef interpreter = builder.build();
        final ActorRef call = call();
        final SipApplicationSession application = request.getApplicationSession();
        application.setAttribute(Call.class.getName(), call);
        call.tell(request, self);
        interpreter.tell(new StartInterpreter(call), self);
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
    final Class<?> klass = message.getClass();
    final ActorRef self = self();
    final ActorRef sender = sender();
    if(message instanceof SipServletRequest) {
      final SipServletRequest request = (SipServletRequest)message;
      final String method = request.getMethod();
      if("INVITE".equals(method)) {
    	check(request);
        invite(request);
      } else if("OPTIONS".equals(method)) {
        pong(request);
      } else if("CANCEL".equals(method) || "BYE".equals(method)) {
        inDialogRequest(request);
      }
    } else if(CreateCall.class.equals(klass)) {
      try {
        sender.tell(new CallManagerResponse<ActorRef>(outbound(message)), self);
      } catch(final Exception exception) {
        sender.tell(new CallManagerResponse<ActorRef>(exception), self);
      }
    } else if(ExecuteCallScript.class.equals(klass)) {
      execute(message);
    } else if(DestroyCall.class.equals(klass)) {
      destroy(message);
    } else if(message instanceof SipServletResponse) {
      response(message);
    } else if(message instanceof SipApplicationSessionEvent) {
      timeout(message);
    }
  }
  
  private void execute(final Object message) {
    final ExecuteCallScript request = (ExecuteCallScript)message;
    final ActorRef self = self();
    final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
    builder.setConfiguration(configuration);
    builder.setStorage(storage);
    builder.setCallManager(self);
    builder.setConferenceManager(conferences);
    builder.setSmsService(sms);
    builder.setAccount(request.account());
    builder.setVersion(request.version());
    builder.setUrl(request.url());
    builder.setMethod(request.method());
    builder.setFallbackUrl(request.fallbackUrl());
    builder.setFallbackMethod(request.fallbackMethod());
    builder.setStatusCallback(request.callback());
    builder.setStatusCallbackMethod(request.callbackMethod());
    final ActorRef interpreter = builder.build();
    interpreter.tell(new StartInterpreter(request.call()), self);
  }
  
  private ActorRef outbound(final Object message) throws ServletParseException {
    final CreateCall request = (CreateCall)message;
    final Configuration runtime = configuration.subset("runtime-settings");
    final String uri = runtime.getString("outbound-proxy-uri");
    final SipURI from = factory.createSipURI(request.from(), uri);
    SipURI to = null;
    switch(request.type()) {
      case CLIENT: {
        final RegistrationsDao registrations = storage.getRegistrationsDao();
        final Registration registration = registrations.getRegistration(request.to());
        if(registration != null) {
          final String location = registration.getLocation();
          to = (SipURI)factory.createURI(location);
        } else {
          throw new NullPointerException(request.to() + " is not currently registered.");
        }
        break;
      }
      case PSTN: {
        to = factory.createSipURI(request.to(), uri);
        break;
      }
      case SIP: {
        to = (SipURI)factory.createURI(request.to());
        break;
      }
    }
    final ActorRef call = call();
    final ActorRef self = self();
    final InitializeOutbound init = new InitializeOutbound(null, from, to,
        request.timeout(), request.isFromApi());
    call.tell(init, self);
    return call;
  }
  
  public void inDialogRequest(final Object message) {
    final ActorRef self = self();
    final SipServletRequest request = (SipServletRequest)message;
    final SipApplicationSession application = request.getApplicationSession();
    final ActorRef call = (ActorRef)application.getAttribute(Call.class.getName());
    call.tell(request, self);
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
