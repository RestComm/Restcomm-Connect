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

import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readDateTime;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readSid;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readString;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readUri;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.writeDateTime;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.writeSid;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.writeUri;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.SandBoxesDao;
import org.mobicents.servlet.sip.restcomm.entities.SandBox;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
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
      session.commit();
    } finally {
      session.close();
    }
  }

  @Override public SandBox getSandBox(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
	  final Map<String, Object> result = session.selectOne(namespace + "getSandBox", accountSid.toString());
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
      session.commit();
    } finally {
      session.close();
    }
  }

  @Override public void updateSandBox(final SandBox sandBox) {
    final SqlSession session = sessions.openSession();
    try {
      session.update(namespace + "updateSandBox", toMap(sandBox));
      session.commit();
    } finally {
      session.close();
    }
  }
  
  private Map<String, Object> toMap(final SandBox sandBox) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("date_created", writeDateTime(sandBox.getDateCreated()));
    map.put("date_updated", writeDateTime(sandBox.getDateUpdated()));
    map.put("pin", sandBox.getPin());
    map.put("account_sid", writeSid(sandBox.getAccountSid()));
    map.put("phone_number", sandBox.getPhoneNumber());
    map.put("application_sid", writeSid(sandBox.getApplicationSid()));
    map.put("api_version", sandBox.getApiVersion());
    map.put("voice_url", writeUri(sandBox.getVoiceUrl()));
    map.put("voice_method", sandBox.getVoiceMethod());
    map.put("sms_url", writeUri(sandBox.getSmsUrl()));
    map.put("sms_method", sandBox.getSmsMethod());
    map.put("status_callback", writeUri(sandBox.getStatusCallback()));
    map.put("status_callback_method", sandBox.getStatusCallbackMethod());
    map.put("uri", writeUri(sandBox.getUri()));
    return map;
  }
  
  private SandBox toSandBox(final Map<String, Object> map) {
    final DateTime dateCreated = readDateTime(map.get("date_created"));
    final DateTime dateUpdated = readDateTime(map.get("date_updated"));
    final String pin = readString(map.get("pin"));
    final Sid accountSid = readSid(map.get("account_sid"));
    final String phoneNumber = readString(map.get("phone_number"));
    final Sid applicationSid = readSid(map.get("application_sid"));
    final String apiVersion = readString(map.get("api_version"));
    final URI voiceUrl = readUri(map.get("voice_url"));
    final String voiceMethod = readString(map.get("voice_method"));
    final URI smsUrl = readUri(map.get("sms_url"));
    final String smsMethod = readString(map.get("sms_method"));
    final URI statusCallback = readUri(map.get("status_callback"));
    final String statusCallbackMethod = readString(map.get("status_callback_method"));
    final URI uri = readUri(map.get("uri"));
    return new SandBox(dateCreated, dateUpdated, pin, accountSid, phoneNumber, applicationSid,
        apiVersion, voiceUrl, voiceMethod, smsUrl, smsMethod, statusCallback, statusCallbackMethod, uri);
  }
}
