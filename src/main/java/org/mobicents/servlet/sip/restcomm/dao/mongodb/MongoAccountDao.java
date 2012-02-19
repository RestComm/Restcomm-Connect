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

import java.util.List;

import org.mobicents.servlet.sip.restcomm.Account;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AccountDao;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public class MongoAccountDao implements AccountDao {
  public MongoAccountDao() {
    super();
  }

  @Override public void addAccount(final Account account) {
    
  }
  
  @Override public void addSubAccount(final Sid primaryAccountSid, final Account subAccount) {
    
  }

  @Override public Account getAccount(final Sid sid) {
    return null;
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
}
