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
package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Account;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AccountsDao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoAccountsDao implements AccountsDao {
  private final DBCollection accounts;
  private final DBCollection subAccounts;

  public MongoAccountsDao(final DB database) {
    super();
    accounts = database.getCollection("restcomm_accounts");
    subAccounts = database.getCollection("restcomm_sub_accounts");
  }

  @Override public void addAccount(final Account account) {
    accounts.insert(toDbObject(account));
  }
  
  @Override public void addSubAccount(final Sid primaryAccountSid, final Account subAccount) {
    final DBObject object = toDbObject(subAccount);
    object.put("account_sid", primaryAccountSid.toString());
    subAccounts.insert(object);
  }

  @Override public Account getAccount(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    final DBObject result = accounts.findOne(query);
    if(result != null) {
      return toAccount(result);
    } else {
      return null;
    }
  }
  
  @Override public Account getSubAccount(final Sid sid) {
    return null;
  }
  
  @Override public List<Account> getSubAccounts(final Sid sid) {
    return null;
  }

  @Override public void removeAccount(final Sid sid) {
    
  }
  
  @Override public void removeSubAccount(final Sid sid) {
    
  }

  @Override public void updateAccount(final Account account) {
    
  }

  @Override public void updateSubAccount(final Account account) {
    
  }
  
  private Account toAccount(final DBObject object) {
	final Sid sid = new Sid((String)object.get("sid"));
	final DateTime dateCreated = new DateTime((Date)object.get("date_created"));
	final DateTime dateUpdated = new DateTime((Date)object.get("date_updated"));
	final String friendlyName = (String)object.get("friendly_name");
	final Account.Type type = Account.Type.getValueOf((String)object.get("type"));
	final Account.Status status = Account.Status.getValueOf((String)object.get("status"));
	final String authToken = (String)object.get("auth_token");
	final URI uri = URI.create((String)object.get("uri"));
    return new Account(sid, dateCreated, dateUpdated, friendlyName, type, status, authToken, uri);
  }
  
  private DBObject toDbObject(final Account account) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", account.getSid());
    object.put("date_created", account.getDateCreated().toDate());
    object.put("date_updated", account.getDateUpdated().toDate());
    object.put("friendly_name", account.getFriendlyName());
    object.put("type", account.getType().toString());
    object.put("status", account.getStatus().toString());
    object.put("auth_token", account.getAuthToken());
    object.put("uri", account.getUri().toString());
    return object;
  }
}
