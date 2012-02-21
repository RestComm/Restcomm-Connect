package org.mobicents.servlet.sip.restcomm.dao.mybatis;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.SandBox;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.SandBoxesDao;

@ThreadSafe public final class MybatisSandBoxesDao implements SandBoxesDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.SandBoxesDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisSandBoxesDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }
  
  @Override public void addSandBox(final SandBox sandBox) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addSandBox", toMap(sandBox));
    } finally {
      session.close();
    }
  }

  @Override public SandBox getSandBox(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
	  final Map<String, Object> result = (Map<String, Object>)session.selectOne(namespace + "getSandBox", accountSid.toString());
      if(result != null) {
        return toSandBox(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }

  @Override public void removeSandBox(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(namespace + "removeSandBox", accountSid.toString());
    } finally {
      session.close();
    }
  }

  @Override public void updateSandBox(final SandBox sandBox) {
    final SqlSession session = sessions.openSession();
    try {
      session.update(namespace + "updateSandBox", toMap(sandBox));
    } finally {
      session.close();
    }
  }
  
  private Map<String, Object> toMap(final SandBox sandBox) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("date_created", sandBox.getDateCreated().toDate());
    map.put("date_updated", sandBox.getDateUpdated().toDate());
    map.put("pin", sandBox.getPin());
    map.put("account_sid", sandBox.getAccountSid().toString());
    map.put("phone_number", sandBox.getPhoneNumber());
    map.put("application_sid", sandBox.getApplicationSid().toString());
    map.put("api_version", sandBox.getApiVersion());
    map.put("voice_url", sandBox.getVoiceUrl().toString());
    map.put("voice_method", sandBox.getVoiceMethod());
    map.put("sms_url", sandBox.getSmsUrl().toString());
    map.put("sms_method", sandBox.getSmsMethod());
    map.put("status_callback", sandBox.getStatusCallback().toString());
    map.put("status_callback_method", sandBox.getStatusCallbackMethod());
    map.put("uri", sandBox.getUri().toString());
    return map;
  }
  
  private SandBox toSandBox(final Map<String, Object> map) {
    final DateTime dateCreated = new DateTime((Date)map.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)map.get("date_updated"));
    final String pin = (String)map.get("pin");
    final Sid accountSid = new Sid((String)map.get("account_sid"));
    final String phoneNumber = (String)map.get("phone_number");
    final Sid applicationSid = new Sid((String)map.get("application_sid"));
    final String apiVersion = (String)map.get("api_version");
    final URI voiceUrl = URI.create((String)map.get("voice_url"));
    final String voiceMethod = (String)map.get("voice_method");
    final URI smsUrl = URI.create((String)map.get("sms_url"));
    final String smsMethod = (String)map.get("sms_method");
    final URI statusCallback = URI.create((String)map.get("status_callback"));
    final String statusCallbackMethod = (String)map.get("status_callback_method");
    final URI uri = URI.create((String)map.get("uri"));
    return new SandBox(dateCreated, dateUpdated, pin, accountSid, phoneNumber, applicationSid,
        apiVersion, voiceUrl, voiceMethod, smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
}
