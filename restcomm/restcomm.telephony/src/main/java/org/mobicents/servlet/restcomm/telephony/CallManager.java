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

import static javax.servlet.sip.SipServletResponse.SC_BAD_REQUEST;
import static javax.servlet.sip.SipServletResponse.SC_NOT_FOUND;
import static javax.servlet.sip.SipServletResponse.SC_OK;
import static javax.servlet.sip.SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED;
import static org.mobicents.servlet.restcomm.util.HexadecimalUtils.toHex;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author ivelin.ivanov@telestax.com
 */
public final class CallManager extends UntypedActor {
  private final ActorSystem system;
  private final Configuration configuration;
  private final ActorRef conferences;
  private final ActorRef gateway;
  private final ActorRef sms;
  private final SipFactory sipFactory;
  private final DaoManager storage;
    
  // configurable switch whether to use the To field in a SIP header to determine the callee address
  // alternatively the Request URI can be used
  private boolean useTo;
  
  private static final String B2BUA_LAST_RESPONSE = "lastResponse";
  private static final String B2BUA_LAST_REQUEST = "lastRequest";
  private static final String LINKED_SESSION = "linkedSession";  
  
  private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
  
  public CallManager(final Configuration configuration, final ActorSystem system,
      final ActorRef gateway, final ActorRef conferences, final ActorRef sms,
      final SipFactory factory, final DaoManager storage) {
    super();
    this.system = system;
    this.configuration = configuration;
    this.gateway = gateway;
    this.conferences = conferences;
    this.sms = sms;
    this.sipFactory = factory;
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
          return new Call(sipFactory, gateway);
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
    	// if the client has authenticated, try to redirect to the Client VoiceURL app
    	// otherwise continue trying to process the Client invite
    	if (redirectToClientVoiceApp(self, request, accounts, applications, client)) {
    			return;
    	}
      }
    }
    if(useTo) {
      uri = (SipURI)request.getTo().getURI();
      id = uri.getUser();
    } else {
      uri = (SipURI)request.getRequestURI();
      id = uri.getUser();
    }
    // Try to see if the request is destined for an application we are hosting.
    if (redirectToHostedVoiceApp(self, request, accounts, applications, id)) {
    	return;
    // Next try to see if the request is destined to another registered client
    } else {
    	if (client != null) { // make sure the caller is a registered client and not some external SIP agent that we have little control over
    		Client toClient = clients.getClient(id);
    		if (toClient != null) { // looks like its a p2p attempt between two valid registered clients, lets redirect to the b2bua
                if (redirectToB2BUA(request, client, toClient)) {
            		// if all goes well with proxying the invitation on to the next client
            		// then we can end further processing of this INVITE
            		return;
                }
    		}
    	}
    }
    // We didn't find anyway to handle the call.
    final SipServletResponse response = request.createResponse(SC_NOT_FOUND);
    response.send();
  }

/**
 * @param request
 * @param client
 * @param toClient
 * @throws IOException
 * @throws UnsupportedEncodingException
 */
private boolean redirectToB2BUA(final SipServletRequest request,
		final Client client, Client toClient) throws IOException,
		UnsupportedEncodingException {
	request.getSession().setAttribute("lastRequest", request);
	if(logger.isInfoEnabled()) {
	        logger.info("B2BUA (p2p proxy): Got request:\n"
	                        + request.getMethod());
	        logger.info(String.format("B2BUA: Proxying a call between %s and %s", client.getUri(), toClient.getUri()));
	}
	
	String user = ((SipURI) request.getTo().getURI()).getUser();

	final RegistrationsDao registrations = storage.getRegistrationsDao();
	final Registration registration = registrations.getRegistration(user);
	if(registration != null) {
	    final String location = registration.getLocation();
	    SipURI to;
		try {
			to = (SipURI)sipFactory.createURI(location);

			final SipSession incomingSession = request.getSession();
			//create and send the outgoing invite and do the session linking
			incomingSession.setAttribute(B2BUA_LAST_REQUEST, request); 
			SipServletRequest outRequest = sipFactory.createRequest(request.getApplicationSession(),
	                "INVITE", request.getFrom().getURI(), request.getTo().getURI());
	        outRequest.setRequestURI(to);
	        if(request.getContent() != null) {
	                outRequest.setContent(request.getContent(), request.getContentType());
	        }
	        final SipSession outgoingSession = outRequest.getSession();
	        if(request.isInitial()) {
	        	incomingSession.setAttribute(LINKED_SESSION, outgoingSession);
	        	outgoingSession.setAttribute(LINKED_SESSION, incomingSession);
	        }
	        outgoingSession.setAttribute(B2BUA_LAST_REQUEST, outRequest);	        
	        outRequest.send();
	        return true; // successfully proxied INVITE between two registered clients
		} catch (ServletParseException badUriEx) {
	        if(logger.isInfoEnabled()) {
	            logger.info(String.format("B2BUA: Error parsing Client Contact URI: %s", location), badUriEx);
	        }
		};
	}
	return false;
}

/**
 * 
 * Try to locate a hosted app corresponding to the callee/To address.
 * If one is found, begin execution, otherwise return false;
 * 
 * @param self
 * @param request
 * @param accounts
 * @param applications
 * @param id
 */
private boolean redirectToHostedVoiceApp(final ActorRef self,
		final SipServletRequest request, final AccountsDao accounts,
		final ApplicationsDao applications, String id) {
	boolean isFoundHostedApp = false;
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
        isFoundHostedApp = true;
      } 
  	} catch(final NumberParseException notANumber) {
        isFoundHostedApp = false;
    };
    return isFoundHostedApp;
}

