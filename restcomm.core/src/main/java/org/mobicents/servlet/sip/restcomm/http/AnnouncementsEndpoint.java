package org.mobicents.servlet.sip.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AnnouncementsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.entities.Announcement;
import org.mobicents.servlet.sip.restcomm.entities.AnnouncementList;
import org.mobicents.servlet.sip.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.sip.restcomm.http.converter.AnnouncementConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.AnnouncementListConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizerException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a> 
 */
@NotThreadSafe
public abstract class AnnouncementsEndpoint extends AbstractEndpoint {

	protected final SpeechSynthesizer speechSynthesizer;
	protected final Gson gson;
	protected final XStream xstream;
	protected final AnnouncementsDao dao;

	public AnnouncementsEndpoint() {
		super();
		final ServiceLocator services = ServiceLocator.getInstance();
		dao = services.get(DaoManager.class).getAnnouncementsDao();
		speechSynthesizer = services.get(SpeechSynthesizer.class);
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

	public Response getAnnouncements(final String accountSid, final MediaType responseType){
		try { 
			secure(new Sid(accountSid), "RestComm:Read:Announcements"); 
		}catch(final AuthorizationException exception) {
			return status(UNAUTHORIZED).build(); 
		}
		final List<Announcement> announcements = dao.getAnnouncements(new Sid(accountSid));
		if(APPLICATION_JSON_TYPE == responseType) {
			return ok(gson.toJson(announcements), APPLICATION_JSON).build();
		} else if(APPLICATION_XML_TYPE == responseType) {
			final RestCommResponse response = new RestCommResponse(new AnnouncementList(announcements));
			return ok(xstream.toXML(response), APPLICATION_XML).build();
		} else {
			return null;
		}
	}

	public Response putAnnouncement(final String accountSid, final MultivaluedMap<String, String> data,
			final MediaType responseType) {
		try { 
			secure(new Sid(accountSid), "RestComm:Create:Announcements"); 
		} catch(final AuthorizationException exception) { 
			return status(UNAUTHORIZED).build(); 
		}

		Announcement announcement = createFrom(accountSid, data);
		dao.addAnnouncement(announcement);

		if(APPLICATION_JSON_TYPE == responseType) {
			return ok(gson.toJson(announcement), APPLICATION_JSON).build();
		} else if(APPLICATION_XML_TYPE == responseType) {
			final RestCommResponse response = new RestCommResponse(announcement);
			return ok(xstream.toXML(response), APPLICATION_XML).build();
		} else {
			return null;
		}
	}

	private URI precache(String text, String gender, String language) {
		return speechSynthesizer.synthesize(text, gender, language);
	}

	private Announcement createFrom(String accountSid, MultivaluedMap<String, String> data){
		Sid sid = Sid.generate(Sid.Type.ANNOUNCEMENT);
		String gender = data.getFirst("Gender");
		if (gender == null)
			gender = "man";
		String language = data.getFirst("Language");
		if (language == null)
			language = "en";
		String text = data.getFirst("Text");
		if (text == null)
			throw new SpeechSynthesizerException("Text cannot be null");
		URI uri = precache(text, gender, language);
		Announcement announcement = new Announcement(sid, new Sid(accountSid), gender, language, text, uri);
		return announcement;
	}
}
