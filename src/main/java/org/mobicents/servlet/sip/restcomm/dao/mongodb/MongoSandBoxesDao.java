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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import java.net.URI;

import org.apache.log4j.Logger;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.SandBox;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.SandBoxesDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoSandBoxesDao implements SandBoxesDao {
  private static final Logger logger = Logger.getLogger(MongoSandBoxesDao.class);
  private final DBCollection collection;

  public MongoSandBoxesDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_sand_boxes");
  }
  
  @Override public void addSandBox(final SandBox sandBox) {
    final WriteResult result = collection.insert(toDbObject(sandBox));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public SandBox getSandBox(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    final DBObject result = collection.findOne(query);
    if(result != null) {
      return toSandBox(result);
    } else {
      return null;
    }
  }

  @Override public void removeSandBox(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public void updateSandBox(final SandBox sandBox) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", sandBox.getAccountSid().toString());
    final WriteResult result = collection.update(query, toDbObject(sandBox));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private DBObject toDbObject(final SandBox sandBox) {
    final BasicDBObject object = new BasicDBObject();
    object.put("date_created", writeDateTime(sandBox.getDateCreated()));
    object.put("date_updated", writeDateTime(sandBox.getDateUpdated()));
    object.put("pin", sandBox.getPin());
    object.put("account_sid", writeSid(sandBox.getAccountSid()));
    object.put("phone_number", sandBox.getPhoneNumber());
    object.put("application_sid", writeSid(sandBox.getApplicationSid()));
    object.put("api_version", sandBox.getApiVersion());
    object.put("voice_url", writeUri(sandBox.getVoiceUrl()));
    object.put("voice_method", sandBox.getVoiceMethod());
    object.put("sms_url", writeUri(sandBox.getSmsUrl()));
    object.put("sms_method", sandBox.getSmsMethod());
    object.put("status_callback", writeUri(sandBox.getStatusCallback()));
    object.put("status_callback_method", sandBox.getStatusCallbackMethod());
    object.put("uri", writeUri(sandBox.getUri()));
    return object;
  }
  
  private SandBox toSandBox(final DBObject object) {
    final DateTime dateCreated = readDateTime(object.get("date_created"));
    final DateTime dateUpdated = readDateTime(object.get("date_updated"));
    final String pin = readString(object.get("pin"));
    final Sid accountSid = readSid(object.get("account_sid"));
    final String phoneNumber = readString(object.get("phone_number"));
    final Sid applicationSid = readSid(object.get("application_sid"));
    final String apiVersion = readString(object.get("api_version"));
    final URI voiceUrl = readUri(object.get("voice_url"));
    final String voiceMethod = readString(object.get("voice_method"));
    final URI smsUrl = readUri(object.get("sms_url"));
    final String smsMethod = readString(object.get("sms_method"));
    final URI statusCallback = readUri(object.get("status_callback"));
    final String statusCallbackMethod = readString(object.get("status_callback_method"));
    final URI uri = readUri(object.get("uri"));
    return new SandBox(dateCreated, dateUpdated, pin, accountSid, phoneNumber, applicationSid, apiVersion,
        voiceUrl, voiceMethod, smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
}
