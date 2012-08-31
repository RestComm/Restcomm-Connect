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

import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readDateTime;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readInteger;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readSid;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readString;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readUri;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.writeDateTime;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.writeSid;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.writeUri;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ShortCodesDao;
import org.mobicents.servlet.sip.restcomm.entities.ShortCode;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoShortCodesDao implements ShortCodesDao {
  private static final Logger logger = Logger.getLogger(MongoShortCodesDao.class);
  private final DBCollection collection;
  
  public MongoShortCodesDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_short_codes");
  }
  
  @Override public void addShortCode(final ShortCode shortCode) {
    final WriteResult result = collection.insert(toDbObject(shortCode));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public ShortCode getShortCode(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    final DBObject result = collection.findOne(query);
    if(result != null) {
      return toShortCode(result);
    } else {
      return null;
    }
  }

  @Override public List<ShortCode> getShortCodes(final Sid accountSid) {
    final List<ShortCode> shortCodes = new ArrayList<ShortCode>();
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      shortCodes.add(toShortCode(results.next()));
    }
    return shortCodes;
  }

  @Override public void removeShortCode(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    removeShortCodes(query);
  }

  @Override public void removeShortCodes(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    removeShortCodes(query);
  }
  
  private void removeShortCodes(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public void updateShortCode(final ShortCode shortCode) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", shortCode.getSid().toString());
    final WriteResult result = collection.update(query, toDbObject(shortCode));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private DBObject toDbObject(final ShortCode shortCode) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", writeSid(shortCode.getSid()));
    object.put("date_created", writeDateTime(shortCode.getDateCreated()));
    object.put("date_updated", writeDateTime(shortCode.getDateUpdated()));
    object.put("friendly_name", shortCode.getFriendlyName());
    object.put("account_sid", writeSid(shortCode.getAccountSid()));
    object.put("short_code", shortCode.getShortCode());
    object.put("api_version", shortCode.getApiVersion());
    object.put("sms_url", writeUri(shortCode.getSmsUrl()));
    object.put("sms_method", shortCode.getSmsMethod());
    object.put("sms_fallback_url", writeUri(shortCode.getSmsFallbackUrl()));
    object.put("sms_fallback_method", shortCode.getSmsFallbackMethod());
    object.put("uri", writeUri(shortCode.getUri()));
    return object;
  }
  
  private ShortCode toShortCode(final DBObject object) {
    final Sid sid = readSid(object.get("sid"));
    final DateTime dateCreated = readDateTime(object.get("date_created"));
    final DateTime dateUpdated = readDateTime(object.get("date_updated"));
    final String friendlyName = readString(object.get("friendly_name"));
    final Sid accountSid = readSid(object.get("account_sid"));
    final Integer shortCode = readInteger(object.get("short_code"));
    final String apiVersion = readString(object.get("api_version"));
    final URI smsUrl = readUri(object.get("sms_url"));
    final String smsMethod = readString(object.get("sms_method"));
    final URI smsFallbackUrl = readUri(object.get("sms_fallback_url"));
    final String smsFallbackMethod = readString(object.get("sms_fallback_method"));
    final URI uri = readUri(object.get("uri"));
    return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode, apiVersion,
        smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
}
