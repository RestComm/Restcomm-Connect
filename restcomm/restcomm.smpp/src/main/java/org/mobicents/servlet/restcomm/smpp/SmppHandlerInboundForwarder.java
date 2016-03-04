package org.mobicents.servlet.restcomm.smpp;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.CreateSmppSession;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.DestroySmppSession;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppServiceResponse;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppStartInterpreter;
import org.mobicents.servlet.restcomm.util.UriUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.telestax.servlet.MonitoringService;

public class SmppHandlerInboundForwarder extends UntypedActor {

    private static final Logger logger = Logger.getLogger(SmppHandlerInboundForwarder.class);
    private final DaoManager storage = SmppInitConfigurationDetails.getStorage();
    final AccountsDao accounts = storage.getAccountsDao();
    final ApplicationsDao applications = storage.getApplicationsDao();
    private final ActorSystem system = SmppInitConfigurationDetails.getSystem();
    private final Configuration configuration = SmppInitConfigurationDetails.getConfiguration();
    private final SipFactory sipFactory = SmppInitConfigurationDetails.getSipFactory();
    private final ActorRef monitoringService = (ActorRef) SmppInitConfigurationDetails.getServletContext().getAttribute(MonitoringService.class.getName());
    private final ServletContext servletContext = SmppInitConfigurationDetails.getServletContext() ;


    private void HandleInboundMessages(final SmppInboundMessageEntity request ) throws IOException {
        final ActorRef self = self();

        String to = request.getSmppTo();
        final IncomingPhoneNumbersDao numbers = storage.getIncomingPhoneNumbersDao();
        IncomingPhoneNumber number = numbers.getIncomingPhoneNumber(to);
        if (number == null) {
            logger.error("There is no matching Restcomm registered number to handle inbound SMPP number : " + to );
            //number = numbers.getIncomingPhoneNumber(to);
            return;
        }
        if (number.getSmsUrl() == null){
            logger.error("A matching Registered Restcomm number is found, but no SMS URL App is attached. " );
            return;
        }

        URI appUri = number.getSmsUrl();

        if (appUri != null){
            //final String toUser = CallControlHelper.getUserSipId(request, useTo);
            if( redirectToHostedSmsApp(self,request, accounts, applications,to  )){
                logger.info("SMPP Message Accepted - A Restcomm Hosted App is Found for Number : " + number.getPhoneNumber() );
            }
            else {
                logger.error("SMPP Message Rejected : No Restcomm Hosted App Found for inbound number : " + number );
            }
        }
    }


    private boolean redirectToHostedSmsApp(final ActorRef self, final SmppInboundMessageEntity request, final AccountsDao accounts,
            final ApplicationsDao applications, String id) throws IOException {
        boolean isFoundHostedApp = false;

        String to = request.getSmppTo();
        String phone = to;

        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

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
                    final SmppInterpreterBuilder builder = new SmppInterpreterBuilder(system);
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
                    builder.setMethod(number.getSmsMethod());
                    URI appFallbackUrl = number.getSmsFallbackUrl();
                    if (appFallbackUrl != null) {
                        builder.setFallbackUrl(UriUtils.resolve(number.getSmsFallbackUrl()));
                        builder.setFallbackMethod(number.getSmsFallbackMethod());
                    }
                    interpreter = builder.build();
                }
                final ActorRef session = session();
                session.tell(request, self);
                final SmppStartInterpreter start = new SmppSessionObjects().new SmppStartInterpreter(session);
                interpreter.tell(start, self);
                isFoundHostedApp = true;

            }
        } catch (Exception e) {
            logger.error("Error processing inbound SMPP Message. There is no locally hosted Restcomm app for the number :" + e);

        }
        return isFoundHostedApp;
    }


    @Override
    public void onReceive(Object request) throws Exception {

        final UntypedActorContext context = getContext();
        final ActorRef sender = sender();
        final ActorRef self = self();
        if( request instanceof SmppInboundMessageEntity){
            HandleInboundMessages((SmppInboundMessageEntity) request);
        }else if (request instanceof CreateSmppSession) {
            final ActorRef session = session();
            final SmppServiceResponse<ActorRef> response = new  SmppSessionObjects().new SmppServiceResponse<ActorRef>(session);
            sender.tell(response, self);
        }else if (request instanceof DestroySmppSession ) {
            final DestroySmppSession message = (DestroySmppSession) request;
            final ActorRef session = message.session();
            context.stop(session);
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

    private ActorRef session() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                Configuration smsConfiguration = configuration.subset("sms-aggregator");
                //return new SmppSessionHandler();
                return new SmppSessionHandler(smsConfiguration, sipFactory, outboundInterface(), storage, monitoringService);
            }
        }));
    }



}
