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
package org.mobicents.servlet.restcomm.http;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.util.Timeout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.thoughtworks.xstream.XStream;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;

import static akka.pattern.Patterns.ask;
import static javax.ws.rs.core.MediaType.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.SmsMessagesDao;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.SmsMessage;
import org.mobicents.servlet.restcomm.entities.SmsMessageList;
import org.mobicents.servlet.restcomm.entities.SmsMessage.Status;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.http.converter.SmsMessageConverter;
import org.mobicents.servlet.restcomm.http.converter.SmsMessageListConverter;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.sms.CreateSmsSession;
import org.mobicents.servlet.restcomm.sms.SmsServiceResponse;
import org.mobicents.servlet.restcomm.sms.SmsSessionAttribute;
import org.mobicents.servlet.restcomm.sms.SmsSessionInfo;
import org.mobicents.servlet.restcomm.sms.SmsSessionRequest;
import org.mobicents.servlet.restcomm.sms.SmsSessionResponse;
import org.mobicents.servlet.restcomm.util.StringUtils;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe
public abstract class SmsMessagesEndpoint extends SecuredEndpoint {
    @Context
    protected ServletContext context;
    protected ActorSystem system;
    protected Configuration configuration;
    protected ActorRef aggregator;
    protected SmsMessagesDao dao;
    protected Gson gson;
    protected XStream xstream;
    //protected AccountsDao accountsDao;

    private boolean normalizePhoneNumbers;

