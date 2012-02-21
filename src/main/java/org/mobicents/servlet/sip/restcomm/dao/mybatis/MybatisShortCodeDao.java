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

import org.mobicents.servlet.sip.restcomm.ShortCode;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ShortCodeDao;

@ThreadSafe public final class MybatisShortCodeDao implements ShortCodeDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ShortCodeDao.";
  private final SqlSessionFactory sessions;

  public MybatisShortCodeDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }
  
  @Override public void addShortCode(final ShortCode shortCode) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addShortCode", toMap(shortCode));
    } finally {
      session.close();
    }
  }

  @Override public ShortCode getShortCode(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final Map<String, Object> result = (Map<String, Object>)session.selectOne(namespace + "getShortCode", sid.toString());
      if(result != null) {
        return toShortCode(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }

  @Override public List<ShortCode> getShortCodes(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(namespace + "getShortCodes", accountSid.toString());
      final List<ShortCode> shortCodes = new ArrayList<ShortCode>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          shortCodes.add(toShortCode(result));
        }
      }
      return shortCodes;
    } finally {
      session.close();
    }
  }

  @Override public void removeShortCode(final Sid sid) {
    removeShortCodes(namespace + "removeShortCode", sid);
  }

  @Override public void removeShortCodes(final Sid accountSid) {
    removeShortCodes(namespace + "removeShortCodes", accountSid);
  }
  
  private void removeShortCodes(final String selector, final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(selector, sid.toString());
    } finally {
      session.close();
    }
  }

  @Override public void updateShortCode(final ShortCode shortCode) {
    final SqlSession session = sessions.openSession();
    try {
      session.update(namespace + "updateShortCode", toMap(shortCode));
    } finally {
      session.close();
    }
  }
  
  private Map<String, Object> toMap(final ShortCode shortCode) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("sid", shortCode.getSid().toString());
    map.put("date_created", shortCode.getDateCreated().toDate());
    map.put("date_updated", shortCode.getDateUpdated().toDate());
    map.put("friendly_name", shortCode.getFriendlyName());
    map.put("account_sid", shortCode.getAccountSid().toString());
    map.put("short_code", shortCode.getShortCode());
    map.put("api_version", shortCode.getApiVersion());
    map.put("sms_url", shortCode.getSmsUrl().toString());
    map.put("sms_method", shortCode.getSmsMethod());
    map.put("sms_fallback_url", shortCode.getSmsFallbackUrl().toString());
    map.put("sms_fallback_method", shortCode.getSmsFallbackMethod());
    map.put("uri", shortCode.getUri().toString());
    return map;
  }
  
  private ShortCode toShortCode(final Map<String, Object> map) {
    final Sid sid = new Sid((String)map.get("sid"));
    final DateTime dateCreated = new DateTime((Date)map.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)map.get("date_updated"));
    final String friendlyName = (String)map.get("friendly_name");
    final Sid accountSid = new Sid((String)map.get("account_sid"));
    final Integer shortCode = (Integer)map.get("short_code");
    final String apiVersion = (String)map.get("api_version");
    final URI smsUrl = URI.create((String)map.get("sms_url"));
    final String smsMethod = (String)map.get("sms_method");
    final URI smsFallbackUrl = URI.create((String)map.get("sms_fallback_url"));
    final String smsFallbackMethod = (String)map.get("sms_fallback_method");
    final URI uri = URI.create((String)map.get("uri"));
    return new ShortCode(sid, dateCreated, dateUpdated, friendlyName, accountSid, shortCode,
        apiVersion, smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, uri);
  }
}
