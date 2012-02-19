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
import org.mobicents.servlet.sip.restcomm.Account;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AccountDao;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public class MybatisAccountDao implements AccountDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.AccountDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisAccountDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }
  
  @Override public void addAccount(final Account account) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addAccount", toMap(account));
    } finally {
      session.close();
    }
  }
  
  @Override public void addSubAccount(final Sid primaryAccountSid, final Account subAccount) {
	final Map<String, Object> parameters = toMap(subAccount);
	parameters.put("account_sid", primaryAccountSid.toString());
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addSubAccount", parameters);
    } finally {
      session.close();
    }
  }

  @Override public Account getAccount(final Sid sid) {
    return getAccount(namespace + "getAccount", sid);
  }
  
  @Override public Account getSubAccount(final Sid sid) {
    return getAccount(namespace + "getSubAccount", sid);
  }
  
  private Account getAccount(final String selector, final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
	  @SuppressWarnings("unchecked")
      final Map<String, Object> result = (Map<String, Object>)session.selectOne(selector, sid.toString());
      if(result != null) {
        return toAccount(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }
  
  @Override public List<Account> getSubAccounts(final Sid primaryAccountSid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(namespace + "getSubAccounts", primaryAccountSid.toString());
      final List<Account> accounts = new ArrayList<Account>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          accounts.add(toAccount(result));
        }
      }
      return accounts;
    } finally {
      session.close();
    }
  }

  @Override public void removeAccount(final Sid sid) {
    removeAccount(namespace + "removeAccount", sid);
  }
  
  @Override public void removeSubAccount(final Sid sid) {
    removeAccount(namespace + "removeSubAccount", sid);
  }
  
  private void removeAccount(final String selector, final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(selector, sid.toString());
    } finally {
      session.close();
    }
  }

  @Override public void updateAccount(final Account account) {
    updateAccount(namespace + "updateAccount", account);
  }

  @Override public void updateSubAccount(final Account account) {
    updateAccount(namespace + "updateSubAccount", account);
  }
  
  private void updateAccount(final String selector, final Account account) {
    final SqlSession session = sessions.openSession();
    try {
      session.update(selector, toMap(account));
    } finally {
      session.close();
    }
  }
  
  private Account toAccount(final Map<String, Object> map) {
	final Sid sid = new Sid((String)map.get("sid"));
	final DateTime dateCreated = new DateTime((Date)map.get("date_created"));
	final DateTime dateUpdated = new DateTime((Date)map.get("date_updated"));
	final String friendlyName = (String)map.get("friendly_name");
	final Account.Type type = Account.Type.valueOf((String)map.get("type"));
	final Account.Status status = Account.Status.valueOf((String)map.get("status"));
	final String authToken = (String)map.get("auth_token");
	final URI uri = URI.create((String)map.get("uri"));
    return new Account(sid, dateCreated, dateUpdated, friendlyName, type, status, authToken, uri);
  }
  
  private Map<String, Object> toMap(final Account account) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("sid", account.getSid().toString());
    map.put("date_created", account.getDateCreated().toDate());
    map.put("date_updated", account.getDateUpdated().toDate());
    map.put("friendly_name", account.getFriendlyName());
    map.put("type", account.getType().toString());
    map.put("status", account.getStatus().toString());
    map.put("auth_token", account.getAuthToken());
    map.put("uri", account.getUri().toString());
    return map;
  }
}