    public SmsMessagesEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        dao = storage.getSmsMessagesDao();
        //accountsDao = storage.getAccountsDao();
        aggregator = (ActorRef) context.getAttribute("org.mobicents.servlet.restcomm.sms.SmsService");
        system = (ActorSystem) context.getAttribute(ActorSystem.class.getName());
        super.init(configuration);
        final SmsMessageConverter converter = new SmsMessageConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SmsMessage.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new SmsMessageListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));

        normalizePhoneNumbers = configuration.getBoolean("normalize-numbers-for-outbound-calls");
    }

    protected Response getSmsMessage(final String accountSid, final String sid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:SmsMessages");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final SmsMessage smsMessage = dao.getSmsMessage(new Sid(sid));
        if (smsMessage == null) {
            return status(NOT_FOUND).build();
        } else {
            if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(smsMessage), APPLICATION_JSON).build();
            } else if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(smsMessage);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else {
                return null;
            }
        }
    }

    protected Response getSmsMessages(final String accountSid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:SmsMessages");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final List<SmsMessage> smsMessages = dao.getSmsMessages(new Sid(accountSid));
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(smsMessages), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new SmsMessageList(smsMessages));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    private void normalize(final MultivaluedMap<String, String> data) throws IllegalArgumentException {
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        final String from = data.getFirst("From");
        data.remove("From");
        try {
            data.putSingle("From", phoneNumberUtil.format(phoneNumberUtil.parse(from, "US"), PhoneNumberFormat.E164));
        } catch (final NumberParseException exception) {
            throw new IllegalArgumentException(exception);
        }
        final String to = data.getFirst("To");
        data.remove("To");
        try {
            data.putSingle("To", phoneNumberUtil.format(phoneNumberUtil.parse(to, "US"), PhoneNumberFormat.E164));
        } catch (final NumberParseException exception) {
            throw new IllegalArgumentException(exception);
        }
        final String body = data.getFirst("Body");
        if (body.getBytes().length > 160) {
            data.remove("Body");
            data.putSingle("Body", body.substring(0, 159));
        }
    }

    @SuppressWarnings("unchecked")
    protected Response putSmsMessage(final String accountSid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:SmsMessages");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        try {
            validate(data);
            if(normalizePhoneNumbers)
                normalize(data);
        } catch (final RuntimeException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        final String sender = data.getFirst("From");
        final String recipient = data.getFirst("To");
        final String body = data.getFirst("Body");
        ConcurrentHashMap<String, String> customRestOutgoingHeaderMap = new ConcurrentHashMap<String, String>();
        Iterator<String> iter = data.keySet().iterator();
        while (iter.hasNext()) {
            String name = iter.next();
            if (name.startsWith("X-")){
                customRestOutgoingHeaderMap.put(name, data.getFirst(name));
            }
        }
        final Timeout expires = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        try {
            Future<Object> future = (Future<Object>) ask(aggregator, new CreateSmsSession(), expires);
            Object object = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
            Class<?> klass = object.getClass();
            if (SmsServiceResponse.class.equals(klass)) {
                final SmsServiceResponse<ActorRef> smsServiceResponse = (SmsServiceResponse<ActorRef>) object;
                if (smsServiceResponse.succeeded()) {
                    // Create an SMS record for the text message.
                    final SmsMessage record = sms(new Sid(accountSid), getApiVersion(data), sender, recipient, body,
                            SmsMessage.Status.SENDING, SmsMessage.Direction.OUTBOUND_API);
                    dao.addSmsMessage(record);
                    // Send the sms.
                    final ActorRef session = smsServiceResponse.get();
                    final ActorRef observer = observer();
                    session.tell(new Observe(observer), observer);
                    session.tell(new SmsSessionAttribute("record", record), null);
                    final SmsSessionRequest request = new SmsSessionRequest(sender, recipient, body, customRestOutgoingHeaderMap);
                    session.tell(request, null);
                    if (APPLICATION_JSON_TYPE == responseType) {
                        return ok(gson.toJson(record), APPLICATION_JSON).build();
                    } else if (APPLICATION_XML_TYPE == responseType) {
                        final RestCommResponse response = new RestCommResponse(record);
                        return ok(xstream.toXML(response), APPLICATION_XML).build();
                    } else {
                        return null;
                    }
                }
            }
            return status(INTERNAL_SERVER_ERROR).build();
        } catch (final Exception exception) {
            return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }
    }

    private SmsMessage sms(final Sid accountSid, final String apiVersion, final String sender, final String recipient,
            final String body, final SmsMessage.Status status, final SmsMessage.Direction direction) {
        final SmsMessage.Builder builder = SmsMessage.builder();
        final Sid sid = Sid.generate(Sid.Type.SMS_MESSAGE);
        builder.setSid(sid);
        builder.setAccountSid(accountSid);
        builder.setSender(sender);
        builder.setRecipient(recipient);
        builder.setBody(body);
        builder.setStatus(status);
        builder.setDirection(direction);
        builder.setPrice(new BigDecimal(0.00));
        // TODO - this needs to be added as property to Configuration somehow
        builder.setPriceUnit(Currency.getInstance("USD"));
        builder.setApiVersion(apiVersion);
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(apiVersion).append("/Accounts/");
        buffer.append(accountSid.toString()).append("/SMS/Messages/");
        buffer.append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        builder.setUri(uri);
        return builder.build();
    }

    private void validate(final MultivaluedMap<String, String> data) throws NullPointerException {
        if (!data.containsKey("From")) {
            throw new NullPointerException("From can not be null.");
        } else if (!data.containsKey("To")) {
            throw new NullPointerException("To can not be null.");
        } else if (!data.containsKey("Body")) {
            throw new NullPointerException("Body can not be null.");
        }
    }

    private ActorRef observer() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new SmsSessionObserver();
            }
        }));
    }

    private final class SmsSessionObserver extends UntypedActor {
        public SmsSessionObserver() {
            super();
        }

        @Override
        public void onReceive(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (SmsSessionResponse.class.equals(klass)) {
                final SmsSessionResponse response = (SmsSessionResponse) message;
                final SmsSessionInfo info = response.info();
                SmsMessage record = (SmsMessage) info.attributes().get("record");
                if (response.succeeded()) {
                    final DateTime now = DateTime.now();
                    record = record.setDateSent(now);
                    record = record.setStatus(Status.SENT);
                } else {
                    record = record.setStatus(Status.FAILED);
                }
                dao.updateSmsMessage(record);
                final UntypedActorContext context = getContext();
                final ActorRef self = self();
                context.stop(self);
            }
        }
    }
}
