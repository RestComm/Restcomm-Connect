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
package org.restcomm.connect.sms;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.util.UriUtils;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.ApplicationsDao;
import org.restcomm.connect.dao.ClientsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.IncomingPhoneNumbersDao;
import org.restcomm.connect.dao.NotificationsDao;
import org.restcomm.connect.dao.SmsMessagesDao;
import org.restcomm.connect.dao.entities.Application;
import org.restcomm.connect.dao.entities.Client;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.Notification;
import org.restcomm.connect.dao.entities.SmsMessage;
import org.restcomm.connect.dao.entities.SmsMessage.Direction;
import org.restcomm.connect.dao.entities.SmsMessage.Status;
import org.restcomm.connect.extension.api.ExtensionType;
import org.restcomm.connect.extension.api.IExtensionCreateSmsSessionRequest;
import org.restcomm.connect.extension.api.RestcommExtensionException;
import org.restcomm.connect.extension.api.RestcommExtensionGeneric;
import org.restcomm.connect.extension.controller.ExtensionController;
import org.restcomm.connect.interpreter.SmsInterpreterBuilder;
import org.restcomm.connect.interpreter.StartInterpreter;
import org.restcomm.connect.monitoringservice.MonitoringService;
import org.restcomm.connect.sms.api.CreateSmsSession;
import org.restcomm.connect.sms.api.DestroySmsSession;
import org.restcomm.connect.sms.api.SmsServiceResponse;
import org.restcomm.connect.sms.api.SmsSessionAttribute;
import org.restcomm.connect.sms.api.SmsSessionRequest;
import org.restcomm.connect.telephony.api.TextMessage;
import org.restcomm.connect.telephony.api.util.B2BUAHelper;
import org.restcomm.connect.telephony.api.util.CallControlHelper;
import org.restcomm.smpp.parameter.TlvSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Currency;
import java.util.List;

