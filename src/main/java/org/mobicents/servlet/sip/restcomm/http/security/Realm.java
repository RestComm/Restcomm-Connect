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
package org.mobicents.servlet.sip.restcomm.http.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import org.mobicents.servlet.sip.restcomm.Account;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AccountsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class Realm extends AuthorizingRealm {
  public Realm() {
    super();
  }

  @Override protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
    return null;
  }

  @Override protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token)
      throws AuthenticationException {
    final UsernamePasswordToken authenticationToken = (UsernamePasswordToken)token;
    Sid sid = null;
    try {
      sid = new Sid(authenticationToken.getUsername());
      final ServiceLocator services = ServiceLocator.getInstance();
      final DaoManager daos = services.get(DaoManager.class);
      final AccountsDao accounts = daos.getAccountsDao();
      final Account account = accounts.getAccount(sid);
      final String authToken = account.getAuthToken();
      return new SimpleAuthenticationInfo(sid.toString(), authToken.toCharArray(), getName());
    } catch(final IllegalArgumentException ignored) {
      return null;
    }
  }
}
