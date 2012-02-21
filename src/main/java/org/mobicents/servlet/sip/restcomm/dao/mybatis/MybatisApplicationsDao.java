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

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Application;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ApplicationsDao;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MybatisApplicationsDao implements ApplicationsDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ApplicationsDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisApplicationsDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }

  @Override public void addApplication(final Application application) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addApplication", toMap(application));
    } finally {
      session.close();
    }
  }

  @Override public Application getApplication(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final Map<String, Object> result = (Map<String, Object>)session.selectOne(namespace + "getApplication", sid.toString());
      if(result != null) {
        return toApplication(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }

  @Override public List<Application> getApplications(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(namespace + "getApplications", accountSid.toString());
      final List<Application> applications = new ArrayList<Application>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          applications.add(toApplication(result));
        }
      }
      return applications;
    } finally {
      session.close();
    }
  }

  @Override public void removeApplication(final Sid sid) {
    removeApplications("removeApplication", sid);
  }

  @Override public void removeApplications(final Sid accountSid) {
    removeApplications("removeApplications", accountSid);
  }
  
  private void removeApplications(final String selector, final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(namespace + selector, sid.toString());
    } finally {
      session.close();
    }
  }

  @Override public void updateApplication(final Application application) {
    final SqlSession session = sessions.openSession();
    try {
      session.update(namespace + "updateApplication", toMap(application));
    } finally {
      session.close();
    }
  }
  
  private Application toApplication(final Map<String, Object> map) {
	final Sid sid = new Sid((String)map.get("sid"));
	final DateTime dateCreated = new DateTime((Date)map.get("date_created"));
	final DateTime dateUpdated = new DateTime((Date)map.get("date_updated"));
	final String friendlyName = (String)map.get("friendly_name");
	final Sid accountSid = new Sid((String)map.get("account_sid"));
	final String apiVersion = (String)map.get("api_version");
	final URI voiceUrl = URI.create((String)map.get("voice_url"));
	final String voiceMethod = (String)map.get("voice_method");
	final URI voiceFallbackUrl = URI.create((String)map.get("voice_fallback_url"));
	final String voiceFallbackMethod = (String)map.get("voice_fallback_method");
	final URI statusCallback = URI.create((String)map.get("status_callback"));
	final String statusCallbackMethod = (String)map.get("status_callback_method");
	final Boolean hasVoiceCallerIdLookup = (Boolean)map.get("voice_caller_id_lookup");
	final URI smsUrl = URI.create((String)map.get("sms_url"));
	final String smsMethod = (String)map.get("sms_method");
	final URI smsFallbackUrl = URI.create((String)map.get("sms_fallback_url"));
	final String smsFallbackMethod = (String)map.get("sms_fallback_method");
	final URI smsStatusCallback = URI.create((String)map.get("sms_status_callback"));
	final URI uri = URI.create((String)map.get("uri"));
    return new Application(sid, dateCreated, dateUpdated, friendlyName, accountSid, apiVersion,
        voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod,
        hasVoiceCallerIdLookup, smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, smsStatusCallback,
        uri);
  }
  
  private Map<String, Object> toMap(final Application application) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("sid", application.getSid().toString());
    map.put("date_created", application.getDateCreated().toDate());
    map.put("date_updated", application.getDateUpdated().toDate());
    map.put("friendly_name", application.getFriendlyName());
    map.put("account_sid", application.getAccountSid().toString());
    map.put("api_version", application.getApiVersion());
    map.put("voice_url", application.getVoiceUrl().toString());
    map.put("voice_method", application.getVoiceMethod());
    map.put("voice_fallback_url", application.getVoiceFallbackUrl().toString());
    map.put("voice_fallback_method", application.getVoiceFallbackMethod());
    map.put("status_callback", application.getStatusCallback().toString());
    map.put("status_callback_method", application.getStatusCallbackMethod());
    map.put("voice_caller_id_lookup", application.hasVoiceCallerIdLookup());
    map.put("sms_url", application.getSmsUrl().toString());
    map.put("sms_method", application.getSmsMethod());
    map.put("sms_fallback_url", application.getSmsFallbackUrl().toString());
    map.put("sms_fallback_method", application.getSmsFallbackMethod());
    map.put("sms_status_callback", null);
    map.put("uri", application.getUri().toString());
    return map;
  }
}
