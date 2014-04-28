package org.mobicents.servlet.restcomm.http;

import static akka.pattern.Patterns.ask;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.cache.DiskCache;
import org.mobicents.servlet.restcomm.cache.DiskCacheRequest;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Announcement;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.AnnouncementConverter;
import org.mobicents.servlet.restcomm.http.converter.AnnouncementListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerRequest;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerResponse;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.util.Timeout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */
@NotThreadSafe
public abstract class AnnouncementsEndpoint extends AbstractEndpoint {
    private static Logger logger = Logger.getLogger(AnnouncementsEndpoint.class);

    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected Configuration runtime;
    protected ActorSystem system;
    protected ActorRef synthesizer;
    protected ActorRef cache;
    protected Gson gson;
    protected XStream xstream;
    protected AccountsDao dao;
    private URI uri;

    public AnnouncementsEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        dao = storage.getAccountsDao();
        system = (ActorSystem) context.getAttribute(ActorSystem.class.getName());
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        Configuration ttsConfiguration = configuration.subset("speech-synthesizer");
        runtime = configuration.subset("runtime-settings");
        synthesizer = tts(ttsConfiguration);
        super.init(runtime);
        final AnnouncementConverter converter = new AnnouncementConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Announcement.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new AnnouncementListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    public Response putAnnouncement(final String accountSid, final MultivaluedMap<String, String> data,
            final MediaType responseType) throws Exception {
        try {
            secure(dao.getAccount(accountSid), "RestComm:Create:Announcements");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        if(cache == null)
            createCacheActor(accountSid);

        Announcement announcement = createFrom(accountSid, data);
        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(announcement), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(announcement);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    private void createCacheActor(final String accountId) {
        String path = runtime.getString("cache-path");
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        path = path + accountId.toString();
        String uri = runtime.getString("cache-uri");
        if (!uri.endsWith("/")) {
            uri = uri + "/";
        }
        uri = uri + accountId.toString();
        this.cache = cache(path, uri);
    }

    private void precache(final String text, final String gender, final String language) throws Exception {
        logger.info("Synthesizing announcement");
        final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest(gender, language, text);
        Timeout expires = new Timeout(Duration.create(6000, TimeUnit.SECONDS));
        Future<Object> future = (Future<Object>) ask(synthesizer, synthesize, expires);
        Object object = Await.result(future, Duration.create(6000, TimeUnit.SECONDS));
        if(object != null) {
            SpeechSynthesizerResponse<URI> response = (SpeechSynthesizerResponse<URI>)object;
            uri = response.get();
        }
        final DiskCacheRequest request = new DiskCacheRequest(uri);
        logger.info("Caching announcement");
        cache.tell(request, null);
    }

    private Announcement createFrom(String accountSid, MultivaluedMap<String, String> data) throws Exception {
        Sid sid = Sid.generate(Sid.Type.ANNOUNCEMENT);
        String gender = data.getFirst("Gender");
        if (gender == null) {
            gender = "man";
        }
        String language = data.getFirst("Language");
        if (language == null) {
            language = "en";
        }
        String text = data.getFirst("Text");
        if (text != null) {
            precache(text, gender, language);
        }
        logger.info("Creating annnouncement");
        Announcement announcement = new Announcement(sid, new Sid(accountSid), gender, language, text, uri);
        return announcement;
    }

    private ActorRef tts(final Configuration configuration) {
        final String classpath = configuration.getString("[@class]");

        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return (UntypedActor) Class.forName(classpath).getConstructor(Configuration.class).newInstance(configuration);
            }
        }));
    }

    private ActorRef cache(final String path, final String uri) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return new DiskCache(path, uri, true);
            }
        }));
    }

    @PreDestroy
    private void cleanup() {
        logger.info("Stopping actors before endpoint destroy");
        system.stop(cache);
        system.stop(synthesizer);
    }
}
