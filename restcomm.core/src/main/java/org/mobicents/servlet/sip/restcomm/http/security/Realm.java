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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.permission.DomainPermission;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.AccountsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.entities.Account;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class Realm extends AuthorizingRealm {
  private volatile Map<String, SimpleRole> roles;
  
  public Realm() {
    super();
  }

  @Override protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
    final Sid sid = new Sid((String)principals.getPrimaryPrincipal());
    final ServiceLocator services = ServiceLocator.getInstance();
    final DaoManager daos = services.get(DaoManager.class);
    final AccountsDao accounts = daos.getAccountsDao();
    final Account account = accounts.getAccount(sid);
    final String roleName = account.getRole();
    final Set<String> set = new HashSet<String>();
    set.add(roleName);
    final SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo(set);
    final SimpleRole role = getRole(roleName);
    if(role != null) {
      authorizationInfo.setObjectPermissions(role.getPermissions());
    }
    return authorizationInfo;
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
    } catch(final Exception ignored) {
      return null;
    }
  }
  
  private SimpleRole getRole(final String role) {
    if(roles != null) {
      return roles.get(role);
    } else {
      synchronized(this) {
        if(roles == null) {
          roles = new HashMap<String, SimpleRole>();
          final ServiceLocator services = ServiceLocator.getInstance();
          final Configuration configuration = services.get(Configuration.class);
          loadSecurityRoles(configuration.subset("security-roles"));
        }
      }
      return roles.get(role);
    }
  }
  
  private void loadSecurityRoles(final Configuration configuration) {
    @SuppressWarnings("unchecked")
    final List<String> roleNames = (List<String>)configuration.getList("role[@name]");
    final int numberOfRoles = roleNames.size();
    if(numberOfRoles > 0) {
      for(int roleIndex = 0; roleIndex < numberOfRoles; roleIndex++) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("role(").append(roleIndex).append(")").toString();
      	final String prefix = buffer.toString();
      	final String name = configuration.getString(prefix + "[@name]");
      	@SuppressWarnings("unchecked")
      	final List<String> permissions = configuration.getList(prefix + ".permission");
      	final int numberOfPermissions = permissions.size();
      	if(name != null) {  
      	  if(numberOfPermissions > 0) {
      	    final SimpleRole role = new SimpleRole(name);
      	    for(int permissionIndex = 0; permissionIndex < numberOfPermissions; permissionIndex++) {
      	      buffer = new StringBuilder();
      	      buffer.append(prefix).append(".permission(").append(permissionIndex).append(")");
      	      final Permission permission = new DomainPermission(buffer.toString());
      	      role.add(permission);
      	    }
      	    roles.put(name, role);
      	  }
      	}
      }
    }
  }
}
