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
package org.restcomm.connect.ussd.telephony;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.util.UriUtils;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.ApplicationsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.IncomingPhoneNumbersDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Application;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.Organization;
import org.restcomm.connect.interpreter.StartInterpreter;
import org.restcomm.connect.telephony.api.CallManagerResponse;
import org.restcomm.connect.telephony.api.CreateCall;
import org.restcomm.connect.telephony.api.ExecuteCallScript;
import org.restcomm.connect.telephony.api.InitializeOutbound;
import org.restcomm.connect.telephony.api.util.CallControlHelper;
import org.restcomm.connect.ussd.interpreter.UssdInterpreter;
import org.restcomm.connect.ussd.interpreter.UssdInterpreterBuilder;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import javax.servlet.ServletContext;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import static javax.servlet.sip.SipServlet.OUTBOUND_INTERFACES;
import static javax.servlet.sip.SipServletResponse.SC_BAD_REQUEST;
import static javax.servlet.sip.SipServletResponse.SC_NOT_FOUND;
import static javax.servlet.sip.SipServletResponse.SC_OK;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
public class UssdCallManager extends UntypedActor {

    static final int ERROR_NOTIFICATION = 0;
    static final int WARNING_NOTIFICATION = 1;
    static final Pattern PATTERN = Pattern.compile("[\\*#0-9]{1,12}");

    private final Configuration configuration;
    private final ServletContext context;
    private final SipFactory sipFactory;
    private final DaoManager storage;
    private CreateCall createCallRequest;
    private final String ussdGatewayUri;
    private final String ussdGatewayUsername;
    private final String ussdGatewayPassword;

    // configurable switch whether to use the To field in a SIP header to determine the callee address
    // alternatively the Request URI can be used
    private boolean useTo;

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final ActorSystem system;
    private final String defaultOrganization;

    /**
     * @param configuration
     * @param context
     * @param conferences
     * @param sms
     * @param factory
     * @param storage
     */
    public UssdCallManager(final ActorSystem system, Configuration configuration, ServletContext context,
                           ActorRef conferences, ActorRef sms, SipFactory factory, DaoManager storage) {
        super();
        this.system = system;
        this.configuration = configuration;
        this.context = context;
        this.sipFactory = factory;
        this.storage = storage;
        final Configuration runtime = configuration.subset("runtime-settings");
        final Configuration ussdGatewayConfig = runtime.subset("ussd-gateway");
        this.ussdGatewayUri = ussdGatewayConfig.getString("ussd-gateway-uri");
        this.ussdGatewayUsername = ussdGatewayConfig.getString("ussd-gateway-user");
        this.ussdGatewayPassword = ussdGatewayConfig.getString("ussd-gateway-password");
        defaultOrganization = (String) context.getAttribute("defaultOrganization");
    }

