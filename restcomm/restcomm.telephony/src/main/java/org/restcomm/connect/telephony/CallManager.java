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
package org.restcomm.connect.telephony;

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
import gov.nist.javax.sip.header.UserAgent;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.patterns.StopObserving;
import org.restcomm.connect.commons.util.SdpUtils;
import org.restcomm.connect.commons.util.UriUtils;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.ApplicationsDao;
import org.restcomm.connect.dao.ClientsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.IncomingPhoneNumbersDao;
import org.restcomm.connect.dao.NotificationsDao;
import org.restcomm.connect.dao.RegistrationsDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Application;
import org.restcomm.connect.dao.entities.Client;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.Notification;
import org.restcomm.connect.dao.entities.Registration;
import org.restcomm.connect.extension.api.CallRequest;
import org.restcomm.connect.extension.api.ExtensionResponse;
import org.restcomm.connect.extension.api.ExtensionType;
import org.restcomm.connect.extension.api.RestcommExtensionException;
import org.restcomm.connect.extension.api.RestcommExtensionGeneric;
import org.restcomm.connect.extension.controller.ExtensionController;
import org.restcomm.connect.interpreter.StartInterpreter;
import org.restcomm.connect.interpreter.StopInterpreter;
import org.restcomm.connect.interpreter.VoiceInterpreterBuilder;
import org.restcomm.connect.monitoringservice.MonitoringService;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;
import org.restcomm.connect.telephony.api.CallManagerResponse;
import org.restcomm.connect.telephony.api.CallResponse;
import org.restcomm.connect.telephony.api.CallStateChanged;
import org.restcomm.connect.telephony.api.CreateCall;
import org.restcomm.connect.telephony.api.DestroyCall;
import org.restcomm.connect.telephony.api.ExecuteCallScript;
import org.restcomm.connect.telephony.api.GetActiveProxy;
import org.restcomm.connect.telephony.api.GetCall;
import org.restcomm.connect.telephony.api.GetCallObservers;
import org.restcomm.connect.telephony.api.GetProxies;
import org.restcomm.connect.telephony.api.GetRelatedCall;
import org.restcomm.connect.telephony.api.Hangup;
import org.restcomm.connect.telephony.api.InitializeOutbound;
import org.restcomm.connect.telephony.api.SwitchProxy;
import org.restcomm.connect.telephony.api.UpdateCallScript;
import org.restcomm.connect.telephony.api.util.B2BUAHelper;
import org.restcomm.connect.telephony.api.util.CallControlHelper;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.sdp.SdpParseException;
import javax.servlet.ServletContext;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TelURL;
import javax.sip.header.RouteHeader;
import javax.sip.message.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static akka.pattern.Patterns.ask;
import static javax.servlet.sip.SipServlet.OUTBOUND_INTERFACES;
import static javax.servlet.sip.SipServletResponse.SC_BAD_REQUEST;
import static javax.servlet.sip.SipServletResponse.SC_FORBIDDEN;
import static javax.servlet.sip.SipServletResponse.SC_NOT_FOUND;
import static javax.servlet.sip.SipServletResponse.SC_OK;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author ivelin.ivanov@telestax.com
 * @author jean.deruelle@telestax.com
 * @author gvagenas@telestax.com
 */
public final class CallManager extends UntypedActor {

    static final int ERROR_NOTIFICATION = 0;
    static final int WARNING_NOTIFICATION = 1;
    static final Pattern PATTERN = Pattern.compile("[\\*#0-9]{1,12}");
    static final String EMAIL_SENDER = "restcomm@restcomm.org";
    static final String EMAIL_SUBJECT = "RestComm Error Notification - Attention Required";
    static final int DEFAUL_IMS_PROXY_PORT = -1;

    private final ActorSystem system;
    private final Configuration configuration;
    private final ServletContext context;
    private final MediaServerControllerFactory msControllerFactory;
    private final ActorRef conferences;
    private final ActorRef bridges;
    private final ActorRef sms;
    private final SipFactory sipFactory;
    private final DaoManager storage;
    private final ActorRef monitoring;

    // configurable switch whether to use the To field in a SIP header to determine the callee address
    // alternatively the Request URI can be used
    private boolean useTo;
    private boolean authenticateUsers;

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
    private String mediaExternalIp;
    private String myHostIp;
    private String proxyIp;

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private CreateCall createCallRequest;
    private SwitchProxy switchProxyRequest;

    //Control whether Restcomm will patch Request-URI and SDP for B2BUA calls
    private boolean patchForNatB2BUASessions;

    //List of extensions for CallManager
    List<RestcommExtensionGeneric> extensions;

    // IMS authentication
    private boolean actAsImsUa;
    private String imsProxyAddress;
    private int imsProxyPort;
    private String imsDomain;

    // used for sending warning and error logs to notification engine and to the console
    private void sendNotification(String errMessage, int errCode, String errType, boolean createNotification) {
        NotificationsDao notifications = storage.getNotificationsDao();
        Notification notification;

        if (errType == "warning") {
            if(logger.isDebugEnabled()) {
                // https://github.com/RestComm/Restcomm-Connect/issues/1419 moved to debug to avoid polluting logs
                logger.debug(errMessage); // send message to console
            }
            if (createNotification) {
                notification = notification(ERROR_NOTIFICATION, errCode, errMessage);
                notifications.addNotification(notification);
            }
        } else if (errType == "error") {
            // https://github.com/RestComm/Restcomm-Connect/issues/1419 moved to debug to avoid polluting logs
            if(logger.isDebugEnabled()) {
                logger.debug(errMessage); // send message to console
            }
            if (createNotification) {
                notification = notification(ERROR_NOTIFICATION, errCode, errMessage);
                notifications.addNotification(notification);
            }
        } else if (errType == "info") {
            // https://github.com/RestComm/Restcomm-Connect/issues/1419 moved to debug to avoid polluting logs
            if(logger.isDebugEnabled()) {
                logger.debug(errMessage); // send message to console
            }
        }

    }

    public CallManager(final Configuration configuration, final ServletContext context, final ActorSystem system,
                       final MediaServerControllerFactory msControllerFactory, final ActorRef conferences, final ActorRef bridges,
                       final ActorRef sms, final SipFactory factory, final DaoManager storage) {
        super();
        this.system = system;
        this.configuration = configuration;
        this.context = context;
        this.msControllerFactory = msControllerFactory;
        this.conferences = conferences;
        this.bridges = bridges;
        this.sms = sms;
        this.sipFactory = factory;
        this.storage = storage;
        final Configuration runtime = configuration.subset("runtime-settings");
        final Configuration outboundProxyConfig = runtime.subset("outbound-proxy");
        SipURI outboundIntf = outboundInterface("udp");
        if (outboundIntf != null) {
            myHostIp = ((SipURI) outboundIntf).getHost().toString();
        } else {
            String errMsg = "SipURI outboundIntf is null";
            sendNotification(errMsg, 14001, "error", false);

            if (context == null)
                errMsg = "SipServlet context is null";
            sendNotification(errMsg, 14002, "error", false);
        }
        Configuration mediaConf = configuration.subset("media-server-manager");
        mediaExternalIp = mediaConf.getString("mgcp-server.external-address");
        proxyIp = runtime.subset("telestax-proxy").getString("uri").replaceAll("http://", "").replaceAll(":2080", "");

        if (mediaExternalIp == null || mediaExternalIp.isEmpty())
            mediaExternalIp = myHostIp;

        if (proxyIp == null || proxyIp.isEmpty())
            proxyIp = myHostIp;

        this.useTo = runtime.getBoolean("use-to");
        this.authenticateUsers = runtime.getBoolean("authenticate");

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

        allowFallback = outboundProxyConfig.getBoolean("allow-fallback", false);

        maxNumberOfFailedCalls = outboundProxyConfig.getInt("max-failed-calls", 20);

        allowFallbackToPrimary = outboundProxyConfig.getBoolean("allow-fallback-to-primary", false);

        patchForNatB2BUASessions = runtime.getBoolean("patch-for-nat-b2bua-sessions", true);

        //Monitoring Service
        this.monitoring = (ActorRef) context.getAttribute(MonitoringService.class.getName());

        extensions = ExtensionController.getInstance().getExtensions(ExtensionType.CallManager);
        if (logger.isInfoEnabled()) {
            logger.info("CallManager extensions: "+(extensions != null ? extensions.size() : "0"));
        }

        final Configuration imsAuthentication = runtime.subset("ims-authentication");
        this.actAsImsUa = imsAuthentication.getBoolean("act-as-ims-ua");
        if (actAsImsUa) {
            this.imsProxyAddress = imsAuthentication.getString("proxy-address");
            this.imsProxyPort = imsAuthentication.getInt("proxy-port");
            if (imsProxyPort == 0) {
                imsProxyPort = DEFAUL_IMS_PROXY_PORT;
            }
            this.imsDomain = imsAuthentication.getString("domain");
            if (actAsImsUa && (imsProxyAddress == null || imsProxyAddress.isEmpty()
                    || imsDomain == null || imsDomain.isEmpty())) {
                logger.warning("ims proxy-address or domain is not configured");
            }
            this.actAsImsUa = actAsImsUa && imsProxyAddress != null && !imsProxyAddress.isEmpty()
                    && imsDomain != null && !imsDomain.isEmpty();
        }
    }

