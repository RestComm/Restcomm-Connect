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
package org.mobicents.servlet.restcomm.http;

import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.NotImplementedException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSetImpl;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IdentityInstancesDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.AuthOutcome;
import org.mobicents.servlet.restcomm.identity.UserIdentityContext;
import org.mobicents.servlet.restcomm.identity.keycloak.IdentityContext;
import org.mobicents.servlet.restcomm.identity.shiro.RestcommRoles;


/**
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 */
public abstract class SecuredEndpoint extends AbstractEndpoint {
    protected UserIdentityContext userIdentityContext;
    protected AccountsDao accountsDao;
    protected IdentityContext identityContext;

    public SecuredEndpoint() {
        super();
    }

    protected void init(final Configuration configuration) {
        super.init(configuration);
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        this.accountsDao = storage.getAccountsDao();
        this.identityContext = (IdentityContext) context.getAttribute(IdentityContext.class.getName());
        this.userIdentityContext = new UserIdentityContext(request, accountsDao);
    }


    // Authorize request by using either keycloak token or API key method. If any of them succeeds, request is allowed
    /**
     * High level authorization. It grants access to 'account' resources required by permission.
     * It takes into account any Oauth token of API Key existing in the request.
     * @param operatedAccount
     * @param permission
     * @throws AuthorizationException
     */
    protected void secure(final Account operatedAccount, final String permission) throws AuthorizationException {
        secure(permission); // check an authbenticated account allowed to do "permission" is available
        if ( secureAccount(userIdentityContext.getEffectiveAccount(), operatedAccount) != AuthOutcome.OK )
            throw new AuthorizationException();
    }

    /**
     * Checks if the effective account (aka subject) has 'permission' on the operatedAccount. Actually, this checks (a) if
     * effective account sid == operated account sid, and (b) effective account has 'permission' according to its roles. For (a)
     * see secureAccount(...).
     *
     * @param operatedAccount
     * @param permission
     * @return
     */
    protected boolean isSecured(final Account operatedAccount, final String permission) {
        try {
            secure(operatedAccount, permission);
            return true;
        } catch (AuthorizationException e) {
            return false;
        }
    }

    /**
     * Grants general purpose access if any valid token exists in the request
     */
    protected void secure() {
        if (userIdentityContext.getEffectiveAccount() == null)
            throw new AuthorizationException();
    }

    // boolean overloaded form of secure()
    protected boolean isSecured() {
        try {
            secure();
            return true;
        } catch (AuthorizationException e) {
            return false;
        }
    }

    /**
     * Checks there is a valid authenticated account in the request credentials (BASIC http or Oauth) and its roles
     * allow *permission*.
     *
     * @param permission
     */
    protected void secure (final String permission) {
        secure(); // ok there is a valid authenticated account
        if ( secureApi(permission, userIdentityContext.getEffectiveAccountRoles()) != AuthOutcome.OK )
            throw new AuthorizationException();

    }

    protected boolean isSecured(final String permission) {
        try {
            secure(permission);
            return true;
        } catch (AuthorizationException e) {
            return false;
        }
    }

    /**
     * Checks is the effective account (aka 'subject' in shiro terminology) has the specified role. Throws an exception if not.
     * At some point we should add support for multiple roles.
     *
     * @param role
     * @return
     */
    protected boolean hasAccountRole(final String role) {
        if (userIdentityContext.getEffectiveAccount() != null) {
            return userIdentityContext.getEffectiveAccountRoles().contains(role);
        }
        return false;
    }

    /**
     * Checks if the operating account with roles 'roleNames' is allowed to perform actions in 'neededPermissionString'
     * @param neededPermissionString
     * @param roleNames
     * @return
     */
    private AuthOutcome secureApi(String neededPermissionString, Set<String> roleNames) {
        // if this is an administrator ask no more questions
        if ( roleNames.contains(getAdministratorRole()))
            return AuthOutcome.OK;

        // normalize the permission string
        neededPermissionString = "domain:" + neededPermissionString;

        WildcardPermissionResolver resolver = new WildcardPermissionResolver();
        Permission neededPermission = resolver.resolvePermission(neededPermissionString);

        // check the neededPermission against all roles of the user
        RestcommRoles restcommRoles = identityContext.getRestcommRoles();
        for (String roleName: roleNames) {
            SimpleRole simpleRole = restcommRoles.getRole(roleName);
            if ( simpleRole == null) {
                // logger.warn("Cannot map keycloak role '" + roleName + "' to local restcomm configuration. Ignored." );
            }
            else {
                Set<Permission> permissions = simpleRole.getPermissions();
                // check the permissions one by one
                for (Permission permission: permissions) {
                    if (permission.implies(neededPermission)) {
                        logger.debug("Granted access by permission " + permission.toString());
                        return AuthOutcome.OK;
                    }
                }
                logger.debug("Role " + roleName + " does not allow " + neededPermissionString);
            }
        }
        return AuthOutcome.FAILED;
    }

    /**
     * Makes sure a user authenticated against actorAccount can access operatedAccount. In practice allows access if actorAccount == operatedAccount
     * OR (UNDER REVIEW) if operatedAccount is a sub-account of actorAccount
     *
     * UPDATE: parent-child relation check is disabled for compatibility reasons.
     *
     * @param actorAccount
     * @param operatedAccount
     * @return
     */
    protected AuthOutcome secureAccount(Account actorAccount, final Account operatedAccount) {
        if ( actorAccount != null && actorAccount.getSid() != null ) {
            if ( actorAccount.getSid().equals(operatedAccount.getSid()) /*|| actorAccount.getSid().equals(operatedAccount.getAccountSid()) */ ) {
                return AuthOutcome.OK;
            }
        }
        return AuthOutcome.FAILED;
    }

    /**
     * Returns the string literal for the administrator role. This role is granted implicitly access from secure() method.
     * No need to explicitly apply it at each protected resource
     * .
     * @return the administrator role as string
     */
    private String getAdministratorRole() {
        return "Administrator";
    }

}