    private ActorRef ussdCall() {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public UntypedActor create() throws Exception {
                return new UssdCall(sipFactory);
            }
        });
        return system.actorOf(props);
    }

    private void check(final Object message) throws IOException {
        final SipServletRequest request = (SipServletRequest) message;
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
                processRequest(request);
            } else if ("ACK".equalsIgnoreCase(method)) {
                processRequest(request);
            } else if("BYE".equalsIgnoreCase(method)) {
                processRequest(request);
            } else if("CANCEL".equalsIgnoreCase(method)) {
                processRequest(request);
            }
        } else if (message instanceof SipServletResponse) {
            response(message);
        } else if (CreateCall.class.equals(klass)) {
            try {
                this.createCallRequest = (CreateCall) message;
                sender.tell(new CallManagerResponse<ActorRef>(outbound(message)), self);
            } catch (final Exception exception) {
                sender.tell(new CallManagerResponse<ActorRef>(exception), self);
            }
        } else if (ExecuteCallScript.class.equals(klass)) {
            execute(message);
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
            number = getMostOptimalIncomingPhoneNumber(request, id);
            if (number != null) {
                final UssdInterpreterBuilder builder = new UssdInterpreterBuilder(system);
                builder.setConfiguration(configuration);
                builder.setStorage(storage);
                builder.setCallManager(self);
                builder.setAccount(number.getAccountSid());
                builder.setVersion(number.getApiVersion());
                final Account account = accounts.getAccount(number.getAccountSid());
                builder.setEmailAddress(account.getEmailAddress());
                final Sid sid = number.getUssdApplicationSid();
                if (sid != null) {
                    final Application application = applications.getApplication(sid);
                    builder.setUrl(UriUtils.resolve(application.getRcmlUrl()));
                } else {
                    builder.setUrl(UriUtils.resolve(number.getUssdUrl()));
                }
                final String ussdMethod = number.getUssdMethod();
                if (ussdMethod == null || ussdMethod.isEmpty()) {
                    builder.setMethod("POST");
                } else {
                    builder.setMethod(ussdMethod);
                }
                if (number.getUssdFallbackUrl() != null)
                    builder.setFallbackUrl(number.getUssdFallbackUrl());
                builder.setFallbackMethod(number.getUssdFallbackMethod());
                builder.setStatusCallback(number.getStatusCallback());
                builder.setStatusCallbackMethod(number.getStatusCallbackMethod());
                final ActorRef ussdInterpreter = builder.build();
                final ActorRef ussdCall = ussdCall();
                ussdCall.tell(request, self);

                ussdInterpreter.tell(new StartInterpreter(ussdCall), self);

                SipApplicationSession applicationSession = request.getApplicationSession();
                applicationSession.setAttribute("UssdCall","true");
                applicationSession.setAttribute(UssdInterpreter.class.getName(), ussdInterpreter);
                applicationSession.setAttribute(UssdCall.class.getName(), ussdCall);
                isFoundHostedApp = true;
            } else {
                logger.info("USSD Number registration NOT FOUND");
                request.createResponse(SipServletResponse.SC_NOT_FOUND).send();
            }
        }
        return isFoundHostedApp;
    }

    private void processRequest(SipServletRequest request) throws IOException {
        final ActorRef ussdInterpreter = (ActorRef) request.getApplicationSession().getAttribute(UssdInterpreter.class.getName());
        if(ussdInterpreter != null) {
            logger.info("Dispatching Request: "+request.getMethod()+" to UssdInterpreter: "+ussdInterpreter);
            ussdInterpreter.tell(request, self());
        } else {
            final SipServletResponse notFound = request.createResponse(SipServletResponse.SC_NOT_FOUND);
            notFound.send();
        }
    }

    private ActorRef outbound(final Object message) throws ServletParseException {
        final CreateCall request = (CreateCall) message;
        final Configuration runtime = configuration.subset("runtime-settings");
        final String uri = ussdGatewayUri;
        final String ussdUsername = (request.username() != null) ? request.username() : ussdGatewayUsername;
        final String ussdPassword = (request.password() != null) ? request.password() : ussdGatewayPassword;

        SipURI from = (SipURI)sipFactory.createSipURI(request.from(), uri);
        SipURI to = (SipURI)sipFactory.createSipURI(request.to(), uri);

        String transport = (to.getTransportParam() != null) ? to.getTransportParam() : "udp";
        //from = outboundInterface(transport);
        SipURI obi = outboundInterface(transport);
        from = (obi == null) ? from : obi;

        final ActorRef ussdCall = ussdCall();
        final ActorRef self = self();
        final InitializeOutbound init = new InitializeOutbound(null, from, to, ussdUsername, ussdPassword, request.timeout(),
                request.isFromApi(), runtime.getString("api-version"), request.accountId(), request.type(), storage, false);
        ussdCall.tell(init, self);
        return ussdCall;
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

    private void execute(final Object message) {
        final ExecuteCallScript request = (ExecuteCallScript) message;
        final ActorRef self = self();
        final UssdInterpreterBuilder builder = new UssdInterpreterBuilder(system);
        builder.setConfiguration(configuration);
        builder.setStorage(storage);
        builder.setCallManager(self);
        builder.setAccount(request.account());
        builder.setVersion(request.version());
        builder.setUrl(request.url());
        builder.setMethod(request.method());
        builder.setFallbackUrl(request.fallbackUrl());
        builder.setFallbackMethod(request.fallbackMethod());
        final ActorRef interpreter = builder.build();
        interpreter.tell(new StartInterpreter(request.call()), self);
    }

    public void response(final Object message) throws IOException {
        final ActorRef self = self();
        final SipServletResponse response = (SipServletResponse) message;
        final SipApplicationSession application = response.getApplicationSession();
        if (application.isValid()) {
            // otherwise the response is coming back to a Voice app hosted by Restcomm
            final ActorRef ussdCall = (ActorRef) application.getAttribute(UssdCall.class.getName());
            ussdCall.tell(response, self);
        }
    }

    /**
     * @param SipServletRequest
     * @param phone
     * @return
     */
    private IncomingPhoneNumber getMostOptimalIncomingPhoneNumber(final SipServletRequest request, String phone) {
        // Format the destination to an E.164 phone number.
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        String formatedPhone = null;
        try {
            formatedPhone = phoneNumberUtil.format(phoneNumberUtil.parse(phone, "US"), PhoneNumberFormat.E164);
        } catch (Exception e) {
        }
        List<IncomingPhoneNumber> numbers = null;
        IncomingPhoneNumber number = null;
        // Try to find an application defined for the phone number.
        final IncomingPhoneNumbersDao numbersDao = storage.getIncomingPhoneNumbersDao();
        //get all number with same number, by both formatedPhone and unformatedPhone
        numbers = numbersDao.getIncomingPhoneNumber(formatedPhone);
        numbers.addAll(numbersDao.getIncomingPhoneNumber(phone));
        if (phone.startsWith("+")) {
            //remove the (+) and check if exists
            phone= phone.replaceFirst("\\+","");
            numbers.addAll(numbersDao.getIncomingPhoneNumber(phone));
        } else {
            //Add "+" add check if number exists
            phone = "+".concat(phone);
            numbers.addAll(numbersDao.getIncomingPhoneNumber(phone));
        }
        if(numbers.isEmpty()){
            // https://github.com/Mobicents/RestComm/issues/84 using wildcard as default application
            numbers.addAll(numbersDao.getIncomingPhoneNumber("*"));
        }
        if(!numbers.isEmpty()){
            boolean foundNumberInSameOrganization = false;
            boolean foundNonSipNumberInDifferntOrganization = false;
            Sid organizationSid = getOrganizationSidBySipURIHost((SipURI)request.getTo().getURI());
            // find number in same organization
            for(IncomingPhoneNumber n : numbers){
                if(n.getOrganizationSid() == organizationSid){
                    foundNumberInSameOrganization = true;
                    number = n;
                }
                if(foundNumberInSameOrganization)
                    break;
            }
            /* if number is not found in same organization
             * then find a non sip (provider) number in a different organization
             */
            if(!foundNumberInSameOrganization){
                for(IncomingPhoneNumber n : numbers){
                    if(!n.isPureSip()){
                        foundNonSipNumberInDifferntOrganization = true;
                        number = n;
                    }
                    if(foundNonSipNumberInDifferntOrganization)
                        break;
                }
            }
        }
        return number;
    }

    /**
     * getOrganizationSidBySipURIHost
     *
     * @param sipURI
     * @return Sid of Organization
     */
    private Sid getOrganizationSidBySipURIHost(final SipURI sipURI){
        final String organizationDomainName = sipURI.getHost();
        if(logger.isDebugEnabled())
            logger.debug("sipURI: "+sipURI+" | organizationDomainName: "+organizationDomainName);
        Organization organization = storage.getOrganizationsDao().getOrganizationByDomainName(organizationDomainName);
        if(logger.isDebugEnabled())
            logger.debug("organization: "+organization);
        if(organization == null){
            organization = storage.getOrganizationsDao().getOrganization(new Sid(defaultOrganization));
            if(logger.isDebugEnabled())
                logger.debug("organization is null going to choose default: "+organization);
        }
        return organization.getSid();
    }
}
