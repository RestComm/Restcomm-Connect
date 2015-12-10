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
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.identity.AccountKey;
import org.mobicents.servlet.restcomm.identity.AuthOutcome;
import org.mobicents.servlet.restcomm.identity.UserIdentityContext;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityResourceNames;
import org.mobicents.servlet.restcomm.identity.keycloak.IdentityContext;
import org.mobicents.servlet.restcomm.identity.shiro.RestcommRoles;


/**
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 */
public abstract class SecuredEndpoint extends AbstractEndpoint {
    protected static RestcommRoles restcommRoles;
    protected MutableIdentityConfigurationSet identityConfiguration;
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
        restcommRoles = identityContext.getRestcommRoles();
        this.userIdentityContext = new UserIdentityContext(identityContext, request, accountsDao);

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
        if ( userIdentityContext.getOauthToken() != null )
            if ( secureKeycloak(operatedAccount, permission, userIdentityContext.getOauthToken() ) == AuthOutcome.OK )
                return;

        if ( userIdentityContext.getAccountKey() != null )
            if ( secureApikey(operatedAccount, permission, userIdentityContext.getAccountKey()) == AuthOutcome.OK )
                return;

        throw new AuthorizationException();
    }

    /**
     * Allows general purpose access if one of the following happens:
     * - User carries a token that contains a role (any role) for this application/client
     * - Account key was verified.
     */
    protected void secure() {
        Set<String> roleNames = null;
        if ( userIdentityContext.getOauthToken() != null ) {
            try {
                roleNames = userIdentityContext.getOauthToken().getResourceAccess(identityContext.getClientName(IdentityResourceNames.RESTCOMM_REST)).getRoles();
            } catch (NullPointerException e) {
                throw new AuthorizationException();
            }
            if (roleNames != null)
                return;
        }
        else
        if ( userIdentityContext.getAccountKey() != null ) {
            if ( userIdentityContext.getAccountKey().isVerified() )
                return;
        }
        throw new AuthorizationException();
    }

    protected void secure (final String permission) {
        Set<String> roleNames = null;
        if ( userIdentityContext.getOauthToken() != null )
            roleNames = userIdentityContext.getOauthToken().getResourceAccess(identityContext.getClientName(IdentityResourceNames.RESTCOMM_REST)).getRoles();
        else
        if ( userIdentityContext.getAccountKey() != null ) {
            if ( userIdentityContext.getAccountKey().isVerified() )
                roleNames = userIdentityContext.getAccountKey().getRoles();
        }

        if ( roleNames != null )
            if ( secureApi(permission, roleNames) == AuthOutcome.OK )
                return;

        throw new AuthorizationException();
    }

    /**
     * Checks if the a user with roles 'roleNames' is allowed to perform actions in 'neededPermissionString'
     * @param neededPermissionString
     * @param roleNames
     * @return
     */
    private AuthOutcome secureApi(String neededPermissionString, Set<String> roleNames) {
        // if this is an administrator ask no more questions
        if ( roleNames.contains(IdentityConfigurationSet.ADMINISTRATOR_ROLE))
            return AuthOutcome.OK;

        // normalize the permission string
        neededPermissionString = "domain:" + neededPermissionString;

        WildcardPermissionResolver resolver = new WildcardPermissionResolver();
        Permission neededPermission = resolver.resolvePermission(neededPermissionString);

        // check the neededPermission against all roles of the user
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
     * Implements authorization using keycloak Oauth token
     * @param account
     * @param neededPermissionString
     * @param accessToken
     * @return
     */
    private AuthOutcome secureKeycloak(final Account account, final String neededPermissionString, final AccessToken accessToken) {
        // both api-level and account-level checks should be satisfied
        AccessToken.Access access = accessToken.getResourceAccess(identityContext.getClientName(IdentityResourceNames.RESTCOMM_REST));
        if (access == null)
            return AuthOutcome.FAILED;
        Set<String> roleNames = access.getRoles();
        if ( secureApi(neededPermissionString, roleNames) == AuthOutcome.FAILED )
            return AuthOutcome.FAILED;
        // check if the logged user has access to the account that is operated upon
        if ( secureAccountByUsername(accessToken.getPreferredUsername(), account) == AuthOutcome.FAILED )
            return AuthOutcome.FAILED;

        return AuthOutcome.OK;
    }

    /**
     * Implements authorization using the API Key credentials i.e. Basic HTTP auth username:password and compares against local authToken and roles.
     * @param account
     * @param permission
     * @param accountKey
     * @return
     */
    private AuthOutcome secureApikey(final Account account, final String permission, final AccountKey accountKey) {
        if ( ! accountKey.isVerified() )
            return AuthOutcome.FAILED;

        Set<String> roleNames = accountKey.getRoles();
        if ( !roleNames.contains(identityConfiguration.getAdministratorRole()) && secureApi(permission, roleNames) == AuthOutcome.FAILED )
            return AuthOutcome.FAILED;
        // check if the logged user has access to the account that is operated upon
        if ( secureAccount(accountKey.getAccount(), account) == AuthOutcome.FAILED )
            return AuthOutcome.FAILED;

        return AuthOutcome.OK;
    }

    // uses keycloak token
    protected String getLoggedUsername() {
        AccessToken token = userIdentityContext.getOauthToken();
        if (token != null) {
            return token.getPreferredUsername();
        }
        return null;
    }

    /**
     * Makes sure that User 'username' can access resources of the operatedAccount. An Account should
     * be mapped to the specific User through its emailAddress property.
     *
     */
    protected AuthOutcome secureAccountByUsername(final String username, final Account operatedAccount) {
        // load logged user's account
        Account loggedAccount = accountsDao.getAccount(username);
        return secureAccount(loggedAccount, operatedAccount);
    }

    /**
     * Makes sure a user authenticated against actorAccount can access operatedAccount. In practice allows access if actorAccount == operatedAccount
     * OR if operatedAccount is a sub-account of actorAccount
     * @param actorAccount
     * @param operatedAccount
     * @return
     */
    protected AuthOutcome secureAccount(Account actorAccount, final Account operatedAccount) {
        if ( actorAccount != null && actorAccount.getSid() != null ) {
            if ( actorAccount.getSid().equals(operatedAccount.getSid()) || actorAccount.getSid().equals(operatedAccount.getAccountSid()) ) {
                return AuthOutcome.OK;
            }
        }
        return AuthOutcome.FAILED;
    }

    protected AccessToken getKeycloakAccessToken() {
        AccessToken token = userIdentityContext.getOauthToken();
        return token;
    }

}
