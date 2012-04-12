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
package org.mobicents.servlet.sip.restcomm.dao.mybatis;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.SmsMessage;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MybatisSmsMessagesDao implements SmsMessagesDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.SmsMessagesDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisSmsMessagesDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }
  
  @Override public void addSmsMessage(final SmsMessage smsMessage) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addSmsMessage", toMap(smsMessage));
    } finally {
      session.close();
    }
  }

  @Override public SmsMessage getSmsMessage(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final Map<String, Object> result = (Map<String, Object>)session.selectOne(namespace + "getSmsMessage", sid.toString());
      if(result != null) {
        return toSmsMessage(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }

  @Override public List<SmsMessage> getSmsMessages(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(namespace + "getSmsMessages", accountSid.toString());
      final List<SmsMessage> smsMessages = new ArrayList<SmsMessage>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          smsMessages.add(toSmsMessage(result));
        }
      }
      return smsMessages;
    } finally {
      session.close();
    }
  }

  @Override public void removeSmsMessage(final Sid sid) {
    deleteSmsMessage(namespace + "removeSmsMessage", sid);
  }

  @Override public void removeSmsMessages(final Sid accountSid) {
    deleteSmsMessage(namespace + "removeSmsMessages", accountSid);
  }
  
  private void deleteSmsMessage(final String selector, final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(selector, sid.toString());
    } finally {
      session.close();
    }
  }
  
  public void updateSmsMessage(final SmsMessage smsMessage) {
    
  }
  
  private Map<String, Object> toMap(final SmsMessage smsMessage) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("sid", writeSid(smsMessage.getSid()));
    map.put("date_created", writeDateTime(smsMessage.getDateCreated()));
    map.put("date_updated", writeDateTime(smsMessage.getDateUpdated()));
    map.put("date_sent", writeDateTime(smsMessage.getDateSent()));
    map.put("account_sid", writeSid(smsMessage.getAccountSid()));
    map.put("sender", smsMessage.getSender());
    map.put("recipient", smsMessage.getRecipient());
    map.put("body", smsMessage.getBody());
    map.put("status", smsMessage.getStatus().toString());
    map.put("direction", smsMessage.getDirection().toString());
    map.put("price", writeBigDecimal(smsMessage.getPrice()));
    map.put("api_version", smsMessage.getApiVersion());
    map.put("uri", writeUri(smsMessage.getUri()));
    return map;
  }
  
  private SmsMessage toSmsMessage(final Map<String, Object> map) {
    final Sid sid = readSid(map.get("sid"));
    final DateTime dateCreated = readDateTime(map.get("date_created"));
    final DateTime dateUpdated = readDateTime(map.get("date_updated"));
    final DateTime dateSent = readDateTime(map.get("date_sent"));
    final Sid accountSid = readSid(map.get("account_sid"));
    final String sender = readString(map.get("sender"));
    final String recipient = readString(map.get("recipient"));
    final String body = readString(map.get("body"));
    final SmsMessage.Status status = SmsMessage.Status.getStatusValue(readString(map.get("status")));
    final SmsMessage.Direction direction = SmsMessage.Direction.getDirectionValue(readString(map.get("direction")));
    final BigDecimal price = readBigDecimal(map.get("price"));
    final String apiVersion = readString(map.get("api_version"));
    final URI uri = readUri(map.get("uri"));
    return new SmsMessage(sid, dateCreated, dateUpdated, dateSent, accountSid, sender, recipient, body, status,
        direction, price, apiVersion, uri);
  }
}
