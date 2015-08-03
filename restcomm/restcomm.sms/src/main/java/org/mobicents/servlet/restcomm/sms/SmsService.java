/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.sms;

import static javax.servlet.sip.SipServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Currency;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.SmsMessage;
import org.mobicents.servlet.restcomm.entities.SmsMessage.Direction;
import org.mobicents.servlet.restcomm.entities.SmsMessage.Status;
import org.mobicents.servlet.restcomm.interpreter.SmsInterpreterBuilder;
import org.mobicents.servlet.restcomm.interpreter.StartInterpreter;
import org.mobicents.servlet.restcomm.smpp.SmppClientOpsThread;
import org.mobicents.servlet.restcomm.smpp.SmppSessionOutbound;
import org.mobicents.servlet.restcomm.telephony.util.B2BUAHelper;
import org.mobicents.servlet.restcomm.telephony.util.CallControlHelper;
import org.mobicents.servlet.restcomm.util.UriUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.cloudhopper.smpp.SmppSession;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
public final class SmsService extends UntypedActor {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final ActorSystem system;
    private final Configuration configuration;
    private boolean authenticateUsers = true;
    private final ServletConfig servletConfig;
    private final SipFactory sipFactory;
    private final DaoManager storage;
    private final ServletContext servletContext;
    static final int ERROR_NOTIFICATION = 0;
    static final int WARNING_NOTIFICATION = 1;

    // configurable switch whether to use the To field in a SIP header to determine the callee address
    // alternatively the Request URI can be used
    private boolean useTo = true;

    public SmsService(final ActorSystem system, final Configuration configuration, final SipFactory factory,
            final DaoManager storage, final ServletContext servletContext) {
        super();
        this.system = system;
        this.configuration = configuration;
        final Configuration runtime = configuration.subset("runtime-settings");
        this.authenticateUsers = runtime.getBoolean("authenticate");
        this.servletConfig = (ServletConfig) configuration.getProperty(ServletConfig.class.getName());
        this.sipFactory = factory;
        this.storage = storage;
        this.servletContext = servletContext;
        // final Configuration runtime = configuration.subset("runtime-settings");
        // TODO this.useTo = runtime.getBoolean("use-to");
    }