    private ActorRef call() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Call(sipFactory, msControllerFactory.provideCallController(), configuration);
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

    private void destroy(final Object message) throws Exception {
        final UntypedActorContext context = getContext();
        final DestroyCall request = (DestroyCall) message;
        ActorRef call = request.call();
        if (call != null) {
            if(logger.isInfoEnabled()) {
                logger.info("About to destroy call: "+request.call().path()+", call isTerminated(): "+sender().isTerminated()+", sender: "+sender());
            }
            context.stop(call);
        }
    }

    private void invite(final Object message) throws IOException, NumberParseException, ServletParseException {
        final ActorRef self = self();
        final SipServletRequest request = (SipServletRequest) message;
        // Make sure we handle re-invites properly.
        if (!request.isInitial()) {
            SipApplicationSession appSession = request.getApplicationSession();
            ActorRef call = null;
            if (appSession.getAttribute(Call.class.getName()) != null) {
                call = (ActorRef) appSession.getAttribute(Call.class.getName());
            }
            if (call != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("For In-Dialog INVITE dispatched to Call actor: "+call.path());
                }
                call.tell(request, self);
                return;
            }

            if (logger.isInfoEnabled()) {
                logger.info("No call actor found will respond 200OK for In-dialog INVITE: "+request.getRequestURI().toString());
            }
            final SipServletResponse okay = request.createResponse(SC_OK);
            okay.send();
            return;
        }
        if (actAsImsUa) {
            boolean isFromIms = isFromIms(request);
            if (!isFromIms) {
                //This is a WebRTC client that dials out
                String user = request.getHeader("X-RestComm-Ims-User");
                String pass = request.getHeader("X-RestComm-Ims-Password");
                request.removeHeader("X-RestComm-Ims-User");
                request.removeHeader("X-RestComm-Ims-Password");
                imsProxyThroughMediaServer(request, null, request.getTo().getURI(), user, pass, isFromIms);
//                proxyThroughMediaServer(request, null, request.getTo().getURI().toString(), "48399000047@neofon.tp.pl", "Ims12Ims", false);
                return;
            } else {
                logger.info("in client null");
                // Client is null, check if this call is for a registered DID (application)
                imsProxyThroughMediaServer(request, null, request.getTo().getURI(), "", "", isFromIms);
                return;
            }
        }
        //Run proInboundAction Extensions here
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
            if (!authenticateUsers // https://github.com/Mobicents/RestComm/issues/29 Allow disabling of SIP authentication
                    || CallControlHelper.checkAuthentication(request, storage)) {
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
        final String ruri = ((SipURI) request.getRequestURI()).getHost();
        final String toHost = ((SipURI) request.getTo().getURI()).getHost();
        final String toHostIpAddress = InetAddress.getByName(toHost).getHostAddress();
        final String toPort = String.valueOf(((SipURI) request.getTo().getURI()).getPort()).equalsIgnoreCase("-1") ? "5060"
                : String.valueOf(((SipURI) request.getTo().getURI()).getHost());
        final String transport = ((SipURI) request.getTo().getURI()).getTransportParam() == null ? "udp" : ((SipURI) request
                .getTo().getURI()).getTransportParam();
        SipURI outboundIntf = outboundInterface(transport);

        if(logger.isInfoEnabled()) {
            logger.info("ToHost: " + toHost);
            logger.info("ruri: " + ruri);
            logger.info("myHostIp: " + myHostIp);
            logger.info("mediaExternalIp: " + mediaExternalIp);
            logger.info("proxyIp: " + proxyIp);
        }

        if (client != null) { // make sure the caller is a registered client and not some external SIP agent that we have little control over
            Client toClient = clients.getClient(toUser);
            if (toClient != null) { // looks like its a p2p attempt between two valid registered clients, lets redirect to the b2bua
                if(logger.isInfoEnabled()) {
                    logger.info("Client is not null: " + client.getLogin() + " will try to proxy to client: "+ toClient);
                }
                CallRequest callRequest = new CallRequest(fromUser, toUser, CallRequest.Type.CLIENT,
                        client.getAccountSid(), false, false);
                if (executePreOutboundAction(callRequest)) {
                    if (B2BUAHelper.redirectToB2BUA(request, client, toClient, storage, sipFactory, patchForNatB2BUASessions)) {
                        if(logger.isInfoEnabled()) {
                            logger.info("Call to CLIENT.  myHostIp: " + myHostIp + " mediaExternalIp: " + mediaExternalIp + " toHost: "
                                + toHost + " fromClient: " + client.getUri() + " toClient: " + toClient.getUri());
                        }
                        // if all goes well with proxying the invitation on to the next client
                        // then we can end further processing of this INVITE
                    } else {
                        String errMsg = "Cannot Connect to Client: " + toClient.getFriendlyName()
                                + " : Make sure the Client exist or is registered with Restcomm";
                        sendNotification(errMsg, 11001, "warning", true);
                        final SipServletResponse resp = request.createResponse(SC_NOT_FOUND, "Cannot complete P2P call");
                        resp.send();
                    }
                } else {
                    //Extensions didn't allowed this call
                    if (logger.isDebugEnabled()) {
                        final String errMsg = "Client not Allowed to make this outbound call";
                        logger.debug(errMsg);
                    }
                    String errMsg = "Cannot Connect to Client: " + toClient.getFriendlyName()
                            + " : Make sure the Client exist or is registered with Restcomm";
                    sendNotification(errMsg, 11001, "warning", true);
                    final SipServletResponse resp = request.createResponse(SC_FORBIDDEN, "Call not allowed");
                    resp.send();
                }
                executePostOutboundAction(callRequest);
                return;
            } else {
                // toClient is null or we couldn't make the b2bua call to another client. check if this call is for a registered
                // DID (application)
                if (redirectToHostedVoiceApp(self, request, accounts, applications, toUser)) {
                    // This is a call to a registered DID (application)
                    return;
                }
                // This call is not a registered DID (application). Try to proxy out this call.
                // log to console and to notification engine
                String errMsg = "A Restcomm Client is trying to call a Number/DID that is not registered with Restcomm";
                sendNotification(errMsg, 11002, "info", true);

                if (isWebRTC(request)) {
                    //This is a WebRTC client that dials out
                    proxyThroughMediaServer(request, client, toUser);
                    return;
                }

                // https://telestax.atlassian.net/browse/RESTCOMM-335
                final String proxyURI = activeProxy;
                final String proxyUsername = activeProxyUsername;
                final String proxyPassword = activeProxyPassword;
                SipURI from = null;
                SipURI to = null;
                boolean callToSipUri = false;
                // proxy DID or number if the outbound proxy fields are not empty in the restcomm.xml
                if (proxyURI != null && !proxyURI.isEmpty()) {
//                    String destination = ((SipURI)request.getTo().getURI()).getUser();
                    CallRequest callRequest = new CallRequest(fromUser,toUser, CallRequest.Type.PSTN, client.getAccountSid(), false, false);
                    if (executePreOutboundAction(callRequest)) {
                        proxyOut(request, client, toUser, toHost, toHostIpAddress, toPort, outboundIntf, proxyURI, proxyUsername, proxyPassword, from, to, callToSipUri);
                    } else {
                        final SipServletResponse response = request.createResponse(SC_FORBIDDEN, "Call request not allowed");
                        response.send();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Call request now allowed: "+callRequest.toString());
                        }
                    }
                    executePostOutboundAction(callRequest);
                    return;
                } else {
                    String msg = "Restcomm tried to proxy this call to an outbound party but it seems the outbound proxy is not configured.";
                    sendNotification(errMsg, 11004, "warning", true);
                }
            }
        } else {
            // Client is null, check if this call is for a registered DID (application)
            if (redirectToHostedVoiceApp(self, request, accounts, applications, toUser)) {
                // This is a call to a registered DID (application)
                return;
            }
        }
        final SipServletResponse response = request.createResponse(SC_NOT_FOUND);
        response.send();
        // We didn't find anyway to handle the call.
        String errMsg = "Restcomm cannot process this call because the destination number " + toUser
                + "cannot be found or there is application attached to that";
        sendNotification(errMsg, 11005, "error", true);

    }

    private boolean proxyOut(SipServletRequest request, Client client, String toUser, String toHost, String toHostIpAddress, String toPort, SipURI outboundIntf, String proxyURI, String proxyUsername, String proxyPassword, SipURI from, SipURI to, boolean callToSipUri) throws UnknownHostException {
        final Configuration runtime = configuration.subset("runtime-settings");
        final boolean useLocalAddressAtFromHeader = runtime.getBoolean("use-local-address", false);
        final boolean outboudproxyUserAtFromHeader = runtime.subset("outbound-proxy").getBoolean(
                "outboudproxy-user-at-from-header", true);

        final String fromHost = ((SipURI) request.getFrom().getURI()).getHost();
        final String fromHostIpAddress = InetAddress.getByName(fromHost).getHostAddress();
//                    final String fromPort = String.valueOf(((SipURI) request.getFrom().getURI()).getPort()).equalsIgnoreCase("-1") ? "5060"
//                            : String.valueOf(((SipURI) request.getFrom().getURI()).getHost());

        if(logger.isInfoEnabled()) {
            logger.info("fromHost: " + fromHost + "fromHostIP: " + fromHostIpAddress + "myHostIp: " + myHostIp + " mediaExternalIp: " + mediaExternalIp
            + " toHost: " + toHost + " toHostIP: " + toHostIpAddress + " proxyUri: " + proxyURI);
        }
        if ((myHostIp.equalsIgnoreCase(toHost) || mediaExternalIp.equalsIgnoreCase(toHost)) ||
                (myHostIp.equalsIgnoreCase(toHostIpAddress) || mediaExternalIp.equalsIgnoreCase(toHostIpAddress))
                // https://github.com/RestComm/Restcomm-Connect/issues/1357
                || (fromHost.equalsIgnoreCase(toHost) || fromHost.equalsIgnoreCase(toHostIpAddress))
                || (fromHostIpAddress.equalsIgnoreCase(toHost) || fromHostIpAddress.equalsIgnoreCase(toHostIpAddress))) {
            if(logger.isInfoEnabled()) {
                logger.info("Call to NUMBER.  myHostIp: " + myHostIp + " mediaExternalIp: " + mediaExternalIp
                + " toHost: " + toHost + " proxyUri: " + proxyURI);
            }
            try {
                if (useLocalAddressAtFromHeader) {
                    if (outboudproxyUserAtFromHeader) {
                        from = (SipURI) sipFactory.createSipURI(proxyUsername,
                                mediaExternalIp + ":" + outboundIntf.getPort());
                    } else {
                        from = sipFactory.createSipURI(((SipURI) request.getFrom().getURI()).getUser(),
                                mediaExternalIp + ":" + outboundIntf.getPort());
                    }
                } else {
                    if (outboudproxyUserAtFromHeader) {
                        // https://telestax.atlassian.net/browse/RESTCOMM-633. Use the outbound proxy username as
                        // the userpart of the sip uri for the From header
                        from = (SipURI) sipFactory.createSipURI(proxyUsername, proxyURI);
                    } else {
                        from = sipFactory.createSipURI(((SipURI) request.getFrom().getURI()).getUser(), proxyURI);
                    }
                }
                to = sipFactory.createSipURI(((SipURI) request.getTo().getURI()).getUser(), proxyURI);
            } catch (Exception exception) {
                if(logger.isInfoEnabled()) {
                logger.info("Exception: " + exception);
                }
            }
        } else {
            if(logger.isInfoEnabled()) {
            logger.info("Call to SIP URI. myHostIp: " + myHostIp + " mediaExternalIp: " + mediaExternalIp
                + " toHost: " + toHost + " proxyUri: " + proxyURI);
            }
            from = sipFactory.createSipURI(((SipURI) request.getFrom().getURI()).getUser(), outboundIntf.getHost()
                    + ":" + outboundIntf.getPort());
            to = sipFactory.createSipURI(toUser, toHost + ":" + toPort);
            callToSipUri = true;
        }
        if (B2BUAHelper.redirectToB2BUA(request, client, from, to, proxyUsername, proxyPassword, storage,
                sipFactory, callToSipUri, patchForNatB2BUASessions)) {
            return true;
        }
        return false;
    }

    private boolean isWebRTC(final SipServletRequest request) {
        String transport = request.getTransport();
        String userAgent = request.getHeader(UserAgent.NAME);
        //The check for request.getHeader(UserAgentHeader.NAME).equals("sipunit") has been added in order to be able to test this feature with sipunit at the Restcomm testsuite
        if (userAgent != null && !userAgent.isEmpty() && userAgent.equalsIgnoreCase("wss-sipunit")) {
            return true;
        }
        if (!request.getInitialTransport().equalsIgnoreCase(transport)) {
            transport = request.getInitialTransport();
            if ("ws".equalsIgnoreCase(transport) || "wss".equalsIgnoreCase(transport))
                return true;
        }
        try {
            if (SdpUtils.isWebRTCSDP(request.getContentType(), request.getRawContent())) {
                return true;
            }
        } catch (SdpParseException e) {}
          catch (IOException e) {}

        return false;
    }

    private void proxyThroughMediaServer(final SipServletRequest request, final Client client, final String destNumber) {
        String rcml = "<Response><Dial>"+destNumber+"</Dial></Response>";
        final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
        builder.setConfiguration(configuration);
        builder.setStorage(storage);
        builder.setCallManager(self());
        builder.setConferenceManager(conferences);
        builder.setBridgeManager(bridges);
        builder.setSmsService(sms);
        builder.setAccount(client.getAccountSid());
        builder.setVersion(client.getApiVersion());
        final Account account = storage.getAccountsDao().getAccount(client.getAccountSid());
        builder.setEmailAddress(account.getEmailAddress());
        builder.setRcml(rcml);
        builder.setMonitoring(monitoring);
        final ActorRef interpreter = builder.build();
        final ActorRef call = call();
        final SipApplicationSession application = request.getApplicationSession();
        application.setAttribute(Call.class.getName(), call);
        call.tell(request, self());
        interpreter.tell(new StartInterpreter(call), self());
    }

    private void info(final SipServletRequest request) throws IOException {
        final ActorRef self = self();
        final SipApplicationSession application = request.getApplicationSession();

        // if this response is coming from a client that is in a p2p session with another registered client
        // we will just proxy the response
        SipSession linkedB2BUASession = B2BUAHelper.getLinkedSession(request);
        if (linkedB2BUASession != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("B2BUA: Got INFO request: \n %s", request));
            }
            request.getSession().setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, request);
            SipServletRequest clonedInfo = linkedB2BUASession.createRequest("INFO");
            linkedB2BUASession.setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, clonedInfo);

            // Issue #307: https://telestax.atlassian.net/browse/RESTCOMM-307
            SipURI toInetUri = (SipURI) request.getSession().getAttribute(B2BUAHelper.TO_INET_URI);
            SipURI fromInetUri = (SipURI) request.getSession().getAttribute(B2BUAHelper.FROM_INET_URI);
            InetAddress infoRURI = null;
            try {
                infoRURI = InetAddress.getByName(((SipURI) clonedInfo.getRequestURI()).getHost());
            } catch (UnknownHostException e) {
            }
            if (patchForNatB2BUASessions) {
                if (toInetUri != null && infoRURI == null) {

                    if(logger.isInfoEnabled()){
                        logger.info("Using the real ip address of the sip client " + toInetUri.toString()
                            + " as a request uri of the CloneBye request");
                    }
                    clonedInfo.setRequestURI(toInetUri);
                } else if (toInetUri != null
                        && (infoRURI.isSiteLocalAddress() || infoRURI.isAnyLocalAddress() || infoRURI.isLoopbackAddress())) {

                    if(logger.isInfoEnabled()){
                        logger.info("Using the real ip address of the sip client " + toInetUri.toString()
                            + " as a request uri of the CloneInfo request");
                    }
                    clonedInfo.setRequestURI(toInetUri);
                } else if (fromInetUri != null
                        && (infoRURI.isSiteLocalAddress() || infoRURI.isAnyLocalAddress() || infoRURI.isLoopbackAddress())) {
                     if(logger.isInfoEnabled()){
                        logger.info("Using the real ip address of the sip client " + fromInetUri.toString()
                           + " as a request uri of the CloneInfo request");
                     }

                    clonedInfo.setRequestURI(fromInetUri);
                }
            }
            clonedInfo.send();
        } else {
            final ActorRef call = (ActorRef) application.getAttribute(Call.class.getName());
            call.tell(request, self);
        }
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
        } catch (Exception e) {
        }
        IncomingPhoneNumber number = null;
        try {
            // Try to find an application defined for the phone number.
            final IncomingPhoneNumbersDao numbers = storage.getIncomingPhoneNumbersDao();
            number = numbers.getIncomingPhoneNumber(formatedPhone);
            if (number == null) {
                number = numbers.getIncomingPhoneNumber(phone);
            }
            if(number == null){
                if (phone.startsWith("+")) {
                    //remove the (+) and check if exists
                    phone= phone.replaceFirst("\\+","");
                    number = numbers.getIncomingPhoneNumber(phone);
                } else {
                    //Add "+" add check if number exists
                    phone = "+".concat(phone);
                    number = numbers.getIncomingPhoneNumber(phone);
                }
            }
            if (number == null) {
                // https://github.com/Mobicents/RestComm/issues/84 using wildcard as default application
                number = numbers.getIncomingPhoneNumber("*");
            }
            if (number != null) {
                final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
                builder.setConfiguration(configuration);
                builder.setStorage(storage);
                builder.setCallManager(self);
                builder.setConferenceManager(conferences);
                builder.setBridgeManager(bridges);
                builder.setSmsService(sms);
                builder.setAccount(number.getAccountSid());
                builder.setVersion(number.getApiVersion());
                final Account account = accounts.getAccount(number.getAccountSid());
                builder.setEmailAddress(account.getEmailAddress());
                final Sid sid = number.getVoiceApplicationSid();
                if (sid != null) {
                    final Application application = applications.getApplication(sid);
                    builder.setUrl(UriUtils.resolve(application.getRcmlUrl()));
                } else {
                    builder.setUrl(UriUtils.resolve(number.getVoiceUrl()));
                }
                final String voiceMethod = number.getVoiceMethod();
                if (voiceMethod == null || voiceMethod.isEmpty()) {
                    builder.setMethod("POST");
                } else {
                    builder.setMethod(voiceMethod);
                }
                URI uri = number.getVoiceFallbackUrl();
                if (uri != null)
                    builder.setFallbackUrl(UriUtils.resolve(uri));
                else
                    builder.setFallbackUrl(null);
                builder.setFallbackMethod(number.getVoiceFallbackMethod());
                builder.setStatusCallback(number.getStatusCallback());
                builder.setStatusCallbackMethod(number.getStatusCallbackMethod());
                builder.setMonitoring(monitoring);
                final ActorRef interpreter = builder.build();
                final ActorRef call = call();
                final SipApplicationSession application = request.getApplicationSession();
                application.setAttribute(Call.class.getName(), call);
                call.tell(request, self);
                interpreter.tell(new StartInterpreter(call), self);
                isFoundHostedApp = true;
            }
        } catch (Exception notANumber) {
            String errMsg;
            if (number != null) {
                errMsg = "The number " + number.getPhoneNumber() + " does not have a Restcomm hosted application attached";
            } else {
                errMsg = "The number does not have a Restcomm hosted application attached";
            }
            sendNotification(errMsg, 11007, "error", false);
            logger.warning(errMsg, notANumber);
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
        Sid applicationSid = client.getVoiceApplicationSid();
        URI clientAppVoiceUrl = null;
        if (applicationSid != null) {
            final Application application = applications.getApplication(applicationSid);
            clientAppVoiceUrl = UriUtils.resolve(application.getRcmlUrl());
        }
        if (clientAppVoiceUrl == null) {
            clientAppVoiceUrl = client.getVoiceUrl();
        }
        boolean isClientManaged =( (applicationSid != null && !applicationSid.toString().isEmpty() && !applicationSid.toString().equals("")) ||
                (clientAppVoiceUrl != null && !clientAppVoiceUrl.toString().isEmpty() &&  !clientAppVoiceUrl.toString().equals("")));
        if (isClientManaged) {
            final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
            builder.setConfiguration(configuration);
            builder.setStorage(storage);
            builder.setCallManager(self);
            builder.setConferenceManager(conferences);
            builder.setBridgeManager(bridges);
            builder.setSmsService(sms);
            builder.setAccount(client.getAccountSid());
            builder.setVersion(client.getApiVersion());
            final Account account = accounts.getAccount(client.getAccountSid());
            builder.setEmailAddress(account.getEmailAddress());
            final Sid sid = client.getVoiceApplicationSid();
            builder.setUrl(clientAppVoiceUrl);
            builder.setMethod(client.getVoiceMethod());
            URI uri = client.getVoiceFallbackUrl();
            if (uri != null)
                builder.setFallbackUrl(UriUtils.resolve(uri));
            else
                builder.setFallbackUrl(null);
            builder.setFallbackMethod(client.getVoiceFallbackMethod());
            builder.setMonitoring(monitoring);
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
        if(logger.isDebugEnabled()) {
            logger.debug("######### CallManager new message received, message instanceof : " + klass + " from sender : "
                + sender.getClass());
        }
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
            } else if ("INFO".equals(method)) {
                info(request);
            }
        } else if (CreateCall.class.equals(klass)) {
            this.createCallRequest = (CreateCall) message;
            outbound(message, sender);
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
//            if (!ack.getHeaders("Route").hasNext() && patchForNatB2BUASessions) {
            if (patchForNatB2BUASessions) {
                InetAddress ackRURI = null;
                try {
                    ackRURI = InetAddress.getByName(((SipURI) ack.getRequestURI()).getHost());
                } catch (UnknownHostException e) {
                }
                boolean isBehindLB = false;
                final String initialIpBeforeLB = response.getHeader("X-Sip-Balancer-InitialRemoteAddr");
                String initialPortBeforeLB = response.getHeader("X-Sip-Balancer-InitialRemotePort");
                if (initialIpBeforeLB != null) {
                    if (initialPortBeforeLB == null)
                        initialPortBeforeLB = "5060";
                    if (logger.isDebugEnabled()) {
                        logger.debug("We are behind load balancer, checking if the request URI needs to be patched");
                    }
                    isBehindLB = true;
                }
                // Issue #307: https://telestax.atlassian.net/browse/RESTCOMM-307
                SipURI toInetUri = (SipURI) request.getSession().getAttribute(B2BUAHelper.TO_INET_URI);
                if (toInetUri != null && ackRURI == null) {
                    if(isBehindLB) {
                        // https://github.com/RestComm/Restcomm-Connect/issues/1357
                        boolean patchRURI = isLBPatchRURI(ack, initialIpBeforeLB, initialPortBeforeLB);
                        if(patchRURI) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("We are behind load balancer, but Using the real ip address of the sip client " + toInetUri.toString()
                                    + " as a request uri of the ACK request");
                            }
                            ack.setRequestURI(toInetUri);
                        } else {
                            // https://github.com/RestComm/Restcomm-Connect/issues/1357
                            if (logger.isDebugEnabled()) {
                                logger.debug("removing the toInetUri to avoid the other subsequent requests using it " + toInetUri.toString());
                            }
                            request.getSession().removeAttribute(B2BUAHelper.TO_INET_URI);
                        }
                    } else {
                        if(logger.isInfoEnabled()) {
                            logger.info("Using the real ip address of the sip client " + toInetUri.toString()
                                + " as a request uri of the ACK request");
                        }
                        ack.setRequestURI(toInetUri);
                    }
                } else if (toInetUri != null
                        && (ackRURI.isSiteLocalAddress() || ackRURI.isAnyLocalAddress() || ackRURI.isLoopbackAddress())) {
                    if(isBehindLB) {
                        // https://github.com/RestComm/Restcomm-Connect/issues/1357
                        boolean patchRURI = isLBPatchRURI(ack, initialIpBeforeLB, initialPortBeforeLB);
                        if(patchRURI) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("We are behind load balancer, but Using the real ip address of the sip client " + toInetUri.toString()
                                    + " as a request uri of the ACK request");
                            }
                            ack.setRequestURI(toInetUri);
                        } else {
                            // https://github.com/RestComm/Restcomm-Connect/issues/1357
                            if (logger.isDebugEnabled()) {
                                logger.debug("removing the toInetUri to avoid the other subsequent requests using it " + toInetUri.toString());
                            }
                            request.getSession().removeAttribute(B2BUAHelper.TO_INET_URI);
                        }
                    } else {
                        if(logger.isInfoEnabled()) {
                            logger.info("Using the real ip address of the sip client " + toInetUri.toString()
                                + " as a request uri of the ACK request");
                        }
                        ack.setRequestURI(toInetUri);
                    }
                } else if (toInetUri == null
                        && (ackRURI.isSiteLocalAddress() || ackRURI.isAnyLocalAddress() || ackRURI.isLoopbackAddress())) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Public IP toInetUri from SipSession is null, will check LB headers from last Response");
                    }
                    if(isBehindLB) {
                        String realIP = initialIpBeforeLB + ":" + initialPortBeforeLB;
                        SipURI uri = sipFactory.createSipURI(null, realIP);
                        boolean patchRURI = isLBPatchRURI(ack, initialIpBeforeLB, initialPortBeforeLB);
                        if(patchRURI) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("We are behind load balancer, will use Initial Remote Address " + initialIpBeforeLB + ":"
                                        + initialPortBeforeLB + " for the ACK request");
                            }
                            ack.setRequestURI(uri);
                        }
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info("LB Headers are also null");
                        }
                    }
                }
            }
            ack.send();
            SipApplicationSession sipApplicationSession = request.getApplicationSession();
            // Defaulting the sip application session to 1h
            sipApplicationSession.setExpires(60);
        } else {
            if(logger.isInfoEnabled()) {
                logger.info("Linked Response couldn't be found for ACK request");
            }
            final ActorRef call = (ActorRef) request.getApplicationSession().getAttribute(Call.class.getName());
            if (call != null) {
                if(logger.isInfoEnabled()) {
                    logger.info("Will send ACK to call actor: "+call.path());
                }
                call.tell(request, self());
            }
        }
        // else {
        // SipSession sipSession = request.getSession();
        // SipApplicationSession sipAppSession = request.getApplicationSession();
        // if(sipSession.getInvalidateWhenReady()){
        // logger.info("Invalidating sipSession: "+sipSession.getId());
        // sipSession.invalidate();
        // }
        // if(sipAppSession.getInvalidateWhenReady()){
        // logger.info("Invalidating sipAppSession: "+sipAppSession.getId());
        // sipAppSession.invalidate();
        // }
        // }
    }

    private boolean isLBPatchRURI(SipServletRequest request,
            final String initialIpBeforeLB, String initialPortBeforeLB) {
        try {
            // https://github.com/RestComm/Restcomm-Connect/issues/1336 checking if the initial IP and Port behind LB is part of the route set or not
            ListIterator<? extends Address> routes = request.getAddressHeaders(RouteHeader.NAME);
            while(routes.hasNext()) {
                SipURI route = (SipURI) routes.next().getURI();
                String routeHost = route.getHost();
                int routePort = route.getPort();
                if(routePort < 0) {
                    routePort = 5060;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Checking if route " + routeHost + ":" + routePort + " is matching ip and port before LB " + initialIpBeforeLB + ":"
                        + initialPortBeforeLB + " for the " + request.getMethod() + " request");
                }
                if(routeHost.equalsIgnoreCase(initialIpBeforeLB) && routePort == Integer.parseInt(initialPortBeforeLB)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("route " + route + " is matching ip and port before LB " + initialIpBeforeLB + ":"
                            + initialPortBeforeLB + " for the " + request.getMethod() + " request, so not patching the Request-URI");
                    }
                    return false;
                }
            }
        } catch (ServletParseException e) {
            logger.error("Impossible to parse the route set from the request " + request, e);
        }
        return true;
    }

    private void execute(final Object message) {
        final ExecuteCallScript request = (ExecuteCallScript) message;
        final ActorRef self = self();
        final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
        builder.setConfiguration(configuration);
        builder.setStorage(storage);
        builder.setCallManager(self);
        builder.setConferenceManager(conferences);
        builder.setBridgeManager(bridges);
        builder.setSmsService(sms);
        builder.setAccount(request.account());
        builder.setVersion(request.version());
        builder.setUrl(request.url());
        builder.setMethod(request.method());
        builder.setFallbackUrl(request.fallbackUrl());
        builder.setFallbackMethod(request.fallbackMethod());
        builder.setStatusCallback(request.callback());
        builder.setStatusCallbackMethod(request.callbackMethod());
        builder.setMonitoring(monitoring);
        final ActorRef interpreter = builder.build();
        interpreter.tell(new StartInterpreter(request.call()), self);
    }

    @SuppressWarnings("unchecked")
    private void update(final Object message) throws Exception {
        final UpdateCallScript request = (UpdateCallScript) message;
        final ActorRef self = self();
        final ActorRef call = request.call();
        final Boolean moveConnectedCallLeg = request.moveConnecteCallLeg();

        // Get first call leg observers
        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        Future<Object> future = (Future<Object>) ask(call, new GetCallObservers(), expires);
        CallResponse<List<ActorRef>> response = (CallResponse<List<ActorRef>>) Await.result(future,
                Duration.create(10, TimeUnit.SECONDS));
        List<ActorRef> callObservers = response.get();

        // Get the Voice Interpreter currently handling the call
        //TODO possible bug here. Since we have more than one call observer, later there might be the case that the first one is not the VI
        //TODO set the VI using specific message, also get the VI using specific message. The VI will still be in the observers list but it will set/get using specific method
        ActorRef existingInterpreter = callObservers.iterator().next();

        // Get the outbound leg of this call
        future = (Future<Object>) ask(existingInterpreter, new GetRelatedCall(call), expires);
        Object answer = (Object) Await.result(future, Duration.create(10, TimeUnit.SECONDS));

        ActorRef relatedCall = null;
        List<ActorRef> listOfRelatedCalls = null;
        if (answer instanceof ActorRef) {
            relatedCall = (ActorRef) answer;
        } else if (answer instanceof List) {
            listOfRelatedCalls = (List<ActorRef>) answer;
        }

        if(logger.isInfoEnabled()) {
            logger.info("About to start Live Call Modification, moveConnectedCallLeg: "+moveConnectedCallLeg);
            logger.info("Initial Call path: " + call.path());
            if (relatedCall != null) {
                logger.info("Related Call path: " + relatedCall.path());
            }
            if (listOfRelatedCalls != null) {
                logger.info("List of related calls received, size of the list: "+listOfRelatedCalls.size());
            }
            // Cleanup all observers from both call legs
            logger.info("Will tell Call actors to stop observing existing Interpreters");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("LCM account: "+ request.account() +", moveConnectedCallLeg: "+moveConnectedCallLeg+", new RCML url: "+request.url());
        }
        call.tell(new StopObserving(), self());
        if (relatedCall != null) {
            relatedCall.tell(new StopObserving(), self());
        }
        if (listOfRelatedCalls != null) {
            for(ActorRef branch: listOfRelatedCalls) {
                branch.tell(new StopObserving(), self());
            }
        }
        if(logger.isInfoEnabled()) {
            logger.info("Existing observers removed from Calls actors");

            // Cleanup existing Interpreter
            logger.info("Existing Interpreter path: " + existingInterpreter.path() + " will be stopped");
        }
        existingInterpreter.tell(new StopInterpreter(true), null);

        // Build a new VoiceInterpreter
        final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
        builder.setConfiguration(configuration);
        builder.setStorage(storage);
        builder.setCallManager(self);
        builder.setConferenceManager(conferences);
        builder.setBridgeManager(bridges);
        builder.setSmsService(sms);
        builder.setAccount(request.account());
        builder.setVersion(request.version());
        builder.setUrl(request.url());
        builder.setMethod(request.method());
        builder.setFallbackUrl(request.fallbackUrl());
        builder.setFallbackMethod(request.fallbackMethod());
        builder.setStatusCallback(request.callback());
        builder.setStatusCallbackMethod(request.callbackMethod());
        builder.setMonitoring(monitoring);

        // Ask first call leg to execute with the new Interpreter
        final ActorRef interpreter = builder.build();
        system.scheduler().scheduleOnce(Duration.create(500, TimeUnit.MILLISECONDS), interpreter,
                new StartInterpreter(request.call()), system.dispatcher());
        // interpreter.tell(new StartInterpreter(request.call()), self);
        if(logger.isInfoEnabled()) {
            logger.info("New Intepreter for first call leg: " + interpreter.path() + " started");
        }

        // Check what to do with the second/outbound call leg of the call
        if (relatedCall != null && listOfRelatedCalls == null) {
            if (moveConnectedCallLeg) {
                final ActorRef relatedInterpreter = builder.build();
                if(logger.isInfoEnabled()) {
                    logger.info("About to redirect related Call :" + relatedCall.path()
                        + " with 200ms delay to related interpreter: " + relatedInterpreter.path());
                }
                system.scheduler().scheduleOnce(Duration.create(1000, TimeUnit.MILLISECONDS), relatedInterpreter,
                        new StartInterpreter(relatedCall), system.dispatcher());

                if(logger.isInfoEnabled()) {
                    logger.info("New Intepreter for Second call leg: " + relatedInterpreter.path() + " started");
                }
            } else {
                if(logger.isInfoEnabled()) {
                    logger.info("moveConnectedCallLeg is: " + moveConnectedCallLeg + " so will hangup relatedCall: "+relatedCall.path());
                }
                relatedCall.tell(new Hangup(), null);
//                getContext().stop(relatedCall);
            }
        }
        if (listOfRelatedCalls != null) {
            for (ActorRef branch: listOfRelatedCalls) {
                branch.tell(new Hangup(), null);
            }
            if (logger.isInfoEnabled()) {
                String msg = String.format("LiveCallModification request while dial forking, terminated %d calls", listOfRelatedCalls.size());
                logger.info(msg);
            }
        }
    }

    private void outbound(final Object message, final ActorRef sender) throws ServletParseException {
        final CreateCall request = (CreateCall) message;
        CallRequest callRequest = new CallRequest(request.from(), request.to(), CallRequest.Type.valueOf(request.type().name()), request.accountId(), request.isFromApi(), request.parentCallSid() != null);
        switch (request.type()) {
            case CLIENT: {
                if (executePreOutboundAction(callRequest)) {
                    outboundToClient(request, sender);
                } else {
                    //Extensions didn't allowed this call
                    final String errMsg = "Not Allowed to make this outbound call";
                    logger.warning(errMsg);
                    sender.tell(new CallManagerResponse<ActorRef>(new RestcommExtensionException(errMsg), this.createCallRequest), self());
                }
                executePostOutboundAction(callRequest);
                break;
            }
            case PSTN: {
                if (executePreOutboundAction(callRequest)) {
                    outboundToPstn(request, sender);
                } else {
                    //Extensions didn't allowed this call
                    final String errMsg = "Not Allowed to make this outbound call";
                    logger.warning(errMsg);
                    sender.tell(new CallManagerResponse<ActorRef>(new RestcommExtensionException(errMsg), this.createCallRequest), self());
                }
                executePostOutboundAction(callRequest);
                break;
            }
            case SIP: {
                if (actAsImsUa) {
                    outboundToIms(request, sender);
                }
                else if (executePreOutboundAction(callRequest)) {
                    outboundToSip(request, sender);
                }  else {
                    //Extensions didn't allowed this call
                    final String errMsg = "Not Allowed to make this outbound call";
                    logger.warning(errMsg);
                    sender.tell(new CallManagerResponse<ActorRef>(new RestcommExtensionException(errMsg), this.createCallRequest), self());
                }
                executePostOutboundAction(callRequest);
                break;
            }
        }
    }

    private boolean executePreOutboundAction(final Object message) {
        if (extensions != null && extensions.size() > 0) {
            for (RestcommExtensionGeneric extension : extensions) {
                if (extension.isEnabled()) {
                    ExtensionResponse response = extension.preOutboundAction(message);
                    if (!response.isAllowed())
                        return false;
                }
            }
        }
        return true;
    }

    private boolean executePostOutboundAction(final CallRequest callRequest) {
        return false;
    }

    private void outboundToClient(final CreateCall request, final ActorRef sender) throws ServletParseException {
        SipURI outboundIntf = null;
        SipURI from = null;
        SipURI to = null;
        boolean webRTC = false;
        boolean isLBPresent = false;

        final RegistrationsDao registrationsDao = storage.getRegistrationsDao();
        final String client = request.to().replaceFirst("client:", "");

        //1, If this is a WebRTC client check if the instance is the current instance
        //2. Check if the client has more than one registrations

        List<Registration> registrationToDial = new CopyOnWriteArrayList<Registration>();

        List<Registration> registrations = registrationsDao.getRegistrations(client);
        if (registrations != null && registrations.size() > 0) {
            if (logger.isInfoEnabled()) {
                logger.info("Preparing call for client: "+client+". There are "+registrations.size()+" registrations at the database for this client");
            }
            for (Registration registration : registrations) {
                if (registration.isWebRTC()) {
                    if (registration.isLBPresent()) {
                        if (logger.isInfoEnabled())
                            logger.info("WebRTC registration behind LB. Will add WebRTC registration: " + registration.getLocation() + " to the list to be dialed for client: " + client);
                        registrationToDial.add(registration);
                    } else {
                        //If this is a WebRTC client registration, check that the InstanceId of the registration is for the current Restcomm instance
                        if ((registration.getInstanceId() != null && !registration.getInstanceId().equals(RestcommConfiguration.getInstance().getMain().getInstanceId()))) {
                            logger.warning("Cannot create call for user agent: " + registration.getLocation() + " since this is a webrtc client registered in another Restcomm instance.");
                        } else {
                            if (logger.isInfoEnabled())
                                logger.info("Will add WebRTC registration: " + registration.getLocation() + " to the list to be dialed for client: " + client);
                            registrationToDial.add(registration);
                        }
                    }
                } else {
                    if (logger.isInfoEnabled())
                        logger.info("Will add registration: "+registration.getLocation()+" to the list to be dialed for client: "+client);
                    registrationToDial.add(registration);
                }
            }
        } else {
            String errMsg = "The SIP Client "+request.to()+" is not registered or does not exist";
            logger.warning(errMsg);
            sendNotification(errMsg, 11008, "error", true);
            sender.tell(new CallManagerResponse<ActorRef>(new NullPointerException(errMsg), this.createCallRequest), self());
            return;
        }

        if (registrationToDial.size() > 0) {
            if (logger.isInfoEnabled()) {
                if (registrationToDial.size()>1) {
                    logger.info("Preparing call for client: "+client+", after WebRTC check, Restcomm have to dial :"+registrationToDial.size()+" registrations");
                }
            }
            List<ActorRef> calls = new CopyOnWriteArrayList<>();
            for (Registration registration : registrationToDial) {
                if (logger.isInfoEnabled())
                    logger.info("Will proceed to create call for client: " + registration.getLocation() + " registration instanceId: " + registration.getInstanceId() + " own InstanceId: " + RestcommConfiguration.getInstance().getMain().getInstanceId());
                String transport;
                if (registration.getLocation().contains("transport")) {
                    transport = registration.getLocation().split(";")[1].replace("transport=", "");
                    outboundIntf = outboundInterface(transport);
                } else {
                    transport = "udp";
                    outboundIntf = outboundInterface(transport);
                }
                if (outboundIntf == null) {
                    String errMsg = "The outbound interface for transport: "+transport+" is NULL, something is wrong with container, cannot proceed to call client "+request.to();
                    logger.error(errMsg);
                    sendNotification(errMsg, 11008, "error", true);
                    sender.tell(new CallManagerResponse<ActorRef>(new NullPointerException(errMsg), this.createCallRequest), self());
                    return;
                }
                if (request.from() != null && request.from().contains("@")) {
                    // https://github.com/Mobicents/RestComm/issues/150 if it contains @ it means this is a sip uri and we allow
                    // to use it directly
                    from = (SipURI) sipFactory.createURI(request.from());
                } else if (request.from() != null) {
                    if (outboundIntf != null) {
                        from = sipFactory.createSipURI(request.from(), mediaExternalIp + ":" + outboundIntf.getPort());
                    } else {
                        logger.error("Outbound interface is null, cannot create From header to be used to Dial client: "+client);
                    }
                } else {
                    from = outboundIntf;
                }
                final String location = registration.getLocation();
                to = (SipURI) sipFactory.createURI(location);
                webRTC = registration.isWebRTC();
                if (from == null || to == null) {
                    //In case From or To are null we have to cancel outbound call and hnagup initial call if needed
                    final String errMsg = "From and/or To are null, we cannot proceed to the outbound call to: "+request.to();
                    logger.warning(errMsg);
                    sender.tell(new CallManagerResponse<ActorRef>(new NullPointerException(errMsg), this.createCallRequest), self());
                } else {
                    calls.add(createOutbound(request,from,to,webRTC));
                }
            }
            if (calls.size() > 0) {
                sender.tell(new CallManagerResponse<List<ActorRef>>(calls), self());
            }
        } else {
            String errMsg = "The SIP Client "+request.to()+" is not registered or does not exist";
            logger.warning(errMsg);
            sendNotification(errMsg, 11008, "error", true);
            sender.tell(new CallManagerResponse<ActorRef>(new NullPointerException(errMsg), this.createCallRequest), self());
        }
    }

    private void outboundToPstn(final CreateCall request, final ActorRef sender) throws ServletParseException {
        final String uri = activeProxy;
        SipURI outboundIntf = null;
        SipURI from = null;
        SipURI to = null;

        final Configuration runtime = configuration.subset("runtime-settings");
        final boolean useLocalAddressAtFromHeader = runtime.getBoolean("use-local-address", false);

        final String proxyUsername = (request.username() != null) ? request.username() : activeProxyUsername;

        if (uri != null) {
            try {
                to = sipFactory.createSipURI(request.to(), uri);
                String transport = (to.getTransportParam() != null) ? to.getTransportParam() : "udp";
                outboundIntf = outboundInterface(transport);
                final boolean outboudproxyUserAtFromHeader = runtime.subset("outbound-proxy").getBoolean(
                        "outboudproxy-user-at-from-header");
                if (request.from() != null && request.from().contains("@")) {
                    // https://github.com/Mobicents/RestComm/issues/150 if it contains @ it means this is a sip uri and we allow
                    // to use it directly
                    from = (SipURI) sipFactory.createURI(request.from());
                } else if (useLocalAddressAtFromHeader) {
                    from = sipFactory.createSipURI(request.from(), mediaExternalIp + ":" + outboundIntf.getPort());
                } else {
                    if (outboudproxyUserAtFromHeader) {
                        // https://telestax.atlassian.net/browse/RESTCOMM-633. Use the outbound proxy username as the userpart
                        // of the sip uri for the From header
                        from = (SipURI) sipFactory.createSipURI(proxyUsername, uri);
                    } else {
                        from = sipFactory.createSipURI(request.from(), uri);
                    }
                }
                if (((SipURI) from).getUser() == null || ((SipURI) from).getUser() == "") {
                    if (uri != null) {
                        from = sipFactory.createSipURI(request.from(), uri);
                    } else {
                        from = (SipURI) sipFactory.createURI(request.from());
                    }
                }
            } catch (Exception exception) {
                sender.tell(new CallManagerResponse<ActorRef>(exception, this.createCallRequest), self());
            }
            if (from == null || to == null) {
                //In case From or To are null we have to cancel outbound call and hnagup initial call if needed
                final String errMsg = "From and/or To are null, we cannot proceed to the outbound call to: "+request.to();
                logger.warning(errMsg);
                sender.tell(new CallManagerResponse<ActorRef>(new NullPointerException(errMsg), this.createCallRequest), self());
            } else {
                sender.tell(new CallManagerResponse<ActorRef>(createOutbound(request,from,to,false)), self());
            }
        } else {
            String errMsg = "Cannot create call to: "+request.to()+". The Active Outbound Proxy is null. Please check configuration";
            logger.warning(errMsg);
            sendNotification(errMsg, 11008, "error", true);
            sender.tell(new CallManagerResponse<ActorRef>(new NullPointerException(errMsg), this.createCallRequest), self());
        }
    }


    private void outboundToSip(final CreateCall request, final ActorRef sender) throws ServletParseException {
        SipURI outboundIntf = null;
        SipURI from = null;
        SipURI to = null;

        to = (SipURI) sipFactory.createURI(request.to());
        String transport = (to.getTransportParam() != null) ? to.getTransportParam() : "udp";
        outboundIntf = outboundInterface(transport);
        if (request.from() == null) {
            from = outboundInterface(transport);
        } else {
            if (request.from() != null && request.from().contains("@")) {
                // https://github.com/Mobicents/RestComm/issues/150 if it contains @ it means this is a sip uri and we
                // allow to use it directly
                from = (SipURI) sipFactory.createURI(request.from());
            } else {
                from = sipFactory.createSipURI(request.from(), outboundIntf.getHost() + ":" + outboundIntf.getPort());
            }
        }
        if (from == null || to == null) {
            //In case From or To are null we have to cancel outbound call and hnagup initial call if needed
            final String errMsg = "From and/or To are null, we cannot proceed to the outbound call to: "+request.to();
            logger.warning(errMsg);
            sender.tell(new CallManagerResponse<ActorRef>(new NullPointerException(errMsg), this.createCallRequest), self());
        } else {
            sender.tell(new CallManagerResponse<ActorRef>(createOutbound(request,from,to,false)), self());
        }
    }


    private ActorRef createOutbound(final CreateCall request, final SipURI from, final SipURI to, final boolean webRTC) {
        final Configuration runtime = configuration.subset("runtime-settings");
        final String proxyUsername = (request.username() != null) ? request.username() : activeProxyUsername;
        final String proxyPassword = (request.password() != null) ? request.password() : activeProxyPassword;

        final ActorRef call = call();
        final ActorRef self = self();
        final boolean userAtDisplayedName = runtime.subset("outbound-proxy").getBoolean("user-at-displayed-name");
        InitializeOutbound init;
        if (request.from() != null && !request.from().contains("@") && userAtDisplayedName) {
            init = new InitializeOutbound(request.from(), from, to, proxyUsername, proxyPassword, request.timeout(),
                    request.isFromApi(), runtime.getString("api-version"), request.accountId(), request.type(), storage, webRTC);
        } else {
            init = new InitializeOutbound(null, from, to, proxyUsername, proxyPassword, request.timeout(), request.isFromApi(),
                    runtime.getString("api-version"), request.accountId(), request.type(), storage, webRTC);
        }
        if (request.parentCallSid() != null) {
            init.setParentCallSid(request.parentCallSid());
        }
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
        SipSession linkedB2BUASession = B2BUAHelper.getLinkedSession(request);
        if (originalRequest != null) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("B2BUA: Got CANCEL request: \n %s", request));
            }
            // SipServletRequest cancel = originalRequest.createCancel();
            request.getSession().setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, request);
            String sessionState = linkedB2BUASession.getState().name();
            SipServletResponse lastFinalResponse = (SipServletResponse) originalRequest.getSession().getAttribute(
                    B2BUAHelper.B2BUA_LAST_FINAL_RESPONSE);

            if ((sessionState == SipSession.State.INITIAL.name() || sessionState == SipSession.State.EARLY.name())
                    && !(lastFinalResponse != null && (lastFinalResponse.getStatus() == 401 || lastFinalResponse.getStatus() == 407))) {
                SipServletRequest clonedCancel = originalRequest.createCancel();
                linkedB2BUASession.setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, clonedCancel);
                clonedCancel.send();
            } else {
                SipServletRequest clonedBye = linkedB2BUASession.createRequest("BYE");
                linkedB2BUASession.setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, clonedBye);
                clonedBye.send();
            }
            // SipServletRequest cancel = originalRequest.createCancel();
            // cancel.send();
            // originalRequest.createCancel().send();
        } else {
            final ActorRef call = (ActorRef) application.getAttribute(Call.class.getName());
            if (call != null)
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

            //Prepare the BYE request to the linked session
            request.getSession().setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, request);
            SipServletRequest clonedBye = linkedB2BUASession.createRequest("BYE");
            linkedB2BUASession.setAttribute(B2BUAHelper.B2BUA_LAST_REQUEST, clonedBye);

            if (patchForNatB2BUASessions) {
                // Issue #307: https://telestax.atlassian.net/browse/RESTCOMM-307
                SipURI toInetUri = (SipURI) request.getSession().getAttribute(B2BUAHelper.TO_INET_URI);
                SipURI fromInetUri = (SipURI) request.getSession().getAttribute(B2BUAHelper.FROM_INET_URI);
                InetAddress byeRURI = null;
                try {
                    byeRURI = InetAddress.getByName(((SipURI) clonedBye.getRequestURI()).getHost());
                } catch (UnknownHostException e) {
                }
                boolean isBehindLB = false;
                final String initialIpBeforeLB = request.getHeader("X-Sip-Balancer-InitialRemoteAddr");
                String initialPortBeforeLB = request.getHeader("X-Sip-Balancer-InitialRemotePort");
                if (initialIpBeforeLB != null) {
                    if (initialPortBeforeLB == null)
                        initialPortBeforeLB = "5060";
                    if (logger.isDebugEnabled()) {
                        logger.debug("We are behind load balancer, checking if the request URI needs to be patched");
                    }
                    isBehindLB = true;
                }
                if(logger.isDebugEnabled()) {
                    logger.debug("toInetUri: " + toInetUri + " fromInetUri: " + fromInetUri + " byeRURI: " + byeRURI + " initialIpBeforeLB: " + initialIpBeforeLB
                    + " initialPortBeforeLB: " + initialPortBeforeLB);
                }
                if (toInetUri != null && byeRURI == null) {
                    if(logger.isInfoEnabled()) {
                        logger.info("Using the real To inet ip address of the sip client " + toInetUri.toString()
                        + " as a request uri of the CloneBye request");
                    }
                    clonedBye.setRequestURI(toInetUri);
                } else if (toInetUri != null
                        && (byeRURI.isSiteLocalAddress() || byeRURI.isAnyLocalAddress() || byeRURI.isLoopbackAddress())) {
                    if(logger.isInfoEnabled()) {
                        logger.info("Using the real To inet ip address of the sip client " + toInetUri.toString()
                        + " as a request uri of the CloneBye request");
                    }
                    clonedBye.setRequestURI(toInetUri);
                } else if (fromInetUri != null
                        && (byeRURI.isSiteLocalAddress() || byeRURI.isAnyLocalAddress() || byeRURI.isLoopbackAddress())) {
                    if(isBehindLB) {
                        // https://github.com/RestComm/Restcomm-Connect/issues/1357
                        boolean patchRURI = isLBPatchRURI(clonedBye, initialIpBeforeLB, initialPortBeforeLB);
                        if(patchRURI) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("We are behind load balancer, but Using the real ip address of the sip client " + fromInetUri.toString()
                                    + " as a request uri of the CloneBye request");
                            }
                            clonedBye.setRequestURI(fromInetUri);
                        }
                    } else {
                        if(logger.isInfoEnabled()) {
                            logger.info("Using the real From inet ip  address of the sip client " + fromInetUri.toString()
                            + " as a request uri of the CloneBye request");
                        }
                        clonedBye.setRequestURI(fromInetUri);
                    }
                } else if (toInetUri == null
                        && (byeRURI.isSiteLocalAddress() || byeRURI.isAnyLocalAddress() || byeRURI.isLoopbackAddress())) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Public IP toInetUri from SipSession is null, will check LB headers from last Response");
                    }
                    if(isBehindLB) {
                        String realIP = initialIpBeforeLB + ":" + initialPortBeforeLB;
                        SipURI uri = sipFactory.createSipURI(null, realIP);
                        boolean patchRURI = isLBPatchRURI(clonedBye, initialIpBeforeLB, initialPortBeforeLB);
                        if(patchRURI) {
                            if(logger.isDebugEnabled()) {
                                logger.debug("We are behind load balancer, will use: " + initialIpBeforeLB + ":"
                                        + initialPortBeforeLB + " for the cloned BYE message");
                            }
                            clonedBye.setRequestURI(uri);
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("We are behind load balancer, will use Initial Remote Address " + initialIpBeforeLB + ":"
                                    + initialPortBeforeLB + " for the cloned BYE request");
                        }
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info("LB Headers are also null");
                        }
                    }
                }
            }
            B2BUAHelper.updateCDR(request, CallStateChanged.State.COMPLETED);
            //Prepare 200 OK for received BYE
            SipServletResponse okay = request.createResponse(Response.OK);
            okay.send();
            //Send the Cloned BYE
            if(logger.isInfoEnabled()) {
                logger.info(String.format("B2BUA: Will send out Cloned BYE request: \n %s", clonedBye));
            }
            clonedBye.send();
        } else {
            final ActorRef call = (ActorRef) application.getAttribute(Call.class.getName());
            if (call != null)
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
            if (response.getStatus() == SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED
                    || response.getStatus() == SipServletResponse.SC_UNAUTHORIZED) {
                AuthInfo authInfo = sipFactory.createAuthInfo();
                String authHeader = response.getHeader("Proxy-Authenticate");
                if (authHeader == null) {
                    authHeader = response.getHeader("WWW-Authenticate");
                }
                String tempRealm = authHeader.substring(authHeader.indexOf("realm=\"") + "realm=\"".length());
                String realm = tempRealm.substring(0, tempRealm.indexOf("\""));
                authInfo.addAuthInfo(response.getStatus(), realm, activeProxyUsername, activeProxyPassword);
                SipServletRequest challengeRequest = response.getSession().createRequest(response.getRequest().getMethod());
                response.getSession().setAttribute(B2BUAHelper.B2BUA_LAST_FINAL_RESPONSE, response);
                challengeRequest.addAuthHeader(response, authInfo);
                SipServletRequest invite = response.getRequest();
                challengeRequest.setContent(invite.getContent(), invite.getContentType());
                invite = challengeRequest;
                challengeRequest.send();
            } else {
                B2BUAHelper.forwardResponse(response, patchForNatB2BUASessions);
            }
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
        final String callPath = getCall.getIdentifier();

        final ActorContext context = getContext();

        // TODO: The context.actorFor has been depreciated for actorSelection at the latest Akka release.
        ActorRef call = null;
        if (callPath != null) {
            try {
                call = context.actorFor(callPath);
            } catch (Exception e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Problem during call lookup, callPath: "+callPath);
                }
                return null;
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("CallPath is null, call lookup cannot be executed");
            }
        }
        return call;
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
                if(logger.isInfoEnabled()) {
                    logger.info("A total number of " + failures + " failures have now been counted.");
                }

                if (failures >= maxNumberOfFailedCalls) {
                    if(logger.isInfoEnabled()) {
                        logger.info("Max number of failed calls has been reached trying to switch over proxy.");
                        logger.info("Current proxy: " + getActiveProxy().get("ActiveProxy"));
                    }
                    switchProxy();
                    if(logger.isInfoEnabled()) {
                        logger.info("Switched to proxy: " + getActiveProxy().get("ActiveProxy"));
                    }
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
        // Sid callSid = new Sid("CA00000000000000000000000000000000");
        if (createCallRequest != null) {
            accountId = createCallRequest.accountId();
        } else if (switchProxyRequest != null) {
            accountId = switchProxyRequest.getSid();
        } else {
            accountId = new Sid("ACae6e420f425248d6a26948c17a9e2acf");
        }

        final Notification.Builder builder = Notification.builder();
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        builder.setSid(sid);
        builder.setAccountSid(accountId);
        // builder.setCallSid(callSid);
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

    private boolean isFromIms(final SipServletRequest request) throws ServletParseException {
        SipURI uri = (SipURI)request.getRequestURI();
        return uri.getUser() == null;
    }

    private Registration findRegistration(javax.servlet.sip.URI regUri){
        if(regUri == null){
            return null;
        }
        String formattedNumber = null;
        if (regUri.isSipURI()) {
            formattedNumber = ((SipURI)regUri).getUser().replaceFirst("\\+","");
        } else {
            formattedNumber = ((TelURL)regUri).getPhoneNumber().replaceFirst("\\+","");
        }
        if(logger.isInfoEnabled()) {
            logger.info("looking for registrations for number: " + formattedNumber);
        }
        final RegistrationsDao registrationsDao = storage.getRegistrationsDao();
        List<Registration> registrations = registrationsDao.getRegistrations(formattedNumber);

        if(registrations == null || registrations.size() ==0){
            return null;
        }
        else{
            return registrations.get(0);
        }
    }

    private void imsProxyThroughMediaServer(final SipServletRequest request, final Client client,
            final javax.servlet.sip.URI destUri, final String user, final String password, boolean isFromIms)
                    throws IOException {
        javax.servlet.sip.URI srcUri = request.getFrom().getURI();
        if(logger.isInfoEnabled()) {
            logger.info("imsProxyThroughMediaServer, isFromIms: " + isFromIms +
                    ", destUri: " + destUri + ", srcUri: " + srcUri);
        }
        final Configuration runtime = configuration.subset("runtime-settings");
        String destNumber = destUri.toString();
        String rcml = "<Response><Dial>" + destNumber + "</Dial></Response>";

        javax.servlet.sip.URI regUri = null;
        if (isFromIms) {
            regUri = destUri;
        } else {
            regUri = srcUri;
        }

        Registration reg = findRegistration(regUri);
        if (reg == null) {
            if(logger.isInfoEnabled()) {
                logger.info("registrations not found");
            }
            final SipServletResponse response = request.createResponse(SC_NOT_FOUND);
            response.send();
            // We didn't find anyway to handle the call.
            String errMsg = "Call cannot be processed because the registration: " + regUri.toString()
                    + "cannot be found";
            sendNotification(errMsg, 11005, "error", true);
            return;
        } else {
            if (isFromIms) {
                rcml = "<Response><Dial><Client>" + reg.getUserName() + "</Client></Dial></Response>";
            }
            boolean isFromWebRTC = false;
            if (!isFromIms) {
                isFromWebRTC = reg.isWebRTC();
            } else {
                Registration srcReg = findRegistration(srcUri);
                if (srcReg != null) {
                    isFromWebRTC = srcReg.isWebRTC();
                }
            }
            if(logger.isInfoEnabled()) {
                logger.info("isFromWebRTC: " + isFromWebRTC);
            }

            final VoiceInterpreterBuilder builder = new VoiceInterpreterBuilder(system);
            builder.setConfiguration(configuration);
            builder.setStorage(storage);
            builder.setCallManager(self());
            builder.setConferenceManager(conferences);
            builder.setBridgeManager(bridges);
            builder.setSmsService(sms);
            builder.setAccount(Sid.generate(Sid.Type.APPLICATION));
            builder.setVersion(runtime.getString("api-version"));
            builder.setRcml(rcml);
            builder.setMonitoring(monitoring);
            builder.setAsImsUa(actAsImsUa);
            if (actAsImsUa) {
                builder.setImsUaLogin(user);
                builder.setImsUaPassword(password);
            }
            final ActorRef interpreter = builder.build();
            //TODO KM
            //final ActorRef call = call(isFromWebRTC, true);
            final ActorRef call = call();
            final SipApplicationSession application = request.getApplicationSession();
            application.setAttribute(Call.class.getName(), call);
            call.tell(request, self());
            interpreter.tell(new StartInterpreter(call), self());
        }
    }

    private void outboundToIms(final CreateCall request, final ActorRef sender) throws ServletParseException {
        if (logger.isInfoEnabled()) {
            logger.info("outboundToIms: "+request);
        }
        SipURI from;
        SipURI to;

        to = (SipURI) sipFactory.createURI(request.to());
        if (request.from() == null) {
            from = sipFactory.createSipURI(null, imsDomain);
        } else {
            if (request.from() != null && request.from().contains("@")) {
                // https://github.com/Mobicents/RestComm/issues/150 if it contains @ it means this is a sip uri and we
                // allow to use it directly
                from = (SipURI) sipFactory.createURI(request.from());
            } else {
                from = sipFactory.createSipURI(request.from(), imsDomain);
            }
        }
        if (from == null || to == null) {
            //In case From or To are null we have to cancel outbound call and hnagup initial call if needed
            final String errMsg = "From and/or To are null, we cannot proceed to the outbound call to: "+request.to();
            logger.error(errMsg);
            sender.tell(new CallManagerResponse<ActorRef>(new NullPointerException(errMsg), this.createCallRequest), self());
        } else {
            final ActorRef call = call();
            final ActorRef self = self();
            final Configuration runtime = configuration.subset("runtime-settings");
            if (logger.isInfoEnabled()) {
                logger.info("outboundToIms: from: "+from +", to: " + to);
            }
            final String proxyUsername = (request.username() != null) ? request.username() : activeProxyUsername;
            final String proxyPassword = (request.password() != null) ? request.password() : activeProxyPassword;
            boolean isToWebRTC = false;
            Registration toReg = findRegistration(to);
            if(toReg !=null){
                isToWebRTC = toReg.isWebRTC();
            }
            if (logger.isInfoEnabled()) {
                logger.info("outboundToIms: isToWebRTC: "+isToWebRTC);
            }
            InitializeOutbound init = new InitializeOutbound(request.from(), from, to, proxyUsername, proxyPassword, request.timeout(),
                    request.isFromApi(), runtime.getString("api-version"), request.accountId(), request.type(), storage, isToWebRTC,
                    true, imsProxyAddress, imsProxyPort);
            if (request.parentCallSid() != null) {
                init.setParentCallSid(request.parentCallSid());
            }
            call.tell(init, self);
            sender.tell(new CallManagerResponse<ActorRef>(call), self());
        }
    }
}
