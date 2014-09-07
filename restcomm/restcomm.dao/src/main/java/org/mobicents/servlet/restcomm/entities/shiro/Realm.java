/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.entities.shiro;

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

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class Realm extends AuthorizingRealm {
    private volatile Map<String, SimpleRole> roles;

    public Realm() {
        super();
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
        final Sid sid = new Sid((String) principals.getPrimaryPrincipal());
        final ShiroResources services = ShiroResources.getInstance();
        final DaoManager daos = services.get(DaoManager.class);
        final AccountsDao accounts = daos.getAccountsDao();
        final Account account = accounts.getAccount(sid);
        final String roleName = account.getRole();
        final Set<String> set = new HashSet<String>();
        set.add(roleName);
        final SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo(set);
        final SimpleRole role = getRole(roleName);
        if (role != null) {
            authorizationInfo.setObjectPermissions(role.getPermissions());
        }
        return authorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) throws AuthenticationException {
        final UsernamePasswordToken authenticationToken = (UsernamePasswordToken) token;
        String username = authenticationToken.getUsername();
        Sid sid = null;
        Account account = null;
        String authToken = null;

        final ShiroResources services = ShiroResources.getInstance();
        final DaoManager daos = services.get(DaoManager.class);
        final AccountsDao accounts = daos.getAccountsDao();

        try {
            if (Sid.pattern.matcher(username).matches()) {
                sid = new Sid(username);
                account = accounts.getAccount(sid);
            } else {
                account = accounts.getAccount(username);
                sid = account.getSid();
            }

            if (account != null) {
                authToken = account.getAuthToken();
                return new SimpleAuthenticationInfo(sid.toString(), authToken.toCharArray(), getName());
            } else {
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private SimpleRole getRole(final String role) {
        if (roles != null) {
            return roles.get(role);
        } else {
            synchronized (this) {
                if (roles == null) {
                    roles = new HashMap<String, SimpleRole>();
                    final ShiroResources services = ShiroResources.getInstance();
                    final Configuration configuration = services.get(Configuration.class);
                    loadSecurityRoles(configuration.subset("security-roles"));
                }
            }
            return roles.get(role);
        }
    }

    private void loadSecurityRoles(final Configuration configuration) {
        @SuppressWarnings("unchecked")
        final List<String> roleNames = (List<String>) configuration.getList("role[@name]");
        final int numberOfRoles = roleNames.size();
        if (numberOfRoles > 0) {
            for (int roleIndex = 0; roleIndex < numberOfRoles; roleIndex++) {
                StringBuilder buffer = new StringBuilder();
                buffer.append("role(").append(roleIndex).append(")").toString();
                final String prefix = buffer.toString();
                final String name = configuration.getString(prefix + "[@name]");
                @SuppressWarnings("unchecked")
                final List<String> permissions = configuration.getList(prefix + ".permission");
                final int numberOfPermissions = permissions.size();
                if (name != null) {
                    if (numberOfPermissions > 0) {
                        final SimpleRole role = new SimpleRole(name);
                        for (int permissionIndex = 0; permissionIndex < numberOfPermissions; permissionIndex++) {
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