    private ActorRef sendToSMPPService() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new  SmppSessionOutbound(); //.sendSmsFromRestcommToSmpp();
            }
        }));
    }

    private void message(final Object message) throws IOException {
        final ActorRef self = self();
        final SipServletRequest request = (SipServletRequest) message;

        final SipURI fromURI = (SipURI) request.getFrom().getURI();
        final String fromUser = fromURI.getUser();
        final ClientsDao clients = storage.getClientsDao();
        final Client client = clients.getClient(fromUser);
        final AccountsDao accounts = storage.getAccountsDao();
        final ApplicationsDao applications = storage.getApplicationsDao();


        /// used to direct message to SMPP endpoint
        final SipURI getUri = (SipURI) request.getRequestURI();
        final String to = getUri.getUser();
        if (to.equals("smpp") ){
            logger.error("Sending message to SMPP endpoint - to: " + to );
            ActorRef smppServiceSend =  sendToSMPPService();
            ActorRef session = session();

            //get SMPP session
            SmppSession smppSession = SmppClientOpsThread.getSmppSessionForOutbound();
            //make sure smpp session is bound

            if (smppSession.isBound() && smppSession != null){
                //accept sms and forward to SMPPSessionOutbound
                logger.error("There is an Smpp Session running, message will be accepted");
                final SipServletResponse messageAccepted = request.createResponse(SipServletResponse.SC_ACCEPTED);
                messageAccepted.send();
                //send message to SmppSessionOutbound
                smppServiceSend.tell(request, null);
            }

            //store message request in db
            recordSmppMessageInDB (clients,  client,  request , to, session );
            return;
        }




        // Make sure we force clients to authenticate.
        if (client != null) {
            // Make sure we force clients to authenticate.
            if (authenticateUsers // https://github.com/Mobicents/RestComm/issues/29 Allow disabling of SIP authentication
                    && !CallControlHelper.checkAuthentication(request, storage)) {
                logger.info("Client " + client.getLogin() + " failed to authenticate");
                // Since the client failed to authenticate, we will ignore the message and not process further
                return;
            }
        }
        // TODO Enforce some kind of security check for requests coming from outside SIP UAs such as ITSPs that are not
        // registered
        final String toUser = CallControlHelper.getUserSipId(request, useTo);
        // Try to see if the request is destined for an application we are hosting.
        if (redirectToHostedSmsApp(self, request, accounts, applications, toUser)) {
            // Tell the sender we received the message okay.
            logger.info("Message to :" + toUser + " matched to one of the hosted applications");
            final SipServletResponse messageAccepted = request.createResponse(SipServletResponse.SC_ACCEPTED);
            messageAccepted.send();
            return;
        }
        if (client != null) {
            // try to see if the request is destined to another registered client
            // if (client != null) { // make sure the caller is a registered client and not some external SIP agent that we
            // have little control over
            Client toClient = clients.getClient(toUser);
            if (toClient != null) { // looks like its a p2p attempt between two valid registered clients, lets redirect
                // to the b2bua
                if (B2BUAHelper.redirectToB2BUA(request, client, toClient, storage, sipFactory)) {
                    // if all goes well with proxying the SIP MESSAGE on to the target client
                    // then we can end further processing of this request
                    logger.info("P2P, Message from: " + client.getLogin() + " redirected to registered client: "
                            + toClient.getLogin());
                    return;
                }
            } else {
            // Since toUser is null, try to route the message outside using the SMS Aggregator
            logger.info("Restcomm will route this SMS to an external aggregator: " + client.getLogin() + " to: " + toUser);
            ActorRef session = session();
            // Create an SMS detail record.
            final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
            final SmsMessage.Builder builder = SmsMessage.builder();
            builder.setSid(sid);
            builder.setAccountSid(client.getAccountSid());
            builder.setApiVersion(client.getApiVersion());
            builder.setRecipient(toUser);
            builder.setSender(client.getLogin());
            builder.setBody(new String(request.getRawContent()));
            builder.setDirection(Direction.OUTBOUND_CALL);
            builder.setStatus(Status.RECEIVED);
            builder.setPrice(new BigDecimal("0.00"));
            // TODO implement currency property to be read from Configuration
            builder.setPriceUnit(Currency.getInstance("USD"));
            final StringBuilder buffer = new StringBuilder();
            buffer.append("/").append(client.getApiVersion()).append("/Accounts/");
            buffer.append(client.getAccountSid().toString()).append("/SMS/Messages/");
            buffer.append(sid.toString());
            final URI uri = URI.create(buffer.toString());
            builder.setUri(uri);
            final SmsMessage record = builder.build();
            final SmsMessagesDao messages = storage.getSmsMessagesDao();
            messages.addSmsMessage(record);
            // Store the sms record in the sms session.
            session.tell(new SmsSessionAttribute("record", record), self());
            // Send the SMS.
            final SmsSessionRequest sms = new SmsSessionRequest(client.getLogin(), toUser, new String(request.getRawContent()),
                    null);
            session.tell(sms, self());
            }
        } else {
            final SipServletResponse response = request.createResponse(SC_NOT_FOUND);
            response.send();
            // We didn't find anyway to handle the SMS.
            String errMsg = "Restcomm cannot process this SMS because the destination number cannot be found. To: "+toUser;
            sendNotification(errMsg, 11005, "error", true);
        }
    }

    /**
     *
     * Try to locate a hosted sms app corresponding to the callee/To address. If one is found, begin execution, otherwise return
     * false;
     *
     * @param self
     * @param request
     * @param accounts
     * @param applications
     * @param id
     * @throws IOException
     */
    private boolean redirectToHostedSmsApp(final ActorRef self, final SipServletRequest request, final AccountsDao accounts,
            final ApplicationsDao applications, String id) throws IOException {
        boolean isFoundHostedApp = false;

        // Handle the SMS message.
        final SipURI uri = (SipURI) request.getRequestURI();
        final String to = uri.getUser();
        // Format the destination to an E.164 phone number.
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        String phone = to;
        try {
            phone = phoneNumberUtil.format(phoneNumberUtil.parse(to, "US"), PhoneNumberFormat.E164);
        } catch (Exception e) {}
        // Try to find an application defined for the phone number.
        final IncomingPhoneNumbersDao numbers = storage.getIncomingPhoneNumbersDao();
        IncomingPhoneNumber number = numbers.getIncomingPhoneNumber(phone);
        if (number == null) {
            number = numbers.getIncomingPhoneNumber(to);
        }
        try {
            if (number != null) {
                URI appUri = number.getSmsUrl();
                ActorRef interpreter = null;
                if (appUri != null) {
                    final SmsInterpreterBuilder builder = new SmsInterpreterBuilder(system);
                    builder.setSmsService(self);
                    builder.setConfiguration(configuration);
                    builder.setStorage(storage);
                    builder.setAccount(number.getAccountSid());
                    builder.setVersion(number.getApiVersion());
                    final Sid sid = number.getSmsApplicationSid();
                    if (sid != null) {
                        final Application application = applications.getApplication(sid);
                        builder.setUrl(UriUtils.resolve(request.getLocalAddr(), 8080, application.getSmsUrl()));
                        builder.setMethod(application.getSmsMethod());
                        builder.setFallbackUrl(UriUtils.resolve(request.getLocalAddr(), 8080, application.getSmsFallbackUrl()));
                        builder.setFallbackMethod(application.getSmsFallbackMethod());
                    } else {
                        builder.setUrl(UriUtils.resolve(request.getLocalAddr(), 8080, appUri));
                        builder.setMethod(number.getSmsMethod());
                        URI appFallbackUrl = number.getSmsFallbackUrl();
                        if (appFallbackUrl != null) {
                            builder.setFallbackUrl(UriUtils.resolve(request.getLocalAddr(), 8080, number.getSmsFallbackUrl()));
                            builder.setFallbackMethod(number.getSmsFallbackMethod());
                        }
                    }
                    interpreter = builder.build();
                }
                // else {
                // appUri = number.getVoiceUrl();
                // if (appUri != null) {
                // final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
                // builder.setConfiguration(configuration);
                // builder.setStorage(storage);
                // builder.setCallManager(self);
                // builder.setSmsService(self);
                // builder.setAccount(number.getAccountSid());
                // builder.setVersion(number.getApiVersion());
                // final Account account = accounts.getAccount(number.getAccountSid());
                // builder.setEmailAddress(account.getEmailAddress());
                // final Sid sid = number.getVoiceApplicationSid();
                // if (sid != null) {
                // final Application application = applications.getApplication(sid);
                // builder.setUrl(UriUtils.resolve(request.getLocalAddr(), 8080, application.getVoiceUrl()));
                // builder.setMethod(application.getVoiceMethod());
                // builder.setFallbackUrl(application.getVoiceFallbackUrl());
                // builder.setFallbackMethod(application.getVoiceFallbackMethod());
                // builder.setStatusCallback(application.getStatusCallback());
                // builder.setStatusCallbackMethod(application.getStatusCallbackMethod());
                // } else {
                // builder.setUrl(UriUtils.resolve(request.getLocalAddr(), 8080, number.getVoiceUrl()));
                // builder.setMethod(number.getVoiceMethod());
                // builder.setFallbackUrl(number.getVoiceFallbackUrl());
                // builder.setFallbackMethod(number.getVoiceFallbackMethod());
                // builder.setStatusCallback(number.getStatusCallback());
                // builder.setStatusCallbackMethod(number.getStatusCallbackMethod());
                // }
                // interpreter = builder.build();
                // }
                // }
                final ActorRef session = session();
                session.tell(request, self);
                final StartInterpreter start = new StartInterpreter(session);
                interpreter.tell(start, self);
                isFoundHostedApp = true;
            }
        } catch (Exception e) {
            String errMsg = "There is no valid Restcomm SMS Request URL configured for this number : " + phone;
            sendNotification(errMsg, 12003, "error", true);
        }
        return isFoundHostedApp;
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final UntypedActorContext context = getContext();
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (CreateSmsSession.class.equals(klass)) {
            final ActorRef session = session();
            final SmsServiceResponse<ActorRef> response = new SmsServiceResponse<ActorRef>(session);
            sender.tell(response, self);
        } else if (DestroySmsSession.class.equals(klass)) {
            final DestroySmsSession request = (DestroySmsSession) message;
            final ActorRef session = request.session();
            context.stop(session);
        } else if (message instanceof SipServletRequest) {
            final SipServletRequest request = (SipServletRequest) message;
            final String method = request.getMethod();
            if ("MESSAGE".equalsIgnoreCase(method)) {
                message(message);
            }
        } else if (message instanceof SipServletResponse) {
            final SipServletResponse response = (SipServletResponse) message;
            final SipServletRequest request = response.getRequest();
            final String method = request.getMethod();
            if ("MESSAGE".equalsIgnoreCase(method)) {
                response(message);
            }
        }
    }

    private void response(final Object message) throws Exception {
        final ActorRef self = self();
        final SipServletResponse response = (SipServletResponse) message;
        // https://bitbucket.org/telestax/telscale-restcomm/issue/144/send-p2p-chat-works-but-gives-npe
        if (B2BUAHelper.isB2BUASession(response)) {
            B2BUAHelper.forwardResponse(response);
            return;
        }
        final SipApplicationSession application = response.getApplicationSession();
        final ActorRef session = (ActorRef) application.getAttribute(SmsSession.class.getName());
        session.tell(response, self);
    }

