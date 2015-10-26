package org.mobicents.servlet.restcomm.smpp;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.SmsMessage;
import org.mobicents.servlet.restcomm.entities.SmsMessage.Direction;
import org.mobicents.servlet.restcomm.entities.SmsMessage.Status;
import org.mobicents.servlet.restcomm.smpp.SmppSessionObjects.SmppStartInterpreter;
import org.mobicents.servlet.restcomm.telephony.TextMessage;
import org.mobicents.servlet.restcomm.util.UriUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
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
    private boolean useTo = true;
    private final SipFactory sipFactory = SmppInitConfigurationDetails.getSipFactory();
    private final ActorRef monitoringService = (ActorRef) SmppInitConfigurationDetails.getServletContext().getAttribute(MonitoringService.class.getName());
    private final List<SipURI>  SipUriList =  (List<SipURI>) SmppInitConfigurationDetails.getServletContext().getAttribute(SipServlet.OUTBOUND_INTERFACES);
    private ConcurrentHashMap<String, String> customRequestHeaderMap = new ConcurrentHashMap<String, String>();



    private void HandleInboundMessages(final SmppInboundMessageEntity request ) throws IOException {
        final ActorRef self = self();

        String from = request.getSmppFrom();
        String to = request.getSmppTo();
        String body = request.getSmppContent();
        String phone = to;


        final ClientsDao clients = storage.getClientsDao();
        final Client client = clients.getClient(to);

        final IncomingPhoneNumbersDao numbers = storage.getIncomingPhoneNumbersDao();
        IncomingPhoneNumber number = numbers.getIncomingPhoneNumber(phone);
        if (number == null) {
            number = numbers.getIncomingPhoneNumber(to);
        }
        URI appUri = number.getSmsUrl();

        if (appUri != null){
            //final String toUser = CallControlHelper.getUserSipId(request, useTo);
            if( redirectToHostedSmsApp(self,request, accounts, applications,to  )){

                logger.info("SMPP Message Accepted - A Restcomm Hosted App is Found for Number : " + number.getPhoneNumber() );

                logger.info("from : " + from);
                logger.info("to : " + to);

                ActorRef session = session();
                // Create an SMS detail record.
                final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
                final SmsMessage.Builder builder = SmsMessage.builder();
                builder.setSid(sid);
                builder.setAccountSid(number.getAccountSid());
                builder.setApiVersion(number.getApiVersion());
                builder.setRecipient(to);
                builder.setSender(from);
                builder.setBody(body);
                builder.setDirection(Direction.INBOUND);
                builder.setStatus(Status.RECEIVED);
                builder.setPrice(new BigDecimal("0.00"));

                // TODO implement currency property to be read from Configuration
                builder.setPriceUnit(Currency.getInstance("USD"));
                final StringBuilder buffer = new StringBuilder();
                buffer.append("/").append(number.getApiVersion()).append("/Accounts/");
                buffer.append(number.getAccountSid().toString()).append("/SMS/Messages/");
                buffer.append(sid.toString());
                final URI uri = URI.create(buffer.toString());
                logger.info("URI : " + uri);
                builder.setUri(uri);
                final SmsMessage record = builder.build();
                final SmsMessagesDao messages = storage.getSmsMessagesDao();
                messages.addSmsMessage(record); //store message in DB
                //Store the sms record in the smppsessionhandler.
                session.tell(new SmppSessionObjects().new SmppSessionAttribute("record", record), self());

                // Send the SMS.
                final SmppSessionObjects.SmppSessionRequest sms = new SmppSessionObjects().new SmppSessionRequest(from, to, body , null);
                monitoringService.tell(new TextMessage(from, to, TextMessage.SmsState.INBOUND_TO_PROXY_OUT), self);
                session.tell(sms, self());
            }
            else {
                logger.error("SMPP Message Rejected : No Restcomm Hosted App Found for inbound number : " + number );
            }
        }
    }


    private boolean redirectToHostedSmsApp(final ActorRef self, final SmppInboundMessageEntity request, final AccountsDao accounts,
            final ApplicationsDao applications, String id) throws IOException {
        boolean isFoundHostedApp = false;

        String from = request.getSmppFrom();
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
                        builder.setMethod(number.getSmsMethod());
                        URI appFallbackUrl = number.getSmsFallbackUrl();
                        if (appFallbackUrl != null) {
                            builder.setFallbackUrl(UriUtils.resolve(number.getSmsFallbackUrl()));
                            builder.setFallbackMethod(number.getSmsFallbackMethod());
                        }
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
            logger.error("There was an error when trying to process inbound SMPP Message. There is no locally hosted Restcomm app for the number :" + e);

        }
        return isFoundHostedApp;
    }

    /*
    @SuppressWarnings("unchecked")
    private SipURI outboundInterface() {
        SipURI result = null;
        final List<SipURI> uris = SipUriList;
        for (final SipURI uri : uris) {
            final String transport = uri.getTransportParam();
            if ("udp".equalsIgnoreCase(transport)) {
                result = uri;
            }
        }
        return result;
    }**/

    @Override
    public void onReceive(Object request) throws Exception {
        if( request instanceof SmppInboundMessageEntity){
            // SipServletRequest convertedRequest = smppToSipConverter((SmppInboundMessageEntity) request);
            HandleInboundMessages((SmppInboundMessageEntity) request);

        }
    }

    private ActorRef session() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new SmppSessionHandler();
                //return new SmsSession(smsConfiguration, sipFactory, outboundInterface(), storage, monitoringService);
            }
        }));
    }



}
