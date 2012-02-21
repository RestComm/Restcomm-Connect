package org.mobicents.servlet.sip.restcomm.dao.mybatis;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.CallDetailRecord;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.CallDetailRecordDao;

@ThreadSafe public final class MybatisCallDetailRecordDao implements CallDetailRecordDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.CallDetailRecordDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisCallDetailRecordDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }

  @Override public void addCallDetailRecord(final CallDetailRecord cdr) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addCallDetailRecord", toMap(cdr));
    } finally {
      session.close();
    }
  }

  @Override public CallDetailRecord getCallDetailRecord(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
	  final Map<String, Object> result = (Map<String, Object>)session.selectOne(namespace + "getCallDetailRecord", sid.toString());
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
      @SuppressWarnings("unchecked")
	  final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(selector, input);
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
    } finally {
      session.close();
    }
  }

  @Override public void updateCallDetailRecord(final CallDetailRecord cdr) {
    final SqlSession session = sessions.openSession();
    try {
      session.update(namespace + "updateCallDetailRecord", toMap(cdr));
    } finally {
      session.close();
    }
  }
  
  private CallDetailRecord toCallDetailRecord(final Map<String, Object> map) {
    final Sid sid = new Sid((String)map.get("sid"));
    final Sid parentCallSid = new Sid((String)map.get("parent_call_sid"));
    final DateTime dateCreated = new DateTime((Date)map.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)map.get("date_updated"));
    final Sid accountSid = new Sid((String)map.get("account_sid"));
    final String to = (String)map.get("to");
    final String from = (String)map.get("from");
    final Sid phoneNumberSid = new Sid((String)map.get("phone_number_sid"));
    final String status = (String)map.get("status");
    final DateTime startTime = new DateTime((Date)map.get("start_time"));
    final DateTime endTime = new DateTime((Date)map.get("end_time"));
    final Integer duration = (Integer)map.get("duration");
    final BigDecimal price = new BigDecimal((String)map.get("price"));
    final String answeredBy = (String)map.get("answered_by");
    final String forwardedFrom = (String)map.get("forwarded_from");
    final String callerName = (String)map.get("caller_name");
    final URI uri = URI.create((String)map.get("uri"));
    return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid, to, from, phoneNumberSid, status,
        startTime, endTime, duration, price, answeredBy, forwardedFrom, callerName, uri);
  }
  
  private Map<String, Object> toMap(final CallDetailRecord cdr) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("sid", cdr.getSid().toString());
    map.put("parent_call_sid", cdr.getParentCallSid().toString());
    map.put("date_created", cdr.getDateCreated().toDate());
    map.put("date_updated", cdr.getDateUpdated().toDate());
    map.put("account_sid", cdr.getAccountSid().toString());
    map.put("to", cdr.getTo());
    map.put("from", cdr.getFrom());
    map.put("phone_number_sid", cdr.getPhoneNumberSid().toString());
    map.put("status", cdr.getStatus());
    map.put("start_time", cdr.getStartTime().toDate());
    map.put("end_time", cdr.getEndTime().toDate());
    map.put("duration", cdr.getDuration());
    map.put("price", cdr.getPrice().toString());
    map.put("answered_by", cdr.getAnsweredBy());
    map.put("forwarded_from", cdr.getForwardedFrom());
    map.put("caller_name", cdr.getCallerName());
    map.put("uri", cdr.getUri().toString());
    return map;
  }
}
