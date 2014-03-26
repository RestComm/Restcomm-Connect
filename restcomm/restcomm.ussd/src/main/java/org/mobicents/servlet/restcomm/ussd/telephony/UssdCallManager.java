/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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
package org.mobicents.servlet.restcomm.ussd.telephony;

import static javax.servlet.sip.SipServletResponse.SC_BAD_REQUEST;
import static javax.servlet.sip.SipServletResponse.SC_NOT_FOUND;
import static javax.servlet.sip.SipServletResponse.SC_OK;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.interpreter.StartInterpreter;
import org.mobicents.servlet.restcomm.telephony.util.B2BUAHelper;
import org.mobicents.servlet.restcomm.telephony.util.CallControlHelper;
import org.mobicents.servlet.restcomm.ussd.interpreter.UssdInterpreter;
import org.mobicents.servlet.restcomm.ussd.interpreter.UssdInterpreterBuilder;
import org.mobicents.servlet.restcomm.util.UriUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
public class UssdCallManager extends UntypedActor {

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

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    /**
     * @param configuration
     * @param context
     * @param system
     * @param gateway
     * @param conferences
     * @param sms
     * @param factory
     * @param storage
     */
    public UssdCallManager(Configuration configuration, ServletContext context, ActorSystem system, ActorRef gateway,
            ActorRef conferences, ActorRef sms, SipFactory factory, DaoManager storage) {
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
    }

    private ActorRef ussdCall() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new UssdCall(sipFactory, gateway);
            }
        }));
    }

    private void check(final Object message) throws IOException {
        final SipServletRequest request = (SipServletRequest) message;
        String rawContent = new String(request.getRawContent());
        if (request.getContentLength() == 0) {
            String contentType = request.getContentType();
            if (!("application/vnd.3gpp.ussd+xml".equals(contentType))) {
                final SipServletResponse response = request.createResponse(SC_BAD_REQUEST);
                response.send();
            }
        }
    }

    @Override
    public void onReceive(final Object message) throws Exception {

        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (message instanceof SipServletRequest) {
            final SipServletRequest request = (SipServletRequest) message;
            final String method = request.getMethod();
            if ("INVITE".equalsIgnoreCase(method)) {
                check(request);
                invite(request);
            } else if ("INFO".equalsIgnoreCase(method)) {
                processInfo(request);
            } else if ("ACK".equals(method)) {
                ack(request);
            }
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
    }

    private void invite(final Object message) throws Exception {
        final ActorRef self = self();
        final SipServletRequest request = (SipServletRequest) message;
        // Make sure we handle re-invites properly.
        if (!request.isInitial()) {
            final SipServletResponse okay = request.createResponse(SC_OK);
            okay.send();
            return;
        }

        final AccountsDao accounts = storage.getAccountsDao();
        final ApplicationsDao applications = storage.getApplicationsDao();
        final String toUser = CallControlHelper.getUserSipId(request, useTo);
        if (redirectToHostedVoiceApp(self, request, accounts, applications, toUser)){
            return;
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
     * @param id
     * @throws Exception
     */
    private boolean redirectToHostedVoiceApp(final ActorRef self, final SipServletRequest request, final AccountsDao accounts,
            final ApplicationsDao applications, String id) throws Exception {
        boolean isFoundHostedApp = false;

        final IncomingPhoneNumbersDao numbersDao = storage.getIncomingPhoneNumbersDao();
        IncomingPhoneNumber number = null;

        if (request.getContentType().equals("application/vnd.3gpp.ussd+xml")) {
            // This is a USSD Invite
            number = numbersDao.getIncomingPhoneNumber(id);
            if (number != null) {
                final UssdInterpreterBuilder builder = new UssdInterpreterBuilder(system);
                builder.setConfiguration(configuration);
                builder.setStorage(storage);
                builder.setCallManager(self);
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
                final ActorRef ussdInterpreter = builder.build();
                final ActorRef ussdCall = ussdCall();

                ussdInterpreter.tell(new StartInterpreter(ussdCall), self);
                ussdCall.tell(request, self);


                SipApplicationSession applicationSession = request.getApplicationSession();
                applicationSession.setAttribute("UssdCall","true");
                applicationSession.setAttribute(UssdInterpreter.class.getName(), ussdInterpreter);
                applicationSession.setAttribute(UssdCall.class.getName(), ussdCall);
                isFoundHostedApp = true;
            } else {
                throw new Exception("Number not found");
            }
        }
        return isFoundHostedApp;
    }

    private void processInfo(SipServletRequest request) throws IOException {
        final ActorRef ussdInterpreter = (ActorRef) request.getApplicationSession().getAttribute(UssdInterpreter.class.getName());
        if(ussdInterpreter != null) {
            ussdInterpreter.tell(request, self());
        } else {
            final SipServletResponse notFound = request.createResponse(SipServletResponse.SC_NOT_FOUND);
            notFound.send();
        }
    }

}
