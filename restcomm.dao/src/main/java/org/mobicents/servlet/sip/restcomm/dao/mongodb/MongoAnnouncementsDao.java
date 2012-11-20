package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AnnouncementsDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;
import org.mobicents.servlet.sip.restcomm.entities.Announcement;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */
@ThreadSafe
public final class MongoAnnouncementsDao implements AnnouncementsDao {

	  private static final Logger logger = Logger.getLogger(MongoAnnouncementsDao.class);
	  private final DBCollection collection;

	  public MongoAnnouncementsDao(final DB database) {
	    super();
	    collection = database.getCollection("restcomm_announcements");
	  }
	
	@Override
	public void addAnnouncement(Announcement announcement) {
	    final WriteResult result = collection.insert(toDbObject(announcement));
	    if(!result.getLastError().ok()) {
	      logger.error(result.getLastError().getErrorMessage());
	    }
	}

	@Override
	public Announcement getAnnouncement(Sid sid) {
	    final BasicDBObject query = new BasicDBObject();
	    query.put("sid", sid.toString());
	    final DBObject result = collection.findOne(query);
	    if(result != null) {
	      return toAnnouncement(result);
	    } else {
	      return null;
	    }
	}

	@Override
	public List<Announcement> getAnnouncements(Sid accountSid) {
	    final List<Announcement> announcements = new ArrayList<Announcement>();
	    final BasicDBObject query = new BasicDBObject();
	    query.put("account_sid", accountSid.toString());
	    final DBCursor results = collection.find(query);
	    while(results.hasNext()) {
	      announcements.add(toAnnouncement(results.next()));
	    }
	    return announcements;
	}

	@Override
	public void removeAnnouncement(Sid sid) {
	    final BasicDBObject query = new BasicDBObject();
	    query.put("sid", sid.toString());
	    removeAnnouncements(query);
	}

	@Override
	public void removeAnnouncements(Sid accountSid) {
	    final BasicDBObject query = new BasicDBObject();
	    query.put("account_sid", accountSid.toString());
	    removeAnnouncements(query);
	}

	  private void removeAnnouncements(final DBObject query) {
		    final WriteResult result = collection.remove(query);
		    if(!result.getLastError().ok()) {
		      logger.error(result.getLastError().getErrorMessage());
		    }
		  }
	
	  private DBObject toDbObject(final Announcement announcement) {
		    final BasicDBObject object = new BasicDBObject();
		    object.put("sid", writeSid(announcement.getSid()));
		    object.put("date_created", writeDateTime(announcement.getDateCreated()));
		    object.put("account_sid", writeSid(announcement.getAccountSid()));
		    object.put("gender", announcement.getGender());
		    object.put("language", announcement.getLanguage());
		    object.put("text", announcement.getText());
		    object.put("uri", writeUri(announcement.getUri()));
		    return object;
		  }
	  
	  private Announcement toAnnouncement(final DBObject object) {
		    final Sid sid = readSid(object.get("sid"));
		    final DateTime dateCreated = readDateTime(object.get("date_created"));
		    final Sid accountSid = readSid(object.get("account_sid"));
		    final String gender = readString(object.get("gender"));
		    final String language = readString(object.get("language"));
		    final String text = readString(object.get("text"));
		    final URI uri = readUri(object.get("uri"));
		    return new Announcement(sid, dateCreated, accountSid, gender, language,
		        text, uri);
		  }
}
