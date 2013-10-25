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

import static akka.pattern.Patterns.ask;
import static javax.servlet.sip.SipServletResponse.SC_BAD_REQUEST;
import static javax.servlet.sip.SipServletResponse.SC_NOT_FOUND;
import static javax.servlet.sip.SipServletResponse.SC_OK;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipFactory;
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
import org.mobicents.servlet.restcomm.interpreter.SubVoiceInterpreterBuilder;
import org.mobicents.servlet.restcomm.interpreter.VoiceInterpreterBuilder;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.telephony.util.B2BUAHelper;
import org.mobicents.servlet.restcomm.telephony.util.CallControlHelper;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author ivelin.ivanov@telestax.com
 * @author jean.deruelle@telestax.com
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
    final SipURI fromUri = (SipURI)request.getFrom().getURI();
    String fromUser = fromUri.getUser();
    final ClientsDao clients = storage.getClientsDao();
    final Client client = clients.getClient(fromUser);
    if(client != null) {
      // Make sure we force clients to authenticate.
      if (CallControlHelper.checkAuthentication(request, storage)) {
    	// if the client has authenticated, try to redirect to the Client VoiceURL app
    	// otherwise continue trying to process the Client invite
    	if (redirectToClientVoiceApp(self, request, accounts, applications, client)) {
    			return;
    	} // else continue trying other ways to handle the request 
      } else {
    	// Since the client failed to authenticate, we will take no further action at this time.
        return;
	  }
    } 
    // TODO Enforce some kind of security check for requests coming from outside SIP UAs such as ITSPs that are not registered  

    final String toUser = CallControlHelper.getUserSipId(request, useTo);
    // Try to see if the request is destined for an application we are hosting.
    if (redirectToHostedVoiceApp(self, request, accounts, applications, toUser)) {
    	return;
    // Next try to see if the request is destined to another registered client
    } else {
    	if (client != null) { // make sure the caller is a registered client and not some external SIP agent that we have little control over
    		Client toClient = clients.getClient(toUser);
    		if (toClient != null) { // looks like its a p2p attempt between two valid registered clients, lets redirect to the b2bua
                if (B2BUAHelper.redirectToB2BUA(request, client, toClient, storage, sipFactory)) {
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
 * 
 * Try to locate a hosted voice app corresponding to the callee/To address.
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
    } else if(UpdateCallScript.class.equals(klass)) {
    	try {
    		update(message);
    	} catch (final Exception exception) {
    		sender.tell(new CallManagerResponse<ActorRef>(exception), self);
    	}
      	
    } else if(DestroyCall.class.equals(klass)) {
      destroy(message);
    } else if(message instanceof SipServletResponse) {
      response(message);
    } else if(message instanceof SipApplicationSessionEvent) {
      timeout(message);
    } else if(GetCall.class.equals(klass)) {
      sender.tell(lookup(message), self);
    }
  }
  
  private void ack(SipServletRequest request) throws IOException {
      SipServletResponse response = B2BUAHelper.getLinkedResponse(request);
      // if this is an ACK that belongs to a B2BUA session, then we proxy it to the other client
      if (response != null) { 	  
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

@SuppressWarnings({ "rawtypes", "unchecked" })
private void update(final Object message) throws Exception {
	final UpdateCallScript request = (UpdateCallScript)message;
    final ActorRef self = self();
    final ActorRef call = request.call();
    
	final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
    Future<Object> future = (Future<Object>)ask(call, new GetCallObservers(), expires);
	CallResponse<List<ActorRef>> response = (CallResponse<List<ActorRef>>)Await.result(future, Duration.create(10, TimeUnit.SECONDS));
    List<ActorRef> callObservers = response.get();
    
    for (Iterator iterator = callObservers.iterator(); iterator.hasNext();) {
		ActorRef interpreter = (ActorRef) iterator.next();
		call.tell(new StopObserving(interpreter), null);
		getContext().stop(interpreter);
	}
    
    final SubVoiceInterpreterBuilder builder = new SubVoiceInterpreterBuilder(system);
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
    builder.setHangupOnEnd(true);
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
        final Registration registration = registrations.getRegistration(request.to().replaceFirst("client:", ""));
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
    SipServletRequest originalRequest = B2BUAHelper.getLinkedRequest(request);
    if (originalRequest != null) {
	    if(logger.isInfoEnabled()) {
	        logger.info(String.format("B2BUA: Got CANCEL request: \n %s", request));
	    }
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
    SipSession linkedB2BUASession = B2BUAHelper.getLinkedSession(request);
    if (linkedB2BUASession != null) {
	    if(logger.isInfoEnabled()) {
	        logger.info(String.format("B2BUA: Got BYE request: \n %s", request));
	    }
	    request.getSession().setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, request);
	    SipServletRequest clonedBye = linkedB2BUASession.createRequest("BYE");
	    linkedB2BUASession.setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, clonedBye);
	    clonedBye.send();
    } else {
	    final ActorRef call = (ActorRef)application.getAttribute(Call.class.getName());
	    call.tell(request, self);
    }
  }  
  
  public void response(final Object message) throws UnsupportedEncodingException, IOException {
	final ActorRef self = self();
	final SipServletResponse response = (SipServletResponse)message;
    final SipApplicationSession application = response.getApplicationSession();
    
    // if this response is coming from a client that is in a p2p session with another registered client
    // we will just proxy the response
    if (B2BUAHelper.isB2BUASession(response)) {
    	B2BUAHelper.forwardResponse(response);    
    } else {
    	// otherwise the response is coming back to a Voice app hosted by Restcomm
	    final ActorRef call = (ActorRef)application.getAttribute(Call.class.getName());
	    call.tell(response, self);
    }
  }
  
  public ActorRef lookup(final Object message) {
	  final GetCall getCall = (GetCall)message;
	  final String callPath = getCall.callPath();
	  
	  final ActorContext context = getContext();

	  //The context.actorFor has been depreciated for actorSelection at the latest Akka release.
	  return context.actorFor(callPath);
  }
 
public void timeout(final Object message) {
	final ActorRef self = self();
	final SipApplicationSessionEvent event = (SipApplicationSessionEvent)message;
    final SipApplicationSession application = event.getApplicationSession();
    final ActorRef call = (ActorRef)application.getAttribute(Call.class.getName());
    final ReceiveTimeout timeout = ReceiveTimeout.getInstance();
    call.tell(timeout, self);
  }
}