import static javax.servlet.sip.SipServletResponse.SC_NOT_FOUND;

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

    private final ActorRef monitoringService;

    // configurable switch whether to use the To field in a SIP header to determine the callee address
    // alternatively the Request URI can be used
    private boolean useTo = true;

    //Control whether Restcomm will patch SDP for B2BUA calls
    private boolean patchForNatB2BUASessions;

    //List of extensions for SmsService
    List<RestcommExtensionGeneric> extensions;

    public SmsService(final Configuration configuration, final SipFactory factory,
            final DaoManager storage, final ServletContext servletContext) {
        super();
        this.system = context().system();
        this.configuration = configuration;
        final Configuration runtime = configuration.subset("runtime-settings");
        this.authenticateUsers = runtime.getBoolean("authenticate");
        this.servletConfig = (ServletConfig) configuration.getProperty(ServletConfig.class.getName());
        this.sipFactory = factory;
        this.storage = storage;
        this.servletContext = servletContext;
        monitoringService = (ActorRef) servletContext.getAttribute(MonitoringService.class.getName());
        // final Configuration runtime = configuration.subset("runtime-settings");
        // TODO this.useTo = runtime.getBoolean("use-to");
        patchForNatB2BUASessions = runtime.getBoolean("patch-for-nat-b2bua-sessions", true);

        extensions = ExtensionController.getInstance().getExtensions(ExtensionType.SmsService);
        if (logger.isInfoEnabled()) {
            logger.info("SmsService extensions: "+(extensions != null ? extensions.size() : "0"));
        }
    }

    private void message(final Object message) throws IOException {
        final ActorRef self = self();
        final SipServletRequest request = (SipServletRequest) message;

        // ignore composing messages and accept content type including text only
        // https://github.com/Mobicents/RestComm/issues/494
        if (request.getContentLength()==0 || !request.getContentType().contains("text/plain")) {
            SipServletResponse reject = request.createResponse(SipServletResponse.SC_NOT_ACCEPTABLE);
            reject.addHeader("Reason","Content Type is not text plain");
            reject.send();
            return;
        }

        final SipURI fromURI = (SipURI) request.getFrom().getURI();
        final String fromUser = fromURI.getUser();
        final ClientsDao clients = storage.getClientsDao();
        final Client client = clients.getClient(fromUser);
        final AccountsDao accounts = storage.getAccountsDao();
        final ApplicationsDao applications = storage.getApplicationsDao();

        // Make sure we force clients to authenticate.
        if (client != null) {
            // Make sure we force clients to authenticate.
            if (authenticateUsers // https://github.com/Mobicents/RestComm/issues/29 Allow disabling of SIP authentication
                    && !CallControlHelper.checkAuthentication(request, storage)) {
                if(logger.isInfoEnabled()) {
                    logger.info("Client " + client.getLogin() + " failed to authenticate");
                }
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
            if(logger.isInfoEnabled()) {
                logger.info("Message to :" + toUser + " matched to one of the hosted applications");
            }

            //this is used to send a reply back to SIP client when a Restcomm App forwards inbound sms to a Restcomm client ex. Alice
            final SipServletResponse messageAccepted = request.createResponse(SipServletResponse.SC_ACCEPTED);
            messageAccepted.send();

            monitoringService.tell(new TextMessage(((SipURI)request.getFrom().getURI()).getUser(), ((SipURI)request.getTo().getURI()).getUser(), TextMessage.SmsState.INBOUND_TO_APP), self);

            return;

        }
        if (client != null) {
            // try to see if the request is destined to another registered client
            // if (client != null) { // make sure the caller is a registered client and not some external SIP agent that we
            // have little control over
            Client toClient = clients.getClient(toUser);
            if (toClient != null) { // looks like its a p2p attempt between two valid registered clients, lets redirect
                // to the b2bua
                if (B2BUAHelper.redirectToB2BUA(request, client, toClient, storage, sipFactory, patchForNatB2BUASessions)) {
                    // if all goes well with proxying the SIP MESSAGE on to the target client
                    // then we can end further processing of this request and send response to sender
                    if(logger.isInfoEnabled()) {
                        logger.info("P2P, Message from: " + client.getLogin() + " redirected to registered client: "
                            + toClient.getLogin());
                    }
                    monitoringService.tell(new TextMessage(((SipURI)request.getFrom().getURI()).getUser(), ((SipURI)request.getTo().getURI()).getUser(), TextMessage.SmsState.INBOUND_TO_CLIENT), self);
                    return;
                }
            } else {
                // Since toUser is null, try to route the message outside using the SMS Aggregator
                if(logger.isInfoEnabled()) {
                    logger.info("Restcomm will route this SMS to an external aggregator: " + client.getLogin() + " to: " + toUser);
                }

                final SipServletResponse trying = request.createResponse(SipServletResponse.SC_TRYING);
                trying.send();
                //TODO:do extensions check here too?
                ActorRef session = session(this.configuration);
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
                TlvSet tlvSet = new TlvSet();
                final SmsSessionRequest sms = new SmsSessionRequest(client.getLogin(), toUser, new String(request.getRawContent()), request, tlvSet, null);
                monitoringService.tell(new TextMessage(((SipURI)request.getFrom().getURI()).getUser(), ((SipURI)request.getTo().getURI()).getUser(), TextMessage.SmsState.INBOUND_TO_PROXY_OUT), self);
                session.tell(sms, self());
            }
        } else {
            final SipServletResponse response = request.createResponse(SC_NOT_FOUND);
            response.send();
            // We didn't find anyway to handle the SMS.
            String errMsg = "Restcomm cannot process this SMS because the destination number is not hosted locally. To: "+toUser;
            sendNotification(errMsg, 11005, "error", true);
            monitoringService.tell(new TextMessage(((SipURI)request.getFrom().getURI()).getUser(), ((SipURI)request.getTo().getURI()).getUser(), TextMessage.SmsState.NOT_FOUND), self);
        }}


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
                if (appUri != null || number.getSmsApplicationSid() != null) {
                    final SmsInterpreterBuilder builder = new SmsInterpreterBuilder(system);
                    builder.setSmsService(self);
                    builder.setConfiguration(configuration);
                    builder.setStorage(storage);
                    builder.setAccount(number.getAccountSid());
                    builder.setVersion(number.getApiVersion());
                    final Sid sid = number.getSmsApplicationSid();
                    if (sid != null) {
                        final Application application = applications.getApplication(sid);
                        builder.setUrl(UriUtils.resolve(application.getRcmlUrl()));
                    } else {
                        builder.setUrl(UriUtils.resolve(appUri));
                    }
                    final String smsMethod = number.getSmsMethod();
                    if (smsMethod == null || smsMethod.isEmpty()) {
                        builder.setMethod("POST");
                    } else {
                        builder.setMethod(smsMethod);
                    }
                    URI appFallbackUrl = number.getSmsFallbackUrl();
                    if (appFallbackUrl != null) {
                        builder.setFallbackUrl(UriUtils.resolve(number.getSmsFallbackUrl()));
                        builder.setFallbackMethod(number.getSmsFallbackMethod());
                    }
                    interpreter = builder.build();
                }
                //TODO:do extensions check here too?
                final ActorRef session = session(this.configuration);
                session.tell(request, self);
                final StartInterpreter start = new StartInterpreter(session);
                interpreter.tell(start, self);
                isFoundHostedApp = true;
            }
        } catch (Exception e) {
            String errMsg = "There is no valid Restcomm SMS Request URL configured for this number : " + phone;
            sendNotification(errMsg, 12003, "warning", true);
        }
        return isFoundHostedApp;
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final UntypedActorContext context = getContext();
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        ExtensionController ec = ExtensionController.getInstance();
        if (CreateSmsSession.class.equals(klass)) {
            IExtensionCreateSmsSessionRequest ier = (CreateSmsSession)message;
            ier.setConfiguration(this.configuration);
            ec.executePreOutboundAction(ier, this.extensions);
            if (ier.isAllowed()) {
                final ActorRef session = session(ier.getConfiguration());
                final SmsServiceResponse<ActorRef> response = new SmsServiceResponse<ActorRef>(session);
                sender.tell(response, self);
            } else {
                final SmsServiceResponse<ActorRef> response = new SmsServiceResponse(new RestcommExtensionException("Now allowed to create SmsSession"));
                sender.tell(response, self());
            }
            ec.executePostOutboundAction(message, this.extensions);
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
            B2BUAHelper.forwardResponse(response, patchForNatB2BUASessions);
            return;
        }
        final SipApplicationSession application = response.getApplicationSession();

        //handle SIP application session and make sure it has not being invalidated
        if(logger.isInfoEnabled()) {
            logger.info("Is SipApplicationSession valid: "+application.isValid());
        }
        if(application != null){
            final ActorRef session = (ActorRef) application.getAttribute(SmsSession.class.getName());
            session.tell(response, self);
            final SipServletRequest origRequest = (SipServletRequest) application.getAttribute(SipServletRequest.class.getName());
            if (origRequest != null && origRequest.getSession().isValid()) {
                SipServletResponse responseToOriginator = origRequest.createResponse(response.getStatus(), response.getReasonPhrase());
                responseToOriginator.send();
            }
        }
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

    private ActorRef session(final Configuration p_configuration) {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new SmsSession(p_configuration, sipFactory, outboundInterface(), storage, monitoringService, servletContext);
            }
        });
        return system.actorOf(props);
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
            if(logger.isInfoEnabled()) {
                logger.info(errMessage); // send message to console
            }
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
