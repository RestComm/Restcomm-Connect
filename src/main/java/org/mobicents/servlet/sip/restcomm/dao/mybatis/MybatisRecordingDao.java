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
import org.mobicents.servlet.sip.restcomm.Recording;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.RecordingDao;

@ThreadSafe public final class MybatisRecordingDao implements RecordingDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.RecordingDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisRecordingDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }
	
  @Override public void addRecording(final Recording recording) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addRecording", toMap(recording));
    } finally {
      session.close();
    }
  }

  @Override public Recording getRecording(final Sid sid) {
    return getRecording(namespace + "getRecording", sid);
  }

  @Override public Recording getRecordingByCall(final Sid callSid) {
    return getRecording(namespace + "getRecordingByCall", callSid);
  }
  
  private Recording getRecording(final String selector, final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final Map<String, Object> result = (Map<String, Object>)session.selectOne(selector, sid.toString());
      if(result != null) {
        return toRecording(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }

  @Override public List<Recording> getRecordings(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(namespace + "getRecordings", accountSid.toString());
      final List<Recording> recordings = new ArrayList<Recording>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          recordings.add(toRecording(result));
        }
      }
      return recordings;
    } finally {
     session.close();
    }
  }

  @Override public void removeRecording(final Sid sid) {
    removeRecording(namespace + "removeRecording", sid);
  }

  @Override public void removeRecordings(final Sid accountSid) {
    removeRecording(namespace + "removeRecordings", accountSid);
  }
  
  private void removeRecording(final String selector, final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(selector, sid.toString());
    } finally {
      session.close();
    }
  }
  
  private Map<String, Object> toMap(final Recording recording) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("sid", recording.getSid().toString());
    map.put("date_created", recording.getDateCreated().toDate());
    map.put("date_updated", recording.getDateUpdated().toDate());
    map.put("account_sid", recording.getAccountSid().toString());
    map.put("call_sid", recording.getCallSid().toString());
    map.put("duration", recording.getDuration());
    map.put("api_version", recording.getApiVersion());
    map.put("uri", recording.getUri().toString());
    return map;
  }
  
  private Recording toRecording(final Map<String, Object> map) {
    final Sid sid = new Sid((String)map.get("sid"));
    final DateTime dateCreated = new DateTime((Date)map.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)map.get("date_updated"));
    final Sid accountSid = new Sid((String)map.get("account_sid"));
    final Sid callSid = new Sid((String)map.get("call_sid"));
    final Integer duration = (Integer)map.get("duration");
    final String apiVersion = (String)map.get("api_version");
    final URI uri = URI.create((String)map.get("uri"));
    return new Recording(sid, dateCreated, dateUpdated, accountSid, callSid, duration, apiVersion, uri);
  }
}
