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
import static javax.servlet.sip.SipServlet.OUTBOUND_INTERFACES;
import static javax.servlet.sip.SipServletResponse.SC_BAD_REQUEST;
import static javax.servlet.sip.SipServletResponse.SC_NOT_FOUND;
import static javax.servlet.sip.SipServletResponse.SC_OK;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Registration;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.interpreter.StartInterpreter;
import org.mobicents.servlet.restcomm.interpreter.VoiceInterpreterBuilder;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.telephony.util.B2BUAHelper;
import org.mobicents.servlet.restcomm.telephony.util.CallControlHelper;
import org.mobicents.servlet.restcomm.util.UriUtils;

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

    static final int ERROR_NOTIFICATION = 0;
    static final int WARNING_NOTIFICATION = 1;
    static final Pattern PATTERN = Pattern.compile("[\\*#0-9]{1,12}");
    static final String EMAIL_SENDER = "restcomm@restcomm.org";
    static final String EMAIL_SUBJECT = "RestComm Error Notification - Attention Required";

    private final ActorSystem system;
    private final Configuration configuration;
    private final ServletContext context;
    private final ActorRef conferences;
    private final ActorRef gateway;
    private final ActorRef sms;
    private final SipFactory sipFactory;
    private final DaoManager storage;

    // configurable switch whether to use the To field in a SIP header to determine the callee address
    // alternatively the Request URI can be used
    private boolean useTo;

    private AtomicInteger numberOfFailedCalls;
    private AtomicBoolean useFallbackProxy;
    private boolean allowFallback;
    private boolean allowFallbackToPrimary;
    private int maxNumberOfFailedCalls;

    private String primaryProxyUri;
    private String primaryProxyUsername, primaryProxyPassword;
    private String fallBackProxyUri;
    private String fallBackProxyUsername, fallBackProxyPassword;
    private String activeProxy;
    private String activeProxyUsername, activeProxyPassword;

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private CreateCall createCallRequest;
    private SwitchProxy switchProxyRequest;

    public CallManager(final Configuration configuration, final ServletContext context, final ActorSystem system,
            final ActorRef gateway, final ActorRef conferences, final ActorRef sms, final SipFactory factory,
            final DaoManager storage) {
        super();
        this.system = system;
        this.configuration = configuration;
        this.context = context;
        this.gateway = gateway;
        this.conferences = conferences;
        this.sms = sms;
        this.sipFactory = factory;
        this.storage = storage;
        final Configuration runtime = configuration.subset("runtime-settings");
        final Configuration outboundProxyConfig = runtime.subset("outbound-proxy");
        this.useTo = runtime.getBoolean("use-to");

        this.primaryProxyUri = outboundProxyConfig.getString("outbound-proxy-uri");
        this.primaryProxyUsername = outboundProxyConfig.getString("outbound-proxy-user");
        this.primaryProxyPassword = outboundProxyConfig.getString("outbound-proxy-password");

        this.fallBackProxyUri = outboundProxyConfig.getString("fallback-outbound-proxy-uri");
        this.fallBackProxyUsername = outboundProxyConfig.getString("fallback-outbound-proxy-user");
        this.fallBackProxyPassword = outboundProxyConfig.getString("fallback-outbound-proxy-password");

        this.activeProxy = primaryProxyUri;
        this.activeProxyUsername = primaryProxyUsername;
        this.activeProxyPassword = primaryProxyPassword;

        numberOfFailedCalls = new AtomicInteger();
        numberOfFailedCalls.set(0);
        useFallbackProxy = new AtomicBoolean();
        useFallbackProxy.set(false);

        Boolean bool = outboundProxyConfig.getBoolean("allow-fallback");
        if (bool != null) {
            allowFallback = bool;
        } else {
            allowFallback = false;
        }

        final Integer value = outboundProxyConfig.getInt("max-failed-calls");
        if (value != null) {
            maxNumberOfFailedCalls = value.intValue();
        } else {
            maxNumberOfFailedCalls = 20;
        }

        bool = outboundProxyConfig.getBoolean("allow-fallback-to-primary");
        if (bool != null) {
            allowFallbackToPrimary = bool;
        } else {
            allowFallbackToPrimary = false;
        }
    }

    private ActorRef call() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Call(sipFactory, gateway);
            }
        }));
    }

    private void check(final Object message) throws IOException {
        final SipServletRequest request = (SipServletRequest) message;
        String content = new String(request.getRawContent());
        if (request.getContentLength() == 0
                || !("application/sdp".equals(request.getContentType()) || content.contains("application/sdp"))) {
            final SipServletResponse response = request.createResponse(SC_BAD_REQUEST);
            response.send();
        }
    }

    private void destroy(final Object message) {
        final UntypedActorContext context = getContext();
        final DestroyCall request = (DestroyCall) message;
        context.stop(request.call());
    }

    private void invite(final Object message) throws IOException, NumberParseException {
        final ActorRef self = self();
        final SipServletRequest request = (SipServletRequest) message;
        // Make sure we handle re-invites properly.
        if (!request.isInitial()) {
            final SipServletResponse okay = request.createResponse(SC_OK);
            okay.send();
            return;
        }
        // If it's a new invite lets try to handle it.
        final AccountsDao accounts = storage.getAccountsDao();
        final ApplicationsDao applications = storage.getApplicationsDao();
        // Try to find an application defined for the client.
        final SipURI fromUri = (SipURI) request.getFrom().getURI();
        String fromUser = fromUri.getUser();
        final ClientsDao clients = storage.getClientsDao();
        final Client client = clients.getClient(fromUser);
        if (client != null) {
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
        // TODO Enforce some kind of security check for requests coming from outside SIP UAs such as ITSPs that are not
        // registered

        final String toUser = CallControlHelper.getUserSipId(request, useTo);
        // Try to see if the request is destined for an application we are hosting.
        if (redirectToHostedVoiceApp(self, request, accounts, applications, toUser)) {
            return;
            // Next try to see if the request is destined to another registered client
        } else {
            if (client != null) { // make sure the caller is a registered client and not some external SIP agent that we have
                // little control over
                Client toClient = clients.getClient(toUser);
                if (toClient != null) { // looks like its a p2p attempt between two valid registered clients, lets redirect to
                    // the b2bua
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
     * Try to locate a hosted voice app corresponding to the callee/To address. If one is found, begin execution, otherwise
     * return false;
     *
     * @param self
     * @param request
     * @param accounts
     * @param applications
     * @param phone
     */
    private boolean redirectToHostedVoiceApp(final ActorRef self, final SipServletRequest request, final AccountsDao accounts,
            final ApplicationsDao applications, String phone) {
        boolean isFoundHostedApp = false;
        // Format the destination to an E.164 phone number.
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        String formatedPhone = null;
        try {
            formatedPhone = phoneNumberUtil.format(phoneNumberUtil.parse(phone, "US"), PhoneNumberFormat.E164);
        } catch (Exception e) {}
        try {
            // Try to find an application defined for the phone number.
            final IncomingPhoneNumbersDao numbers = storage.getIncomingPhoneNumbersDao();
            IncomingPhoneNumber number = numbers.getIncomingPhoneNumber(formatedPhone);
            if (number == null) {
                number = numbers.getIncomingPhoneNumber(phone);
            }
            if (number != null) {
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
                if (sid != null) {
                    final Application application = applications.getApplication(sid);
                    builder.setUrl(UriUtils.resolve(request.getLocalAddr(), 8080, application.getVoiceUrl()));
                    builder.setMethod(application.getVoiceMethod());
                    builder.setFallbackUrl(application.getVoiceFallbackUrl());
                    builder.setFallbackMethod(application.getVoiceFallbackMethod());
                    builder.setStatusCallback(application.getStatusCallback());
                    builder.setStatusCallbackMethod(application.getStatusCallbackMethod());
                } else {
                    builder.setUrl(UriUtils.resolve(request.getLocalAddr(), 8080, number.getVoiceUrl()));
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
        } catch (Exception notANumber) {
            isFoundHostedApp = false;
        }
        return isFoundHostedApp;
    }

    /**
     * If there is VoiceUrl provided for a Client configuration, try to begin execution of the RCML app, otherwise return false.
     *
     * @param self
     * @param request
     * @param accounts
     * @param applications
     * @param client
     */
    private boolean redirectToClientVoiceApp(final ActorRef self, final SipServletRequest request, final AccountsDao accounts,
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
            if (sid != null) {
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
        final SipServletRequest request = (SipServletRequest) message;
        final SipServletResponse response = request.createResponse(SC_OK);
        response.send();
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (message instanceof SipServletRequest) {
            final SipServletRequest request = (SipServletRequest) message;
            final String method = request.getMethod();
            if ("INVITE".equals(method)) {
                check(request);
                invite(request);
            } else if ("OPTIONS".equals(method)) {
                pong(request);
            } else if ("ACK".equals(method)) {
                ack(request);
            } else if ("CANCEL".equals(method)) {
                cancel(request);
            } else if ("BYE".equals(method)) {
                bye(request);
            }
        } else if (CreateCall.class.equals(klass)) {
            try {
                this.createCallRequest = (CreateCall) message;
                sender.tell(new CallManagerResponse<ActorRef>(outbound(message)), self);
            } catch (final Exception exception) {
                sender.tell(new CallManagerResponse<ActorRef>(exception), self);
            }
        } else if (ExecuteCallScript.class.equals(klass)) {
            execute(message);
        } else if (UpdateCallScript.class.equals(klass)) {
            try {
                update(message);
            } catch (final Exception exception) {
                sender.tell(new CallManagerResponse<ActorRef>(exception), self);
            }

        } else if (DestroyCall.class.equals(klass)) {
            destroy(message);
        } else if (message instanceof SipServletResponse) {
            response(message);
        } else if (message instanceof SipApplicationSessionEvent) {
            timeout(message);
        } else if (GetCall.class.equals(klass)) {
            sender.tell(lookup(message), self);
        } else if (GetActiveProxy.class.equals(klass)) {
            sender.tell(getActiveProxy(), self);
        } else if (SwitchProxy.class.equals(klass)) {
            this.switchProxyRequest = (SwitchProxy) message;
            sender.tell(switchProxy(), self);
        } else if (GetProxies.class.equals(klass)) {
            sender.tell(getProxies(message), self);
        }
    }

    private void ack(SipServletRequest request) throws IOException {
        SipServletResponse response = B2BUAHelper.getLinkedResponse(request);
        // if this is an ACK that belongs to a B2BUA session, then we proxy it to the other client
        if (response != null) {
            SipServletRequest ack = response.createAck();
            // Issue #307: https://telestax.atlassian.net/browse/RESTCOMM-307
            SipURI toInetUri = (SipURI) request.getSession().getAttribute("toInetUri");
            if (toInetUri != null) {
                logger.info("Using the real ip address of the sip client " + toInetUri.toString()
                        + " as a request uri of the ACK request");
                ack.setRequestURI(toInetUri);
            }
            ack.send();
            SipApplicationSession sipApplicationSession = request.getApplicationSession();
            // Defaulting the sip application session to 1h
            sipApplicationSession.setExpires(60);
        }
        //        else {
        //            SipSession sipSession = request.getSession();
        //            SipApplicationSession sipAppSession = request.getApplicationSession();
        //            if(sipSession.getInvalidateWhenReady()){
        //                logger.info("Invalidating sipSession: "+sipSession.getId());
        //                sipSession.invalidate();
        //            }
        //            if(sipAppSession.getInvalidateWhenReady()){
        //                logger.info("Invalidating sipAppSession: "+sipAppSession.getId());
        //                sipAppSession.invalidate();
        //            }
        //        }
    }

    private void execute(final Object message) {
        final ExecuteCallScript request = (ExecuteCallScript) message;
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
        final UpdateCallScript request = (UpdateCallScript) message;
        final ActorRef self = self();
        final ActorRef call = request.call();

        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        Future<Object> future = (Future<Object>) ask(call, new GetCallObservers(), expires);
        CallResponse<List<ActorRef>> response = (CallResponse<List<ActorRef>>) Await.result(future,
                Duration.create(10, TimeUnit.SECONDS));
        List<ActorRef> callObservers = response.get();

        for (Iterator iterator = callObservers.iterator(); iterator.hasNext();) {
            ActorRef existingInterpreter = (ActorRef) iterator.next();
            call.tell(new StopObserving(existingInterpreter), null);
            getContext().stop(existingInterpreter);
        }

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
        final CreateCall request = (CreateCall) message;
        final Configuration runtime = configuration.subset("runtime-settings");
        // final String uri = runtime.getString("outbound-proxy-uri");
        final String uri = activeProxy;
        final String proxyUsername = (request.username() != null) ? request.username() : activeProxyUsername;
        final String proxyPassword = (request.password() != null) ? request.password() : activeProxyPassword;
        SipURI from = null;
        SipURI to = null;
        switch (request.type()) {
            case CLIENT: {
                from = outboundInterface("udp");
                final RegistrationsDao registrations = storage.getRegistrationsDao();
                final Registration registration = registrations.getRegistration(request.to().replaceFirst("client:", ""));
                if (registration != null) {
                    final String location = registration.getLocation();
                    to = (SipURI) sipFactory.createURI(location);
                } else {
                    throw new NullPointerException(request.to() + " is not currently registered.");
                }
                break;
            }
            case PSTN: {
                from = sipFactory.createSipURI(request.from(), uri);
                to = sipFactory.createSipURI(request.to(), uri);
                break;
            }
            case SIP: {
                to = (SipURI) sipFactory.createURI(request.to());
                String transport = (to.getTransportParam() != null) ? to.getTransportParam() : "udp";
                SipURI outboundIntf = outboundInterface(transport);
                if (request.from() == null) {
                    from = outboundInterface(transport);
                } else {
                    from = sipFactory.createSipURI(request.from(), outboundIntf.getHost()+":"+outboundIntf.getPort());
                }
                break;
            }
        }
        final ActorRef call = call();
        final ActorRef self = self();
        final InitializeOutbound init = new InitializeOutbound(null, from, to, proxyUsername, proxyPassword, request.timeout(),
                request.isFromApi(), runtime.getString("api-version"), request.accountId(), request.type(), storage);
        call.tell(init, self);
        return call;
    }

    public void cancel(final Object message) throws IOException {
        final ActorRef self = self();
        final SipServletRequest request = (SipServletRequest) message;
        final SipApplicationSession application = request.getApplicationSession();

        // if this response is coming from a client that is in a p2p session with another registered client
        // we will just proxy the response
        SipServletRequest originalRequest = B2BUAHelper.getLinkedRequest(request);
        if (originalRequest != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("B2BUA: Got CANCEL request: \n %s", request));
            }
            originalRequest.createCancel().send();
        } else {
            final ActorRef call = (ActorRef) application.getAttribute(Call.class.getName());
            call.tell(request, self);
        }
    }

    public void bye(final Object message) throws IOException {
        final ActorRef self = self();
        final SipServletRequest request = (SipServletRequest) message;
        final SipApplicationSession application = request.getApplicationSession();

        // if this response is coming from a client that is in a p2p session with another registered client
        // we will just proxy the response
        SipSession linkedB2BUASession = B2BUAHelper.getLinkedSession(request);
        if (linkedB2BUASession != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("B2BUA: Got BYE request: \n %s", request));
            }
            request.getSession().setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, request);
            SipServletRequest clonedBye = linkedB2BUASession.createRequest("BYE");
            linkedB2BUASession.setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, clonedBye);

            // Issue #307: https://telestax.atlassian.net/browse/RESTCOMM-307
            SipURI toInetUri = (SipURI) request.getSession().getAttribute("toInetUri");
            SipURI fromInetUri = (SipURI) request.getSession().getAttribute("fromInetUri");
            if (toInetUri != null) {
                logger.info("Using the real ip address of the sip client " + toInetUri.toString()
                        + " as a request uri of the CloneBye request");
                clonedBye.setRequestURI(toInetUri);
            } else if (fromInetUri != null) {
                logger.info("Using the real ip address of the sip client " + fromInetUri.toString()
                        + " as a request uri of the CloneBye request");
                clonedBye.setRequestURI(fromInetUri);
            }

            clonedBye.send();
        } else {
            final ActorRef call = (ActorRef) application.getAttribute(Call.class.getName());
            call.tell(request, self);
        }
    }

    public void response(final Object message) throws IOException {
        final ActorRef self = self();
        final SipServletResponse response = (SipServletResponse) message;

        // If Allow-Falback is true, check for error reponses and switch proxy if needed
        if (allowFallback)
            checkErrorResponse(response);

        final SipApplicationSession application = response.getApplicationSession();

        // if this response is coming from a client that is in a p2p session with another registered client
        // we will just proxy the response
        if (B2BUAHelper.isB2BUASession(response)) {
            B2BUAHelper.forwardResponse(response);
        } else {
            if (application.isValid()) {
                // otherwise the response is coming back to a Voice app hosted by Restcomm
                final ActorRef call = (ActorRef) application.getAttribute(Call.class.getName());
                call.tell(response, self);
            }
        }
    }

    public ActorRef lookup(final Object message) {
        final GetCall getCall = (GetCall) message;
        final String callPath = getCall.callPath();

        final ActorContext context = getContext();

        //TODO: The context.actorFor has been depreciated for actorSelection at the latest Akka release.
        return context.actorFor(callPath);
    }

    public void timeout(final Object message) {
        final ActorRef self = self();
        final SipApplicationSessionEvent event = (SipApplicationSessionEvent) message;
        final SipApplicationSession application = event.getApplicationSession();
        final ActorRef call = (ActorRef) application.getAttribute(Call.class.getName());
        final ReceiveTimeout timeout = ReceiveTimeout.getInstance();
        call.tell(timeout, self);
    }

    public void checkErrorResponse(SipServletResponse response) {
        // Response should not be a proxy branch response and request should be initial and INVITE
        if (!response.isBranchResponse()
                && (response.getRequest().getMethod().equalsIgnoreCase("INVITE") && response.getRequest().isInitial())) {

            final int status = response.getStatus();
            // Response status should be > 400 BUT NOT 401, 404, 407
            if (status != SipServletResponse.SC_UNAUTHORIZED && status != SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED
                    && status != SipServletResponse.SC_NOT_FOUND && status > 400) {

                int failures = numberOfFailedCalls.incrementAndGet();
                logger.info("A total number of " + failures + " failures have now been counted.");

                if (failures >= maxNumberOfFailedCalls) {
                    logger.info("Max number of failed calls has been reached trying to switch over proxy.");
                    logger.info("Current proxy: " + getActiveProxy().get("ActiveProxy"));
                    switchProxy();
                    logger.info("Switched to proxy: " + getActiveProxy().get("ActiveProxy"));
                    numberOfFailedCalls.set(0);
                }
            }
        }
    }

    public Map<String, String> getActiveProxy() {
        Map<String, String> activeProxyMap = new ConcurrentHashMap<String, String>();
        activeProxyMap.put("ActiveProxy", this.activeProxy);
        return activeProxyMap;
    }

    public Map<String, String> switchProxy() {
        if (activeProxy.equalsIgnoreCase(primaryProxyUri)) {
            activeProxy = fallBackProxyUri;
            activeProxyUsername = fallBackProxyUsername;
            activeProxyPassword = fallBackProxyPassword;
            useFallbackProxy.set(true);
        } else if (allowFallbackToPrimary) {
            activeProxy = primaryProxyUri;
            activeProxyUsername = primaryProxyUsername;
            activeProxyPassword = primaryProxyPassword;
            useFallbackProxy.set(false);
        }
        final Notification notification = notification(WARNING_NOTIFICATION, 14110,
                "Max number of failed calls has been reached! Outbound proxy switched");
        final NotificationsDao notifications = storage.getNotificationsDao();
        notifications.addNotification(notification);
        return getActiveProxy();
    }

    public Map<String, String> getProxies(final Object message) {
        Map<String, String> proxies = new ConcurrentHashMap<String, String>();

        proxies.put("ActiveProxy", activeProxy);
        proxies.put("UsingFallBackProxy", useFallbackProxy.toString());
        proxies.put("AllowFallbackToPrimary", String.valueOf(allowFallbackToPrimary));
        proxies.put("PrimaryProxy", primaryProxyUri);
        proxies.put("FallbackProxy", fallBackProxyUri);

        return proxies;
    }

    private Notification notification(final int log, final int error, final String message) {
        String version = configuration.subset("runtime-settings").getString("api-version");
        Sid accountId = null;
        if (createCallRequest != null) {
            accountId = createCallRequest.accountId();
        } else if (switchProxyRequest != null) {
            accountId = switchProxyRequest.getSid();
        }
        final Notification.Builder builder = Notification.builder();
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        builder.setSid(sid);
        builder.setAccountSid(accountId);
        builder.setApiVersion(version);
        builder.setLog(log);
        builder.setErrorCode(error);
        final String base = configuration.subset("runtime-settings").getString("error-dictionary-uri");
        StringBuilder buffer = new StringBuilder();
        buffer.append(base);
        if (!base.endsWith("/")) {
            buffer.append("/");
        }
        buffer.append(error).append(".html");
        final URI info = URI.create(buffer.toString());
        builder.setMoreInfo(info);
        builder.setMessageText(message);
        final DateTime now = DateTime.now();
        builder.setMessageDate(now);
        try {
            builder.setRequestUrl(new URI(""));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        builder.setRequestMethod("");
        builder.setRequestVariables("");
        buffer = new StringBuilder();
        buffer.append("/").append(version).append("/Accounts/");
        buffer.append(accountId.toString()).append("/Notifications/");
        buffer.append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        builder.setUri(uri);
        return builder.build();
    }

    private SipURI outboundInterface(String transport) {
        SipURI result = null;
        @SuppressWarnings("unchecked")
        final List<SipURI> uris = (List<SipURI>) context.getAttribute(OUTBOUND_INTERFACES);
        for (final SipURI uri : uris) {
            final String interfaceTransport = uri.getTransportParam();
            if (transport.equalsIgnoreCase(interfaceTransport)) {
                result = uri;
            }
        }
        return result;
    }
}
