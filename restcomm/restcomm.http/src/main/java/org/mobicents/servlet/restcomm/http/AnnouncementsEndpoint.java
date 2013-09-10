package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.entities.Announcement;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.AnnouncementConverter;
import org.mobicents.servlet.restcomm.http.converter.AnnouncementListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerRequest;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a> 
 */
@NotThreadSafe public abstract class AnnouncementsEndpoint extends AbstractEndpoint {
	@Context protected ServletContext context;
	protected Configuration configuration;
	protected ActorSystem system;
	protected ActorRef synthesizer;
	protected Gson gson;
	protected XStream xstream;

	public AnnouncementsEndpoint() {
		super();
	}

	@PostConstruct
	public void init() {
		system = (ActorSystem)context.getAttribute(ActorSystem.class.getName());
		configuration = (Configuration)context.getAttribute(Configuration.class.getName());
		configuration = configuration.subset("runtime-settings");
		synthesizer = tts(configuration);
		super.init(configuration);
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
			final MediaType responseType) {
		try { secure(new Sid(accountSid), "RestComm:Create:Announcements"); }
		catch(final AuthorizationException exception) { return status(UNAUTHORIZED).build(); }
		Announcement announcement = createFrom(accountSid, data);
		if(APPLICATION_JSON_TYPE == responseType) {
			return ok(gson.toJson(announcement), APPLICATION_JSON).build();
		} else if(APPLICATION_XML_TYPE == responseType) {
			final RestCommResponse response = new RestCommResponse(announcement);
			return ok(xstream.toXML(response), APPLICATION_XML).build();
		} else {
			return null;
		}
	}

	private void precache(final String text, final String gender, final String language) {
		final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest(gender, language, text);
		synthesizer.tell(synthesize, null);
	}

	private Announcement createFrom(String accountSid, MultivaluedMap<String, String> data){
		Sid sid = Sid.generate(Sid.Type.ANNOUNCEMENT);
		String gender = data.getFirst("Gender");
		if(gender == null) {
			gender = "man";
		}
		String language = data.getFirst("Language");
		if(language == null) {
			language = "en";
		}
		String text = data.getFirst("Text");
		if(text != null) {
			precache(text, gender, language);
		}
		Announcement announcement = new Announcement(sid, new Sid(accountSid), gender, language, text, null);
		return announcement;
	}

	private ActorRef tts(final Configuration configuration) {
		final String classpath = configuration.getString("[@class]");

		return system.actorOf(new Props(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;
			@Override public Actor create() throws Exception {			  
				return (UntypedActor)Class.forName(classpath).getConstructor(Configuration.class).newInstance(configuration);
			} 
		}));
	}
}