/**
 * 
 *  If there is VoiceUrl provided for a Client configuration,
 *  try to begin execution of the RCML app, otherwise return false.
 * 
 * 
 * @param self
 * @param request
 * @param accounts
 * @param applications
 * @param client
 */
private boolean redirectToClientVoiceApp(final ActorRef self,
		final SipServletRequest request, final AccountsDao accounts,
		final ApplicationsDao applications, final Client client) {
	URI clientAppVoiceUril = client.getVoiceUrl();
	boolean isClientManaged = (clientAppVoiceUril != null); 
	if (isClientManaged) {
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
		  builder.setUrl(clientAppVoiceUril);
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
     }
	return isClientManaged;
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
      } else if("ACK".equals(method)) {
          ack(request);
      } else if("CANCEL".equals(method)) {
    	  cancel(request); 
      } else if ("BYE".equals(method)) {
          bye(request);
      };
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
  
  private void ack(SipServletRequest request) throws IOException {
      SipSession linkedB2BUASession = getLinkedSession(request);
      // if this is an ACK that belongs to a B2BUA session, then we proxy it to the other client
      if (linkedB2BUASession != null) { 	  
          SipServletResponse response = (SipServletResponse) linkedB2BUASession.
        		  getAttribute(B2BUA_LAST_RESPONSE);
	      response.createAck().send();
	      SipApplicationSession sipApplicationSession = request.getApplicationSession();
	      // Defaulting the sip application session to 1h
	      sipApplicationSession.setExpires(60);
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
    final SipURI from = sipFactory.createSipURI(request.from(), uri);
    SipURI to = null;
    switch(request.type()) {
      case CLIENT: {
        final RegistrationsDao registrations = storage.getRegistrationsDao();
        final Registration registration = registrations.getRegistration(request.to());
        if(registration != null) {
          final String location = registration.getLocation();
          to = (SipURI)sipFactory.createURI(location);
        } else {
          throw new NullPointerException(request.to() + " is not currently registered.");
        }
        break;
      }
      case PSTN: {
        to = sipFactory.createSipURI(request.to(), uri);
        break;
      }
      case SIP: {
        to = (SipURI)sipFactory.createURI(request.to());
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
  
  public void cancel(final Object message) throws IOException {
    final ActorRef self = self();
    final SipServletRequest request = (SipServletRequest)message;
    final SipApplicationSession application = request.getApplicationSession();
    
    // if this response is coming from a client that is in a p2p session with another registered client
    // we will just proxy the response
    SipSession linkedB2BUASession = getLinkedSession(request);
    if (linkedB2BUASession != null) {
	    if(logger.isInfoEnabled()) {
	        logger.info(String.format("B2BUA: Got CANCEL request: \n %s", request));
	    }
	    SipServletRequest originalRequest = (SipServletRequest) linkedB2BUASession.getAttribute(LINKED_SESSION);
	    originalRequest.createCancel().send();
    } else {
	    final ActorRef call = (ActorRef)application.getAttribute(Call.class.getName());
	    call.tell(request, self);
    }
  }

  public void bye(final Object message) throws IOException {
    final ActorRef self = self();
    final SipServletRequest request = (SipServletRequest)message;
    final SipApplicationSession application = request.getApplicationSession();
    
    // if this response is coming from a client that is in a p2p session with another registered client
    // we will just proxy the response
    SipSession linkedB2BUASession = getLinkedSession(request);
    if (linkedB2BUASession != null) {
	    if(logger.isInfoEnabled()) {
	        logger.info(String.format("B2BUA: Got BYE request: \n %s", request));
	    }
	    linkedB2BUASession.createRequest("BYE").send();
    } else {
	    final ActorRef call = (ActorRef)application.getAttribute(Call.class.getName());
	    call.tell(request, self);
    }
  }  
  
  public void response(final Object message) throws UnsupportedEncodingException, IOException {
	final ActorRef self = self();
	final SipServletResponse response = (SipServletResponse)message;
    final SipApplicationSession application = response.getApplicationSession();
    
    SipSession linkedB2BUASession = getLinkedSession(response);
    // if this response is coming from a client that is in a p2p session with another registered client
    // we will just proxy the response
    if (linkedB2BUASession != null) {
	    if(logger.isInfoEnabled()) {
	        logger.info(String.format("B2BUA: Got response: \n %s", response));
	    }
	    // container handles CANCEL related responses no need to forward them
	    if(response.getStatus() == 487 || (response.getStatus()==200 && response.getMethod().equalsIgnoreCase("CANCEL"))) {
	    	if(logger.isDebugEnabled()) {
	    		logger.debug("response to CANCEL not forwarding");
	    	}
	    	return;
	   	}
	    // forward the response
	    response.getSession().setAttribute(B2BUA_LAST_RESPONSE, response);
	    SipServletRequest request = (SipServletRequest) getLinkedSession(response).getAttribute(B2BUA_LAST_REQUEST);	    
		SipServletResponse resp = request.createResponse(response.getStatus());
		if(response.getContent() != null) {
			resp.setContent(response.getContent(), response.getContentType());
		}
		resp.send();    
    } else {
    	// otherwise the response is coming back to a Voice app hosted by Restcomm
	    final ActorRef call = (ActorRef)application.getAttribute(Call.class.getName());
	    call.tell(response, self);
    }
  }

  private SipSession getLinkedSession(SipServletMessage message) {
	  return (SipSession)message.getSession().getAttribute(LINKED_SESSION);
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
