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
package org.mobicents.servlet.restcomm.dao.mybatis;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import org.joda.time.DateTime;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.*;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MybatisCallDetailRecordsDao implements CallDetailRecordsDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.CallDetailRecordsDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisCallDetailRecordsDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }

  @Override public void addCallDetailRecord(final CallDetailRecord cdr) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addCallDetailRecord", toMap(cdr));
      session.commit();
    } finally {
      session.close();
    }
  }

  @Override public CallDetailRecord getCallDetailRecord(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
	  final Map<String, Object> result = session.selectOne(namespace + "getCallDetailRecord", sid.toString());
      if(result != null) {
        return toCallDetailRecord(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }

  @Override public List<CallDetailRecord> getCallDetailRecords(final Sid accountSid) {
    return getCallDetailRecords(namespace + "getCallDetailRecords", accountSid.toString());
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsByRecipient(final String recipient) {
    return getCallDetailRecords(namespace + "getCallDetailRecordsByRecipient", recipient);
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsBySender(final String sender) {
    return getCallDetailRecords(namespace + "getCallDetailRecordsBySender", sender);
  }

  @Override	public List<CallDetailRecord> getCallDetailRecordsByStatus(final String status) {
    return getCallDetailRecords(namespace + "getCallDetailRecordsByStatus", status);
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsByStartTime(final DateTime startTime) {
    return getCallDetailRecords(namespace + "getCallDetailRecordsByStartTime", startTime.toDate());
  }

  @Override public List<CallDetailRecord> getCallDetailRecordsByParentCall(final Sid parentCallSid) {
    return getCallDetailRecords(namespace + "getCallDetailRecordsByParentCall", parentCallSid.toString());
  }
  
  private List<CallDetailRecord> getCallDetailRecords(final String selector, Object input) {
    final SqlSession session = sessions.openSession();
    try {
      final List<Map<String, Object>> results = session.selectList(selector, input);
      final List<CallDetailRecord> cdrs = new ArrayList<CallDetailRecord>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          cdrs.add(toCallDetailRecord(result));
        }
      }
      return cdrs;
    } finally {
      session.close();
    }
  }

  @Override public void removeCallDetailRecord(final Sid sid) {
    removeCallDetailRecords(namespace + "removeCallDetailRecord", sid);
  }

  @Override public void removeCallDetailRecords(final Sid accountSid) {
    removeCallDetailRecords(namespace + "removeCallDetailRecords", accountSid);
  }
  
  private void removeCallDetailRecords(final String selector, final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(selector, sid.toString());
      session.commit();
    } finally {
      session.close();
    }
  }

  @Override public void updateCallDetailRecord(final CallDetailRecord cdr) {
    final SqlSession session = sessions.openSession();
    try {
      session.update(namespace + "updateCallDetailRecord", toMap(cdr));
      session.commit();
    } finally {
      session.close();
    }
  }
  
  private CallDetailRecord toCallDetailRecord(final Map<String, Object> map) {
    final Sid sid = readSid(map.get("sid"));
    final Sid parentCallSid = readSid(map.get("parent_call_sid"));
    final DateTime dateCreated = readDateTime(map.get("date_created"));
    final DateTime dateUpdated = readDateTime(map.get("date_updated"));
    final Sid accountSid = readSid(map.get("account_sid"));
    final String to = readString(map.get("recipient"));
    final String from = readString(map.get("sender"));
    final Sid phoneNumberSid = readSid(map.get("phone_number_sid"));
    final String status = readString(map.get("status"));
    final DateTime startTime = readDateTime(map.get("start_time"));
    final DateTime endTime = readDateTime(map.get("end_time"));
    final Integer duration = readInteger(map.get("duration"));
    final BigDecimal price = readBigDecimal(map.get("price"));
    final String direction = readString(map.get("direction"));
    final String answeredBy = readString(map.get("answered_by"));
    final String apiVersion = readString(map.get("api_version"));
    final String forwardedFrom = readString(map.get("forwarded_from"));
    final String callerName = readString(map.get("caller_name"));
    final URI uri = readUri(map.get("uri"));
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, to, from, phoneNumberSid, status,
        startTime, endTime, duration, price, direction, answeredBy, apiVersion, forwardedFrom, callerName, uri);
  }
  
  private Map<String, Object> toMap(final CallDetailRecord cdr) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("sid", writeSid(cdr.getSid()));
    map.put("parent_call_sid", writeSid(cdr.getParentCallSid()));
    map.put("date_created", writeDateTime(cdr.getDateCreated()));
    map.put("date_updated", writeDateTime(cdr.getDateUpdated()));
    map.put("account_sid", writeSid(cdr.getAccountSid()));
    map.put("to", cdr.getTo());
    map.put("from", cdr.getFrom());
    map.put("phone_number_sid", writeSid(cdr.getPhoneNumberSid()));
    map.put("status", cdr.getStatus());
    map.put("start_time", writeDateTime(cdr.getStartTime()));
    map.put("end_time", writeDateTime(cdr.getEndTime()));
    map.put("duration", cdr.getDuration());
    map.put("price", writeBigDecimal(cdr.getPrice()));
    map.put("direction", cdr.getDirection());
    map.put("answered_by", cdr.getAnsweredBy());
    map.put("api_version", cdr.getApiVersion());
    map.put("forwarded_from", cdr.getForwardedFrom());
    map.put("caller_name", cdr.getCallerName());
    map.put("uri", writeUri(cdr.getUri()));
    return map;
  }
}