private void recordSmppMessageInDB ( ClientsDao clients, Client client, SipServletRequest request, String to, ActorRef session ) throws IOException{
    // Create an SMS detail record.

    final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
    final SmsMessage.Builder builder = SmsMessage.builder();
    builder.setSid(sid);
    builder.setAccountSid(client.getAccountSid());
    builder.setApiVersion(client.getApiVersion());
    builder.setRecipient(to);
    builder.setSender(client.getLogin());
    builder.setBody(new String(request.getRawContent()));
    builder.setDirection(Direction.OUTBOUND_CALL);
    builder.setStatus(Status.RECEIVED);
    builder.setPrice(new BigDecimal("0.00"));
    // TODO implement currency property to be read from Configuration
    builder.setPriceUnit(Currency.getInstance("USD"));
    final StringBuilder buffer = new StringBuilder();
    buffer.append("/").append(client.getApiVersion()).append("/Accounts/");
    buffer.append(client.getAccountSid().toString()).append("/SMS/Messages/");
    buffer.append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    builder.setUri(uri);
    final SmsMessage record = builder.build();
    final SmsMessagesDao messages = storage.getSmsMessagesDao();
    messages.addSmsMessage(record);
    session.tell(new SmsSessionAttribute("record", record), self());

}

    @SuppressWarnings("unchecked")
    private SipURI outboundInterface() {
        SipURI result = null;
        final List<SipURI> uris = (List<SipURI>) servletContext.getAttribute(SipServlet.OUTBOUND_INTERFACES);
        for (final SipURI uri : uris) {
            final String transport = uri.getTransportParam();
            if ("udp".equalsIgnoreCase(transport)) {
                result = uri;
            }
        }
        return result;
    }

    private ActorRef session() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                Configuration smsConfiguration = configuration.subset("sms-aggregator");
                return new SmsSession(smsConfiguration, sipFactory, outboundInterface(), storage);
            }
        }));
    }

    // used for sending warning and error logs to notification engine and to the console
    private void sendNotification(String errMessage, int errCode, String errType, boolean createNotification) {
        NotificationsDao notifications = storage.getNotificationsDao();
        Notification notification;

        if (errType == "warning") {
            logger.warning(errMessage); // send message to console
            if (createNotification) {
                notification = notification(WARNING_NOTIFICATION, errCode, errMessage);
                notifications.addNotification(notification);
            }
        } else if (errType == "error") {
            logger.error(errMessage); // send message to console
            if (createNotification) {
                notification = notification(ERROR_NOTIFICATION, errCode, errMessage);
                notifications.addNotification(notification);
            }
        } else if (errType == "info") {
            logger.info(errMessage); // send message to console
        }

    }

    private Notification notification(final int log, final int error, final String message) {
        String version = configuration.subset("runtime-settings").getString("api-version");
        Sid accountId = new Sid("ACae6e420f425248d6a26948c17a9e2acf");
//        Sid callSid = new Sid("CA00000000000000000000000000000000");
        final Notification.Builder builder = Notification.builder();
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        builder.setSid(sid);
        // builder.setAccountSid(accountId);
        builder.setAccountSid(accountId);
//        builder.setCallSid(callSid);
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
        /**
         * if (response != null) { builder.setRequestUrl(request.getUri()); builder.setRequestMethod(request.getMethod());
         * builder.setRequestVariables(request.getParametersAsString()); }
         **/

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

}
