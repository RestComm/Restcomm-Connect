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

import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readInteger;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.readString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.callmanager.presence.PresenceRecord;
import org.mobicents.servlet.sip.restcomm.dao.PresenceRecordsDao;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MybatisPresenceRecordsDao implements PresenceRecordsDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.PresenceRecordsDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisPresenceRecordsDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }
	
  @Override public void addPresenceRecord(final PresenceRecord record) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addPresenceRecord", toMap(record));
    } finally {
      session.close();
    }
  }

  @Override public List<PresenceRecord> getPresenceRecords(final String aor) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(namespace + "getPresenceRecords", aor);
      final List<PresenceRecord> records = new ArrayList<PresenceRecord>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          records.add(toPresenceRecord(result));
        }
      }
      return records;
    } finally {
     session.close();
    }
  }
  
  @Override public boolean hasPresenceRecord(final String aor) {
    final SqlSession session = sessions.openSession();
    try {
      final Integer result = (Integer)session.selectOne(namespace + "hasPresenceRecord", aor);
      return result != null && result > 0;
    } finally {
      session.close();
    }
  }

  @Override public void removePresenceRecord(final String uri) {
    removePresenceRecords(namespace + "removePresenceRecord", uri);
  }

  @Override public void removePresenceRecords(final String aor) {
    removePresenceRecords(namespace + "removePresenceRecords", aor);
  }
  
  private void removePresenceRecords(final String selector, final String parameter) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(selector, parameter);
    } finally {
      session.close();
    }
  }

  @Override public void updatePresenceRecord(final PresenceRecord record) {
    
  }
  
  private Map<String, Object> toMap(final PresenceRecord record) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("address_of_record", record.getAddressOfRecord());
    map.put("display_name", record.getDisplayName());
    map.put("uri", record.getUri());
    map.put("user_agent", record.getUserAgent());
    map.put("ttl", record.getTimeToLive());
    return map;
  }
  
  private PresenceRecord toPresenceRecord(final Map<String, Object> map) {
    final String aor = readString(map.get("address_of_record"));
    final String name = readString(map.get("display_name"));
    final String uri = readString(map.get("uri"));
    final String ua = readString(map.get("user_agent"));
    final Integer ttl = readInteger(map.get("ttl"));
    return new PresenceRecord(aor, name, uri, ua, ttl);
  }
}
