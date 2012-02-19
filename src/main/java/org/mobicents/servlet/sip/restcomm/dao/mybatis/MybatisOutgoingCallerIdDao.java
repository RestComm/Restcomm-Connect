package org.mobicents.servlet.sip.restcomm.dao.mybatis;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.OutgoingCallerId;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.OutgoingCallerIdDao;

<<<<<<< HEAD
@ThreadSafe public final class MybatisOutgoingCallerIdDao implements OutgoingCallerIdDao {
=======
@ThreadSafe public class MybatisOutgoingCallerIdDao implements OutgoingCallerIdDao {
>>>>>>> d0ac460011b93fe26e2a31da4754d6b84d4eb933
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.OutgoingCallerIdDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisOutgoingCallerIdDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }
  
  @Override public void addOutgoingCallerId(final OutgoingCallerId outgoingCallerId) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addOutgoingCallerId", toMap(outgoingCallerId));
    } finally {
      session.close();
    }
  }

  @Override public OutgoingCallerId getOutgoingCallerId(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final Map<String, Object> result = (Map<String, Object>)session.selectOne(namespace + "getOutgoingCallerId", sid.toString());
      if(result != null) {
        return toOutgoingCallerId(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }
  
  @Override public List<OutgoingCallerId> getOutgoingCallerIds(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(namespace + "getOutgoingCallerIds", accountSid.toString());
      final List<OutgoingCallerId> outgoingCallerIds = new ArrayList<OutgoingCallerId>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          outgoingCallerIds.add(toOutgoingCallerId(result));
        }
      }
      return outgoingCallerIds;
    } finally {
      session.close();
    }
  }

  @Override public void removeOutgoingCallerId(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(namespace + "removeOutgoingCallerId", sid.toString());
    } finally {
      session.close();
    }
  }
  
  @Override public void removeOutgoingCallerIds(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(namespace + "removeOutgoingCallerIds", accountSid.toString());
    } finally {
      session.close();
    }
  }

  @Override public void updateOutgoingCallerId(final OutgoingCallerId outgoingCallerId) {
    final SqlSession session = sessions.openSession();
    try {
      session.update(namespace + "updateOutgoingCallerId", toMap(outgoingCallerId));
    } finally {
      session.close();
    }
  }
  
  private Map<String, Object> toMap(final OutgoingCallerId outgoingCallerId) {
	final Map<String, Object> map = new HashMap<String, Object>();
	map.put("sid", outgoingCallerId.getSid().toString());
	map.put("date_created", outgoingCallerId.getDateCreated().toDate());
	map.put("date_updated", outgoingCallerId.getDateUpdated().toDate());
	map.put("friendly_name", outgoingCallerId.getFriendlyName());
	map.put("account_sid", outgoingCallerId.getAccountSid().toString());
	map.put("phone_number", outgoingCallerId.getPhoneNumber());
	map.put("uri", outgoingCallerId.getUri().toString());
    return map;
  }
  
  private OutgoingCallerId toOutgoingCallerId(final Map<String, Object> map) {
    final Sid sid = new Sid((String)map.get("sid"));
    final DateTime dateCreated = new DateTime((Date)map.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)map.get("date_updated"));
    final String friendlyName = (String)map.get("friendly_name");
    final Sid accountSid = new Sid((String)map.get("account_sid"));
    final String phoneNumber = (String)map.get("phone_number");
    final URI uri = URI.create((String)map.get("uri"));
    return new OutgoingCallerId(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, uri);
  }
}
