package org.mobicents.servlet.restcomm.interpreter;

import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.play;
import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.say;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.email.api.CreateEmailService;
import org.mobicents.servlet.restcomm.email.api.EmailService;
import org.mobicents.servlet.restcomm.email.EmailRequest;
import org.mobicents.servlet.restcomm.email.Mail;
import org.mobicents.servlet.restcomm.cache.DiskCache;
import org.mobicents.servlet.restcomm.cache.DiskCacheRequest;
import org.mobicents.servlet.restcomm.cache.DiskCacheResponse;
import org.mobicents.servlet.restcomm.cache.HashGenerator;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.http.client.Downloader;
import org.mobicents.servlet.restcomm.http.client.DownloaderResponse;
import org.mobicents.servlet.restcomm.http.client.HttpRequestDescriptor;
import org.mobicents.servlet.restcomm.http.client.HttpResponseDescriptor;
import org.mobicents.servlet.restcomm.interpreter.rcml.Attribute;
import org.mobicents.servlet.restcomm.interpreter.rcml.End;
import org.mobicents.servlet.restcomm.interpreter.rcml.GetNextVerb;
import org.mobicents.servlet.restcomm.interpreter.rcml.Parser;
import org.mobicents.servlet.restcomm.interpreter.rcml.Tag;
import org.mobicents.servlet.restcomm.interpreter.rcml.Verbs;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaGroupStateChanged;
import org.mobicents.servlet.restcomm.mscontrol.messages.MediaServerControllerResponse;
import org.mobicents.servlet.restcomm.mscontrol.messages.Play;
import org.mobicents.servlet.restcomm.mscontrol.messages.StartMediaGroup;
import org.mobicents.servlet.restcomm.mscontrol.messages.StopMediaGroup;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;
import org.mobicents.servlet.restcomm.telephony.DestroyWaitUrlConfMediaGroup;
import org.mobicents.servlet.restcomm.tts.api.GetSpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerRequest;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerResponse;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class ConfVoiceInterpreter extends UntypedActor {
    private static final int ERROR_NOTIFICATION = 0;
    private static final int WARNING_NOTIFICATION = 1;
    static String EMAIL_SENDER;

    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // States for the FSM.
    private final State uninitialized;
    private final State acquiringSynthesizerInfo;
    private final State downloadingRcml;
    private final State initializingConfMediaGroup;
    private final State acquiringConfMediaGroup;
    private final State ready;
    private final State notFound;

    private final State caching;
    private final State checkingCache;
    private final State playing;
    private final State synthesizing;
    private final State redirecting;

    private final State finished;
    // FSM.
    private final FiniteStateMachine fsm;
    // The user specific configuration.
    private final Configuration configuration;
    // The block storage cache.
    private final ActorRef cache;
    private final String cachePath;
    // The downloader will fetch resources for us using HTTP.
    private final ActorRef downloader;
    // The mail man that will deliver e-mail.
    private ActorRef mailerNotify = null;

    // The storage engine.
    private final DaoManager storage;
    // The text to speech synthesizer service.
    private final ActorRef synthesizer;

    // The languages supported by the text to speech synthesizer service.
    private SpeechSynthesizerInfo synthesizerInfo;

    // The conference being handled by this interpreter.
    private ActorRef conference;
    private ActorRef conferenceMediaGroup;
    // The information for this call.
    private CallInfo callInfo;

    // The call state.
    private CallStateChanged.State callState;

    // A call detail record.
    private CallDetailRecord callRecord;

    // Information to reach the application that will be executed
    // by this interpreter.
    private final Sid accountId;
    private final String version;
    private final URI url;
    private final String method;
    private final String emailAddress;
    // application data.
    private HttpRequestDescriptor request;
    private HttpResponseDescriptor response;
    private DownloaderResponse downloaderResponse;
    // The RCML parser.
    private ActorRef parser;
    private ActorRef source;
    private Tag verb;

    private ActorRef originalInterpreter;

    public ConfVoiceInterpreter(final Configuration configuration, final Sid account, final String version, final URI url,
            final String method, final String emailAddress, final ActorRef conference, final DaoManager storage,
            final CallInfo callInfo) {

        super();

        source = self();
        uninitialized = new State("uninitialized", null, null);

        acquiringSynthesizerInfo = new State("acquiring tts info", new AcquiringSpeechSynthesizerInfo(source), null);
        acquiringConfMediaGroup = new State("acquiring call media group", new AcquiringConferenceMediaGroup(source), null);
        downloadingRcml = new State("downloading rcml", new DownloadingRcml(source), null);
        initializingConfMediaGroup = new State("initializing call media group", new InitializingConferenceMediaGroup(source),
                null);
        ready = new State("ready", new Ready(source), null);
        notFound = new State("notFound", new NotFound(source), null);

        caching = new State("caching", new Caching(source), null);
        checkingCache = new State("checkingCache", new CheckCache(source), null);
        playing = new State("playing", new Playing(source), null);
        synthesizing = new State("synthesizing", new Synthesizing(source), null);
        redirecting = new State("redirecting", new Redirecting(source), null);

        finished = new State("finished", new Finished(source), null);

        // Initialize the transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, acquiringSynthesizerInfo));
        transitions.add(new Transition(uninitialized, finished));

        transitions.add(new Transition(acquiringSynthesizerInfo, finished));
        transitions.add(new Transition(acquiringSynthesizerInfo, downloadingRcml));

        transitions.add(new Transition(acquiringConfMediaGroup, initializingConfMediaGroup));
        transitions.add(new Transition(acquiringConfMediaGroup, finished));

        transitions.add(new Transition(initializingConfMediaGroup, downloadingRcml));
        transitions.add(new Transition(initializingConfMediaGroup, checkingCache));
        transitions.add(new Transition(initializingConfMediaGroup, caching));
        transitions.add(new Transition(initializingConfMediaGroup, synthesizing));
        transitions.add(new Transition(initializingConfMediaGroup, redirecting));
        transitions.add(new Transition(initializingConfMediaGroup, finished));
        transitions.add(new Transition(initializingConfMediaGroup, ready));
        transitions.add(new Transition(downloadingRcml, ready));
        transitions.add(new Transition(downloadingRcml, notFound));
        transitions.add(new Transition(downloadingRcml, finished));
        transitions.add(new Transition(downloadingRcml, acquiringConfMediaGroup));

        transitions.add(new Transition(ready, checkingCache));
        transitions.add(new Transition(ready, caching));
        transitions.add(new Transition(ready, synthesizing));
        transitions.add(new Transition(ready, redirecting));
        transitions.add(new Transition(ready, finished));

        transitions.add(new Transition(caching, playing));
        transitions.add(new Transition(caching, caching));
        transitions.add(new Transition(caching, redirecting));
        transitions.add(new Transition(caching, synthesizing));

        transitions.add(new Transition(caching, finished));
        transitions.add(new Transition(checkingCache, synthesizing));
        transitions.add(new Transition(checkingCache, playing));
        transitions.add(new Transition(checkingCache, checkingCache));
        transitions.add(new Transition(playing, ready));
        transitions.add(new Transition(playing, finished));

        transitions.add(new Transition(synthesizing, checkingCache));
        transitions.add(new Transition(synthesizing, caching));
        transitions.add(new Transition(synthesizing, redirecting));

        transitions.add(new Transition(synthesizing, synthesizing));
        transitions.add(new Transition(synthesizing, finished));

        transitions.add(new Transition(redirecting, ready));
        transitions.add(new Transition(redirecting, checkingCache));
        transitions.add(new Transition(redirecting, caching));
        transitions.add(new Transition(redirecting, synthesizing));
        transitions.add(new Transition(redirecting, redirecting));

        transitions.add(new Transition(redirecting, finished));

        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        // Initialize the runtime stuff.
        this.accountId = account;
        this.version = version;
        this.url = url;
        this.method = method;
        this.emailAddress = emailAddress;
        this.configuration = configuration;

        this.storage = storage;
        this.synthesizer = tts(configuration.subset("speech-synthesizer"));
        this.mailerNotify = mailer(configuration.subset("smtp-notify"));
        final Configuration runtime = configuration.subset("runtime-settings");
        String path = runtime.getString("cache-path");
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        path = path + accountId.toString();
        cachePath = path;
        String uri = runtime.getString("cache-uri");
        if (!uri.endsWith("/")) {
            uri = uri + "/";
        }
        uri = uri + accountId.toString();
        this.cache = cache(path, uri);
        this.downloader = downloader();

        this.callInfo = callInfo;
        this.conference = conference;
    }

    private ActorRef cache(final String path, final String uri) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new DiskCache(path, uri, true);
            }
        }));
    }

    private ActorRef downloader() {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Downloader();
            }
        }));
    }

    private String e164(final String number) {
        final PhoneNumberUtil numbersUtil = PhoneNumberUtil.getInstance();
        try {
            final PhoneNumber result = numbersUtil.parse(number, "US");
            return numbersUtil.format(result, PhoneNumberFormat.E164);
        } catch (final NumberParseException ignored) {
            return number;
        }
    }

    private void invalidVerb(final Tag verb) {
        final ActorRef self = self();
        // Get the next verb.
        final GetNextVerb next = GetNextVerb.instance();
        parser.tell(next, self);
    }

    ActorRef mailer(final Configuration configuration) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                final CreateEmailService builder = new EmailService();
                builder.CreateEmailSession(configuration);
                EMAIL_SENDER=builder.getUser();
                return builder.build();
            }
        }));
    }

    private Notification notification(final int log, final int error, final String message) {
        final Notification.Builder builder = Notification.builder();
        final Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        builder.setSid(sid);
        builder.setAccountSid(accountId);
        builder.setCallSid(callInfo.sid());
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
        if (request != null) {
            builder.setRequestUrl(request.getUri());
            builder.setRequestMethod(request.getMethod());
            builder.setRequestVariables(request.getParametersAsString());
        }
        if (response != null) {
            builder.setResponseHeaders(response.getHeadersAsString());
            final String type = response.getContentType();
            if (type.contains("text/xml") || type.contains("application/xml") || type.contains("text/html")) {
                try {
                    builder.setResponseBody(response.getContentAsString());
                } catch (final IOException exception) {
                    logger.error(
                            "There was an error while reading the contents of the resource " + "located @ " + url.toString(),
                            exception);
                }
            }
        }
        buffer = new StringBuilder();
        buffer.append("/").append(version).append("/Accounts/");
        buffer.append(accountId.toString()).append("/Notifications/");
        buffer.append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        builder.setUri(uri);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final State state = fsm.state();
        final ActorRef sender = sender();

        if (logger.isInfoEnabled()) {
            logger.info(" ********** ConfVoiceInterpreter's Current State: " + state.toString());
            logger.info(" ********** ConfVoiceInterpreter's Processing Message: " + klass.getName());
        }

        if (StartInterpreter.class.equals(klass)) {
            originalInterpreter = sender;
            fsm.transition(message, acquiringSynthesizerInfo);
        } else if (SpeechSynthesizerResponse.class.equals(klass)) {
            if (acquiringSynthesizerInfo.equals(state)) {
                fsm.transition(message, downloadingRcml);
            } else if (synthesizing.equals(state)) {
                final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>) message;
                if (response.succeeded()) {
                    fsm.transition(message, caching);
                } else {
                    fsm.transition(message, finished);
                }
            }
        } else if (MediaServerControllerResponse.class.equals(klass)) {
            if (acquiringConfMediaGroup.equals(state)) {
                fsm.transition(message, initializingConfMediaGroup);
            }
        } else if (DownloaderResponse.class.equals(klass)) {
            downloaderResponse = (DownloaderResponse) message;
            if (logger.isDebugEnabled()) {
                logger.debug("response succeeded " + downloaderResponse.succeeded() + ", statusCode "
                        + downloaderResponse.get().getStatusCode());
            }
            if (downloaderResponse.succeeded() && HttpStatus.SC_OK == downloaderResponse.get().getStatusCode()) {
                fsm.transition(message, acquiringConfMediaGroup);
            } else if (downloaderResponse.succeeded() && HttpStatus.SC_NOT_FOUND == downloaderResponse.get().getStatusCode()) {
                fsm.transition(message, notFound);
            }
        } else if (MediaGroupStateChanged.class.equals(klass)) {
            final MediaGroupStateChanged event = (MediaGroupStateChanged) message;
            if (MediaGroupStateChanged.State.ACTIVE == event.state()) {
                if (initializingConfMediaGroup.equals(state)) {
                    fsm.transition(message, ready);
                } else if (ready.equals(state)) {
                    if (play.equals(verb.name())) {
                        fsm.transition(message, caching);
                    } else if (say.equals(verb.name())) {
                        fsm.transition(message, checkingCache);
                    } else {
                        invalidVerb(verb);
                    }
                }
            } else if (MediaGroupStateChanged.State.INACTIVE == event.state()) {
                if (acquiringConfMediaGroup.equals(state)) {
                    fsm.transition(message, initializingConfMediaGroup);
                } else if (!finished.equals(state)) {
                    fsm.transition(message, finished);
                }
            }
        } else if (DiskCacheResponse.class.equals(klass)) {
            final DiskCacheResponse response = (DiskCacheResponse) message;
            logger.info("DiskCacheResponse " + response.succeeded() + " error=" + response.error());
            if (response.succeeded()) {
                if (caching.equals(state) || checkingCache.equals(state)) {
                    if (play.equals(verb.name()) || say.equals(verb.name())) {
                        fsm.transition(message, playing);
                    }
                }
            } else {

                if (checkingCache.equals(state)) {
                    fsm.transition(message, synthesizing);
                } else {
                    fsm.transition(message, finished);
                }
            }
        } else if (Tag.class.equals(klass)) {
            verb = (Tag) message;

            logger.info("ConfVoiceInterpreter verb = " + verb.name());

            if (Verbs.dial.equals(verb.name()))
                originalInterpreter.tell(new Exception("Dial verb not supported"), source);

            if (play.equals(verb.name())) {
                fsm.transition(message, caching);
            } else if (say.equals(verb.name())) {
                fsm.transition(message, checkingCache);
            } else {
                invalidVerb(verb);
            }
        } else if (End.class.equals(klass)) {
            // TODO kill this interpreter and also the MediaGroup
            fsm.transition(message, finished);
            // originalInterpreter.tell(message, source);
        } else if (MediaGroupResponse.class.equals(klass)) {
            final MediaGroupResponse<String> response = (MediaGroupResponse<String>) message;
            if (response.succeeded()) {
                if (playing.equals(state)) {
                    fsm.transition(message, ready);
                }
            } else {
                originalInterpreter.tell(message, source);
            }
        } else if (StopInterpreter.class.equals(klass)) {
            fsm.transition(message, finished);
        } else if (message instanceof ReceiveTimeout) {
            // TODO?
        }
    }

    private List<NameValuePair> parameters() {
        final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        final String callSid = callInfo.sid().toString();
        parameters.add(new BasicNameValuePair("CallSid", callSid));
        final String accountSid = accountId.toString();
        parameters.add(new BasicNameValuePair("AccountSid", accountSid));
        final String from = e164(callInfo.from());
        parameters.add(new BasicNameValuePair("From", from));
        final String to = e164(callInfo.to());
        parameters.add(new BasicNameValuePair("To", to));
        // final String state = callState.toString();
        // parameters.add(new BasicNameValuePair("CallStatus", state));
        parameters.add(new BasicNameValuePair("ApiVersion", version));
        final String direction = callInfo.direction();
        parameters.add(new BasicNameValuePair("Direction", direction));
        final String callerName = callInfo.fromName();
        parameters.add(new BasicNameValuePair("CallerName", callerName));
        final String forwardedFrom = callInfo.forwardedFrom();
        parameters.add(new BasicNameValuePair("ForwardedFrom", forwardedFrom));
        return parameters;
    }

    private ActorRef parser(final String xml) {
        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new Parser(xml);
            }
        }));
    }

    private void postCleanup() {
        final ActorRef self = self();
        final UntypedActorContext context = getContext();
        context.stop(self);
    }

    private URI resolve(final URI base, final URI uri) {
        if (base.equals(uri)) {
            return uri;
        } else {
            if (!uri.isAbsolute()) {
                return base.resolve(uri);
            } else {
                return uri;
            }
        }
    }

    private void sendMail(final Notification notification) {
        if (emailAddress == null || emailAddress.isEmpty()) {
            return;
        }

        final String EMAIL_SUBJECT = "RestComm Error Notification - Attention Required";
        final StringBuilder buffer = new StringBuilder();
        buffer.append("<strong>").append("Sid: ").append("</strong></br>");
        buffer.append(notification.getSid().toString()).append("</br>");
        buffer.append("<strong>").append("Account Sid: ").append("</strong></br>");
        buffer.append(notification.getAccountSid().toString()).append("</br>");
        buffer.append("<strong>").append("Call Sid: ").append("</strong></br>");
        buffer.append(notification.getCallSid().toString()).append("</br>");
        buffer.append("<strong>").append("API Version: ").append("</strong></br>");
        buffer.append(notification.getApiVersion()).append("</br>");
        buffer.append("<strong>").append("Log: ").append("</strong></br>");
        buffer.append(notification.getLog() == ERROR_NOTIFICATION ? "ERROR" : "WARNING").append("</br>");
        buffer.append("<strong>").append("Error Code: ").append("</strong></br>");
        buffer.append(notification.getErrorCode()).append("</br>");
        buffer.append("<strong>").append("More Information: ").append("</strong></br>");
        buffer.append(notification.getMoreInfo().toString()).append("</br>");
        buffer.append("<strong>").append("Message Text: ").append("</strong></br>");
        buffer.append(notification.getMessageText()).append("</br>");
        buffer.append("<strong>").append("Message Date: ").append("</strong></br>");
        buffer.append(notification.getMessageDate().toString()).append("</br>");
        buffer.append("<strong>").append("Request URL: ").append("</strong></br>");
        buffer.append(notification.getRequestUrl().toString()).append("</br>");
        buffer.append("<strong>").append("Request Method: ").append("</strong></br>");
        buffer.append(notification.getRequestMethod()).append("</br>");
        buffer.append("<strong>").append("Request Variables: ").append("</strong></br>");
        buffer.append(notification.getRequestVariables()).append("</br>");
        buffer.append("<strong>").append("Response Headers: ").append("</strong></br>");
        buffer.append(notification.getResponseHeaders()).append("</br>");
        buffer.append("<strong>").append("Response Body: ").append("</strong></br>");
        buffer.append(notification.getResponseBody()).append("</br>");
        final Mail emailMsg = new Mail(EMAIL_SENDER,emailAddress,EMAIL_SUBJECT, buffer.toString());
        mailerNotify.tell(new EmailRequest(emailMsg), self());
    }

    private ActorRef tts(final Configuration configuration) {
        final String classpath = configuration.getString("[@class]");

        final UntypedActorContext context = getContext();
        return context.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return (UntypedActor) Class.forName(classpath).getConstructor(Configuration.class).newInstance(configuration);
            }
        }));
    }

    private abstract class AbstractAction implements Action {
        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class AcquiringSpeechSynthesizerInfo extends AbstractAction {
        public AcquiringSpeechSynthesizerInfo(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final StartInterpreter request = (StartInterpreter) message;
            conference = request.resource();
            synthesizer.tell(new GetSpeechSynthesizerInfo(), source);
        }
    }

    private final class AcquiringConferenceMediaGroup extends AbstractAction {
        public AcquiringConferenceMediaGroup(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            conference.tell(new CreateMediaGroup(), source);
        }
    }

    private final class InitializingConferenceMediaGroup extends AbstractAction {
        public InitializingConferenceMediaGroup(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaServerControllerResponse<ActorRef> response = (MediaServerControllerResponse<ActorRef>) message;
            conferenceMediaGroup = response.get();
            conferenceMediaGroup.tell(new Observe(source), source);
            final StartMediaGroup request = new StartMediaGroup();
            conferenceMediaGroup.tell(request, source);
        }
    }

    private final class DownloadingRcml extends AbstractAction {
        public DownloadingRcml(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {

            final Class<?> klass = message.getClass();
            if (SpeechSynthesizerResponse.class.equals(klass)) {

                final SpeechSynthesizerResponse<SpeechSynthesizerInfo> response = (SpeechSynthesizerResponse<SpeechSynthesizerInfo>) message;
                synthesizerInfo = response.get();

                // Ask the downloader to get us the application that will be
                // executed.
                final List<NameValuePair> parameters = parameters();
                request = new HttpRequestDescriptor(url, method, parameters);
                downloader.tell(request, source);
            }
        }
    }

    private final class Ready extends AbstractAction {
        public Ready(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (parser == null) {
                response = downloaderResponse.get();

                final String type = response.getContentType();
                if (type.contains("text/xml") || type.contains("application/xml") || type.contains("text/html")) {
                    parser = parser(response.getContentAsString());
                } else if (type.contains("audio/wav") || type.contains("audio/wave") || type.contains("audio/x-wav")) {
                    parser = parser("<Play>" + request.getUri() + "</Play>");
                } else if (type.contains("text/plain")) {
                    parser = parser("<Say>" + response.getContentAsString() + "</Say>");
                } else {
                    final StopInterpreter stop = new StopInterpreter();
                    source.tell(stop, source);
                    return;
                }
            }
            // Ask the parser for the next action to take.
            final GetNextVerb next = GetNextVerb.instance();
            parser.tell(next, source);
        }
    }

    private final class NotFound extends AbstractAction {
        public NotFound(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final DownloaderResponse response = (DownloaderResponse) message;
            if (logger.isDebugEnabled()) {
                logger.debug("response succeeded " + response.succeeded() + ", statusCode " + response.get().getStatusCode());
            }
            final Notification notification = notification(WARNING_NOTIFICATION, 21402, "URL Not Found : "
                    + response.get().getURI());
            final NotificationsDao notifications = storage.getNotificationsDao();
            notifications.addNotification(notification);
            // Hang up the call.
            conference.tell(new org.mobicents.servlet.restcomm.telephony.NotFound(), source);
        }
    }

    private final class CheckCache extends AbstractAction {
        public CheckCache(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }

            String hash = hash(verb);
            DiskCacheRequest request = new DiskCacheRequest(hash);
            if (logger.isErrorEnabled()) {
                logger.info("Checking cache for hash: " + hash);
            }
            cache.tell(request, source);
        }
    }

    private final class Caching extends AbstractAction {
        public Caching(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (SpeechSynthesizerResponse.class.equals(klass)) {
                final SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>) message;
                final DiskCacheRequest request = new DiskCacheRequest(response.get());
                cache.tell(request, source);
            } else if (Tag.class.equals(klass) || MediaGroupStateChanged.class.equals(klass)) {
                if (Tag.class.equals(klass)) {
                    verb = (Tag) message;
                }
                // Parse the URL.
                final String text = verb.text();
                if (text != null && !text.isEmpty()) {
                    // Try to cache the media.
                    URI target = null;
                    try {
                        target = URI.create(text);
                    } catch (final Exception exception) {
                        final Notification notification = notification(ERROR_NOTIFICATION, 11100, text + " is an invalid URI.");
                        final NotificationsDao notifications = storage.getNotificationsDao();
                        notifications.addNotification(notification);
                        sendMail(notification);
                        final StopInterpreter stop = new StopInterpreter();
                        source.tell(stop, source);
                        return;
                    }
                    final URI base = request.getUri();
                    final URI uri = resolve(base, target);
                    final DiskCacheRequest request = new DiskCacheRequest(uri);
                    cache.tell(request, source);
                } else {
                    // Ask the parser for the next action to take.
                    final GetNextVerb next = GetNextVerb.instance();
                    parser.tell(next, source);
                }
            }
        }
    }

    private final class Playing extends AbstractAction {
        public Playing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (DiskCacheResponse.class.equals(klass)) {
                // Issue 202: https://bitbucket.org/telestax/telscale-restcomm/issue/202
                // Parse the loop attribute.
                int loop = Integer.MAX_VALUE;
                final Attribute attribute = verb.attribute("loop");
                if (attribute != null) {
                    final String number = attribute.value();
                    if (number != null && !number.isEmpty()) {
                        try {
                            loop = Integer.parseInt(number);
                        } catch (final NumberFormatException ignored) {
                            final NotificationsDao notifications = storage.getNotificationsDao();
                            Notification notification = null;
                            if (say.equals(verb.name())) {
                                notification = notification(WARNING_NOTIFICATION, 13510, loop + " is an invalid loop value.");
                                notifications.addNotification(notification);
                            } else if (play.equals(verb.name())) {
                                notification = notification(WARNING_NOTIFICATION, 13410, loop + " is an invalid loop value.");
                                notifications.addNotification(notification);
                            }
                        }
                    }
                }
                final DiskCacheResponse response = (DiskCacheResponse) message;
                final Play play = new Play(response.get(), loop);
                conferenceMediaGroup.tell(play, source);
            }
        }
    }

    private String hash(Object message) {
        Map<String, String> details = getSynthesizeDetails(message);
        if (details == null) {
            if (logger.isInfoEnabled()) {
                logger.info("Cannot generate hash, details are null");
            }
            return null;
        }
        String voice = details.get("voice");
        String language = details.get("language");
        String text = details.get("text");
        return HashGenerator.hashMessage(voice, language, text);
    }

    private Map<String, String> getSynthesizeDetails(final Object message) {
        final Class<?> klass = message.getClass();

        Map<String, String> details = new HashMap<String, String>();

        if (Tag.class.equals(klass)) {
            verb = (Tag) message;
        } else {
            return null;
        }
        if (!say.equals(verb.name()))
            return null;

        // Parse the voice attribute.
        String voice = "man";
        Attribute attribute = verb.attribute("voice");
        if (attribute != null) {
            voice = attribute.value();
            if (voice != null && !voice.isEmpty()) {
                if (!"man".equals(voice) && !"woman".equals(voice)) {
                    final Notification notification = notification(WARNING_NOTIFICATION, 13511, voice
                            + " is an invalid voice value.");
                    final NotificationsDao notifications = storage.getNotificationsDao();
                    notifications.addNotification(notification);
                    voice = "man";
                }
            } else {
                voice = "man";
            }
        }
        // Parse the language attribute.
        String language = "en";
        attribute = verb.attribute("language");
        if (attribute != null) {
            language = attribute.value();
            if (language != null && !language.isEmpty()) {
                if (!synthesizerInfo.languages().contains(language)) {
                    language = "en";
                }
            } else {
                language = "en";
            }
        }
        // Synthesize.
        String text = verb.text();

        details.put("voice", voice);
        details.put("language", language);
        details.put("text", text);

        return details;

    }

    private final class Synthesizing extends AbstractAction {
        public Synthesizing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {

            final Class<?> klass = message.getClass();

            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }

            Map<String, String> details = getSynthesizeDetails(verb);
            if (details != null && !details.isEmpty()) {
                String voice = details.get("voice");
                String language = details.get("language");
                String text = details.get("text");
                final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest(voice, language, text);
                synthesizer.tell(synthesize, source);
            } else {
                // Ask the parser for the next action to take.
                final GetNextVerb next = GetNextVerb.instance();
                parser.tell(next, source);
            }
        }
    }

    private final class Redirecting extends AbstractAction {
        public Redirecting(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (Tag.class.equals(klass)) {
                verb = (Tag) message;
            }
            final NotificationsDao notifications = storage.getNotificationsDao();
            String method = "POST";
            Attribute attribute = verb.attribute("method");
            if (attribute != null) {
                method = attribute.value();
                if (method != null && !method.isEmpty()) {
                    if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                        final Notification notification = notification(WARNING_NOTIFICATION, 13710, method
                                + " is not a valid HTTP method for <Redirect>");
                        notifications.addNotification(notification);
                        method = "POST";
                    }
                } else {
                    method = "POST";
                }
            }
            final String text = verb.text();
            if (text != null && !text.isEmpty()) {
                // Try to redirect.
                URI target = null;
                try {
                    target = URI.create(text);
                } catch (final Exception exception) {
                    final Notification notification = notification(ERROR_NOTIFICATION, 11100, text + " is an invalid URI.");
                    notifications.addNotification(notification);
                    sendMail(notification);
                    final StopInterpreter stop = new StopInterpreter();
                    source.tell(stop, source);
                    return;
                }
                final URI base = request.getUri();
                final URI uri = resolve(base, target);
                final List<NameValuePair> parameters = parameters();
                request = new HttpRequestDescriptor(uri, method, parameters);
                downloader.tell(request, source);
            } else {
                // Ask the parser for the next action to take.
                final GetNextVerb next = GetNextVerb.instance();
                parser.tell(next, source);
            }
        }
    }

    private final class Finished extends AbstractAction {
        public Finished(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            logger.info("Finished called for ConfVoiceInterpreter");

            final StopMediaGroup stop = new StopMediaGroup();
            // Destroy the media group(s).
            if (conferenceMediaGroup != null) {
                conferenceMediaGroup.tell(stop, source);
                final DestroyWaitUrlConfMediaGroup destroy = new DestroyWaitUrlConfMediaGroup(conferenceMediaGroup);
                conference.tell(destroy, source);
//                conferenceMediaGroup = null;
            }

            // TODO should the dependencies be stopped here?

            // Stop the dependencies.
            final UntypedActorContext context = getContext();
            context.stop(mailerNotify);
            context.stop(downloader);
            context.stop(cache);
            context.stop(synthesizer);
            // Stop the interpreter.
            postCleanup();
        }
    }

    @Override
    public void postStop() {
        // final StopMediaGroup stop = new StopMediaGroup();
        // Destroy the media group(s).
        // if (conferenceMediaGroup != null) {
        // conferenceMediaGroup.tell(stop, source);
        // }
        // if (conference != null && !conference.isTerminated()) {
        // final DestroyWaitUrlConfMediaGroup destroy = new DestroyWaitUrlConfMediaGroup(conferenceMediaGroup);
        // conference.tell(destroy, source);
        // }
        //
        // if (conferenceMediaGroup != null && !conferenceMediaGroup.isTerminated())
        // getContext().stop(conferenceMediaGroup);
        //
        // conferenceMediaGroup = null;
        super.postStop();
    }
}
