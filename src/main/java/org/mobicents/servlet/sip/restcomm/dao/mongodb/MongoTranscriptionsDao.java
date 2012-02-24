/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.Transcription;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.TranscriptionsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoTranscriptionsDao implements TranscriptionsDao {
  private static final Logger logger = Logger.getLogger(MongoTranscriptionsDao.class);
  private final DBCollection collection;
  
  public MongoTranscriptionsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_transcriptions");
  }
  
  @Override public void addTranscription(final Transcription transcription) {
    final WriteResult result = collection.insert(toDbObject(transcription));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public Transcription getTranscription(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    return getTranscription(query);
  }

  @Override public Transcription getTranscriptionByRecording(final Sid recordingSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("recording_sid", recordingSid.toString());
    return getTranscription(query);
  }
  
  private Transcription getTranscription(final DBObject query) {
    final DBObject result = collection.findOne(query);
    if(result != null) {
      return toTranscription(result);
    } else {
      return null;
    }
  }

  @Override public List<Transcription> getTranscriptions(final Sid accountSid) {
    final List<Transcription> transcriptions = new ArrayList<Transcription>();
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      transcriptions.add(toTranscription(results.next()));
    }
    return transcriptions;
  }

  @Override public void removeTranscription(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    removeTranscriptions(query);
  }

  @Override	public void removeTranscriptions(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    removeTranscriptions(query);
  }
  
  private void removeTranscriptions(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private DBObject toDbObject(final Transcription transcription) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", transcription.getSid().toString());
    object.put("date_created", transcription.getDateCreated().toDate());
    object.put("date_updated", transcription.getDateUpdated().toDate());
    object.put("account_sid", transcription.getAccountSid().toString());
    object.put("status", transcription.getStatus());
    object.put("recording_sid", transcription.getRecordingSid().toString());
    object.put("duration", transcription.getDuration());
    object.put("transcription_text", transcription.getTranscriptionText());
    object.put("price", transcription.getPrice().toString());
    object.put("uri", transcription.getUri().toString());
    return object;
  }
  
  private Transcription toTranscription(final DBObject object) {
    final Sid sid = new Sid((String)object.get("sid"));
    final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
    final Sid accountSid = new Sid((String)object.get("account_sid"));
    final String status = (String)object.get("status");
    final Sid recordingSid = new Sid((String)object.get("recording_sid"));
    final Integer duration = (Integer)object.get("duration");
    final String transcriptionText = (String)object.get("transcription_text");
    final BigDecimal price = new BigDecimal((String)object.get("price"));
    final URI uri = URI.create((String)object.get("uri"));
    return new Transcription(sid, dateCreated, dateUpdated, accountSid, status, recordingSid, duration,
        transcriptionText, price, uri);
  }
}
