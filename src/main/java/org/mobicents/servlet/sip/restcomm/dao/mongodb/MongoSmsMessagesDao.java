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
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.SmsMessage;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoSmsMessagesDao implements SmsMessagesDao {
  private static final Logger logger = Logger.getLogger(MongoSmsMessagesDao.class);
  private final DBCollection collection;
  
  public MongoSmsMessagesDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_sms_messages");
  }
  
  @Override public void addSmsMessage(final SmsMessage smsMessage) {
    final WriteResult result = collection.insert(toDbObject(smsMessage));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public SmsMessage getSmsMessage(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    final DBObject result = collection.findOne(query);
    if(result != null) {
      return toSmsMessage(result);
    } else {
      return null;
    }
  }

  @Override public List<SmsMessage> getSmsMessages(final Sid accountSid) {
    final List<SmsMessage> smsMessages = new ArrayList<SmsMessage>();
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      smsMessages.add(toSmsMessage(results.next()));
    }
    return smsMessages;
  }

  @Override public void removeSmsMessage(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    removeSmsMessages(query);
  }

  @Override	public void removeSmsMessages(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    removeSmsMessages(query);
  }
  
  private void removeSmsMessages(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private DBObject toDbObject(final SmsMessage smsMessage) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", writeSid(smsMessage.getSid()));
    object.put("date_created", writeDateTime(smsMessage.getDateCreated()));
    object.put("date_updated", writeDateTime(smsMessage.getDateUpdated()));
    object.put("date_sent", writeDateTime(smsMessage.getDateSent()));
    object.put("account_sid", writeSid(smsMessage.getAccountSid()));
    object.put("sender", smsMessage.getSender());
    object.put("recipient", smsMessage.getRecipient());
    object.put("body", smsMessage.getBody());
    object.put("status", smsMessage.getStatus());
    object.put("direction", smsMessage.getDirection());
    object.put("price", writeBigDecimal(smsMessage.getPrice()));
    object.put("api_version", smsMessage.getApiVersion());
    object.put("uri", writeUri(smsMessage.getUri()));
    return object;
  }
  
  private SmsMessage toSmsMessage(final DBObject object) {
    final Sid sid = readSid(object.get("sid"));
    final DateTime dateCreated = readDateTime(object.get("date_created"));
    final DateTime dateUpdated = readDateTime(object.get("date_updated"));
    final DateTime dateSent = readDateTime(object.get("date_sent"));
    final Sid accountSid = readSid(object.get("account_sid"));
    final String sender = readString(object.get("sender"));
    final String recipient = readString(object.get("recipient"));
    final String body = readString(object.get("body"));
    final String status = readString(object.get("status"));
    final String direction = readString(object.get("direction"));
    final BigDecimal price = readBigDecimal(object.get("price"));
    final String apiVersion = readString(object.get("api_version"));
    final URI uri = readUri(object.get("uri"));
    return new SmsMessage(sid, dateCreated, dateUpdated, dateSent, accountSid, sender, recipient,
        body, status, direction, price, apiVersion, uri);
  }
}
