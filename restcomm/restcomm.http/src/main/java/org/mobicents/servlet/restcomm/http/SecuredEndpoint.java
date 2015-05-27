package org.mobicents.servlet.restcomm.http;

import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.shiro.ShiroResources;
import org.mobicents.servlet.restcomm.http.keycloak.KeycloakClient;

/**
 *
 * @author orestis.tsakiridis@telestax.com
 *
 */
public abstract class SecuredEndpoint extends AbstractEndpoint {
    protected KeycloakClient keycloakClient;
    protected static RestcommRoles restcommRoles;

    public SecuredEndpoint() {
        super();
    }

    protected void init(final Configuration configuration) {
        super.init(configuration);
        ShiroResources shiroResources = ShiroResources.getInstance();
        restcommRoles = shiroResources.get(RestcommRoles.class);
        keycloakClient = new KeycloakClient(request);
    }

    // Throws an authorization exception in case the user does not have the permission OR does not own (or is a parent) the account
    protected void secure(final Account account, final String permission) throws AuthorizationException {
        secureKeycloak(account, permission, getKeycloakAccessToken());
    }

    // check if the user with the roles in accessToken can access has the following permissions (on the API)
    protected void secureApi(String neededPermissionString, final AccessToken accessToken) {
        // normalize the permission string
        neededPermissionString = "domain:" + neededPermissionString;

        Set<String> roleNames;
        try {
            roleNames = accessToken.getRealmAccess().getRoles();
        } catch (NullPointerException e) {
            throw new UnauthorizedException("No access token present or no roles in it");
        }

        // no need to check permissions for users with the RestcommAdmin role
        if ( roleNames.contains("RestcommAdmin") ) {
            return;
        }

        WildcardPermissionResolver resolver = new WildcardPermissionResolver();
        Permission neededPermission = resolver.resolvePermission(neededPermissionString);
        // build the authorization token
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo(roleNames);

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
                        return;
                    }
                }
                logger.debug("Role " + roleName + " does not allow " + neededPermissionString);
            }
        }
        throw new AuthorizationException();
    }

    private void secureKeycloak(final Account account, final String neededPermissionString, final AccessToken accessToken) {
        secureApi(neededPermissionString, accessToken);
        // check if the logged user has access to the account that is operated upon
        secureByAccount(accessToken, account);
    }

    // uses keycloak token
    protected String getLoggedUsername() {
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        if (session.getToken() != null) {
            return session.getToken().getPreferredUsername();
        }
        return null;
    }

    /* make sure the token bearer can access data that belong to this account. In its simplest form this means that the username in the token
     * is the same as the account username. When the organization concepts are implemented and hierarchical accounts are created a smarter
     * approach that will allow parant users access the resources of their children should be employed.
     */
    protected void secureByAccount(final AccessToken accessToken, final Account account) {
        // load logged user's account
        Account loggedAccount = accountsDao.getAccount(accessToken.getPreferredUsername());
        if ( loggedAccount != null && loggedAccount.getSid() != null ) {
            // check if loggedAccount is the same as the checked account or is parent of it
            if ( loggedAccount.getSid().equals(account.getSid()) || loggedAccount.getSid().equals(account.getAccountSid()) ) {
                // no op
            } else {
                throw new UnauthorizedException("User cannot access resources for the specified account.");
            }
        }
    }

    // does the accessToken contain the role?
    /*
    protected void secureByRole(final AccessToken accessToken, String role) {
        Set<String> roleNames;
        try {
            roleNames = accessToken.getRealmAccess().getRoles();
        } catch (NullPointerException e) {
            throw new UnauthorizedException("No access token present or no roles in it");
        }

        if (roleNames.contains(role))
            return;
        else
            throw new UnauthorizedException("Role "+role+" is missing from token");
    }*/

    protected AccessToken getKeycloakAccessToken() {
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = session.getToken();
        return accessToken;
    }

}
