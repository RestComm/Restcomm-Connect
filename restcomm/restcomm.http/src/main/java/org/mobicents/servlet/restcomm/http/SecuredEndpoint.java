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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.OrgIdentityDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.OrgIdentity;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.exceptions.AuthorizationException;
import org.mobicents.servlet.restcomm.http.exceptions.InsufficientPermission;
import org.mobicents.servlet.restcomm.http.exceptions.NotAuthenticated;
import org.mobicents.servlet.restcomm.http.exceptions.OrganizationAccessForbidden;
import org.mobicents.servlet.restcomm.http.exceptions.AccountNotLinked;
import org.mobicents.servlet.restcomm.http.exceptions.NoMappedAccount;
import org.mobicents.servlet.restcomm.identity.IdentityRegistrationTool;
import org.mobicents.servlet.restcomm.identity.AuthOutcome;
import org.mobicents.servlet.restcomm.identity.IdentityContext;
import org.mobicents.servlet.restcomm.identity.UserIdentityContext;
import org.mobicents.servlet.restcomm.identity.mocks.Organization;
import org.mobicents.servlet.restcomm.identity.mocks.OrganizationDao;
import org.mobicents.servlet.restcomm.identity.shiro.RestcommRoles;
import org.mobicents.servlet.restcomm.identity.UserIdentityContext.AuthKind;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;


/**
 * Security layer endpoint. It will scan the request for security related assets and populate the
 * UserIdentityContext accordingly. Extend the class and use checkAuthenticatedAccount*() methods to apply security rules to
 * your endpoint.
 *
 * How to use it:
 * - use checkAuthenticatedAccount() method to check that a user (any user) is authenticated.
 * - use checkAuthenticatedAccount(permission) method to check that an authenticated user has the required permission according to his roles
 * - use checkAuthenticatedAccount(account,permission) method to check that besides permission a user also has ownership over an account
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 */
public abstract class SecuredEndpoint extends AbstractEndpoint {

    // types of secured resources used to apply different policies to applications, numbers etc.
    public enum SecuredType {
        SECURED_APP,
        SECURED_ACCOUNT, SECURED_STANDARD
    }

    protected Logger logger = Logger.getLogger(SecuredEndpoint.class);

    protected UserIdentityContext userIdentityContext;
    protected AccountsDao accountsDao;
    protected OrganizationDao organizationDao;
    protected IdentityContext identityContext;
    private OrgIdentity orgIdentity;
    private Organization organization;
    @Context
    protected ServletContext context;
    @Context
    HttpServletRequest request;

    public SecuredEndpoint() {
        super();
    }

    protected void init(final Configuration configuration) {
        super.init(configuration);
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        this.accountsDao = storage.getAccountsDao();
        this.identityContext = (IdentityContext) context.getAttribute(IdentityContext.class.getName());
        this.organization = findCurrentOrganization(request, organizationDao);
        this.orgIdentity = findCurrentOrgIdentity(request, storage.getOrgIdentityDao());
        KeycloakDeployment deployment = null;
        if (this.orgIdentity != null)
            deployment = identityContext.getDeployment(orgIdentity.getSid());
        this.userIdentityContext = new UserIdentityContext(deployment, request, accountsDao);
        checkOrganizationIdentity(); // check bearer-based access to organization in all Secured endpoints
    }

    /**
     * Grants general purpose access if any valid token exists in the request
     */
    protected void checkAuthenticatedAccount() {
        if (userIdentityContext.getEffectiveAccount() != null)
            return;
        if (userIdentityContext.getAuthKind() == AuthKind.KeycloakAuth && userIdentityContext.getKeycloakMappedAccount() != null) {
            if (userIdentityContext.getKeycloakMappedAccount().getLinked() == false)
                throw new AccountNotLinked();
        }
        if (userIdentityContext.getAuthKind() == AuthKind.KeycloakAuth && userIdentityContext.getKeycloakMappedAccount() == null) {
            throw new NoMappedAccount();
        }

        throw new NotAuthenticated();
    }

    /**
     * Grants access by permission. If the effective account has a role that resolves
     * to the specified permission (accoording to mappings of restcomm.xml) access is granted.
     * Administrator is granted access regardless of permissions.
     *
     * @param permission - e.g. 'RestComm:Create:Accounts'
     */
    protected void checkPermission(final String permission) {
        //checkAuthenticatedAccount(); // ok there is a valid authenticated account
        if ( checkPermission(permission, userIdentityContext.getEffectiveAccountRoles()) != AuthOutcome.OK )
            throw new InsufficientPermission();
    }

    /**
     * Checks if the incoming request contains the required roles for accessing the organization specified
     * in the URL
     */
    protected void checkOrganizationIdentity() {
        if (userIdentityContext.getAuthKind() == AuthKind.KeycloakAuth) {
            String neededRole = IdentityRegistrationTool.buildKeycloakClientRole(orgIdentity.getName()); // the following role should be present in the bearer token in order to allow access to the instance
            String keycloakClientName = IdentityRegistrationTool.buildKeycloakClientName(orgIdentity.getName());
            AccessToken oauthToken = userIdentityContext.getOauthToken();
            try {
                if (oauthToken == null) {
                    // looks like the there was a bearer token that couldn't be verified.
                    throw new NotAuthenticated();
                }
                if (!oauthToken.getResourceAccess().get(keycloakClientName).getRoles().contains(neededRole)) {
                    throw new OrganizationAccessForbidden();
                }
            } catch (NullPointerException e) {
                throw new OrganizationAccessForbidden();
            }
        }
    }

    // boolean overloaded form of checkAuthenticatedAccount(permission)
    protected boolean isSecuredByPermission(final String permission) {
        try {
            checkPermission(permission);
            return true;
        } catch (AuthorizationException e) {
            return false;
        }
    }

    /**
     * Personalized type of grant. Besides checking 'permission' the effective account should have some sort of
     * ownership over the operatedAccount. The exact type of ownership is defined in secureAccount()
     *
     * @param operatedAccount
     * @param permission
     * @throws AuthorizationException
     */
    protected void secure(final Account operatedAccount, final String permission) throws AuthorizationException {
        secure(operatedAccount, permission, SecuredType.SECURED_STANDARD);
    }

    protected void secure(final Account operatedAccount, final String permission, SecuredType type) throws AuthorizationException {
        checkAuthenticatedAccount();
        checkPermission(permission); // check an authbenticated account allowed to do "permission" is available
        if (operatedAccount == null)
            throw new AuthorizationException();
        if (type == SecuredType.SECURED_STANDARD) {
            if (secureLevelControl(userIdentityContext.getEffectiveAccount(), operatedAccount, null) != AuthOutcome.OK )
                throw new InsufficientPermission();
        } else
        if (type == SecuredType.SECURED_APP) {
            if (secureLevelControlApplications(userIdentityContext.getEffectiveAccount(),operatedAccount,null) != AuthOutcome.OK)
                throw new InsufficientPermission();
        } else
        if (type == SecuredType.SECURED_ACCOUNT) {
            if (secureLevelControlAccounts(userIdentityContext.getEffectiveAccount(), operatedAccount) != AuthOutcome.OK)
                throw new InsufficientPermission();
        }
    }

    protected void secure(final Account operatedAccount, final Sid resourceAccountSid, SecuredType type) throws AuthorizationException {
        checkAuthenticatedAccount();
        String resourceAccountSidString = resourceAccountSid == null ? null : resourceAccountSid.toString();
        if (type == SecuredType.SECURED_APP) {
            if (secureLevelControlApplications(userIdentityContext.getEffectiveAccount(), operatedAccount, resourceAccountSidString) != AuthOutcome.OK)
                throw new InsufficientPermission();
        } else
        if (type == SecuredType.SECURED_STANDARD){
            if (secureLevelControl(userIdentityContext.getEffectiveAccount(), operatedAccount, resourceAccountSidString) != AuthOutcome.OK)
                throw new InsufficientPermission();
        } else
        if (type == SecuredType.SECURED_ACCOUNT)
            throw new IllegalStateException("Account security is not supported when using sub-resources");
        else {
            throw new NotImplementedException();
        }
    }

//    protected void secure(final Account operatedAccount, final Sid resourceAccountSid, final String permission) throws AuthorizationException {
//        secure(operatedAccount, resourceAccountSid, permission, SecuredType.SECURED_STANDARD);
//    }
//
//    protected void secure(final Account operatedAccount, final Sid resourceAccountSid, final String permission, final SecuredType type ) {
//        secure(operatedAccount, resourceAccountSid, type);
//        checkPermission(permission); // check an authbenticated account allowed to do "permission" is available
//    }

    /**
     * Checks is the effective account has the specified role. Only role values contained in the Restcomm Account
     * are take into account.
     *
     * @param role
     * @return true if the role exists in the Account. Otherwise it returns false.
     */
    protected boolean hasAccountRole(final String role) {
        if (userIdentityContext.getEffectiveAccount() != null) {
            return userIdentityContext.getEffectiveAccountRoles().contains(role);
        }
        return false;
    }

    /**
     * Low level permission checking. roleNames are checked for neededPermissionString permission using permission
     * mappings contained in restcomm.xml. The permission mappings are stored in RestcommRoles.
     *
     * Note: Administrator is granted access with eyes closed

     * @param neededPermissionString
     * @param roleNames
     * @return
     */
    private AuthOutcome checkPermission(String neededPermissionString, Set<String> roleNames) {
        // if this is an administrator ask no more questions
        if ( roleNames.contains(getAdministratorRole()))
            return AuthOutcome.OK;

        // normalize the permission string
        //neededPermissionString = "domain:" + neededPermissionString;

        WildcardPermissionResolver resolver = new WildcardPermissionResolver();
        Permission neededPermission = resolver.resolvePermission(neededPermissionString);

        // check the neededPermission against all roles of the user
        RestcommRoles restcommRoles = identityContext.getRestcommRoles();
        for (String roleName: roleNames) {
            SimpleRole simpleRole = restcommRoles.getRole(roleName);
            if ( simpleRole == null) {
                return AuthOutcome.FAILED;
            }
            else {
                Set<Permission> permissions = simpleRole.getPermissions();
                // check the permissions one by one
                for (Permission permission: permissions) {
                    if (permission.implies(neededPermission)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Granted access by permission " + permission.toString());
                        }
                        return AuthOutcome.OK;
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Role " + roleName + " does not allow " + neededPermissionString);
                }
            }
        }
        return AuthOutcome.FAILED;
    }

    /**
     * Applies the following access control rule:

     * If no sub-resources are involved (resourceAccountSid is null):
     *  - If operatingAccount is the same or a parent of operatedAccount access is granted
     * If there are sub-resources involved:
     *  - If operatingAccount is the same or a parent of operatedAccount AND resoulrce belongs to operatedAccount access is granted

     * @param operatingAccount  the account that is authenticated
     * @param operatedAccount the account specified in the URL
     * @param resourceAccountSid the account SID property of the operated resource e.g. the accountSid of a DID.
     *
     */
    private AuthOutcome secureLevelControl( Account operatingAccount, Account operatedAccount, String resourceAccountSid) {
        String operatingAccountSid = null;
        if (operatingAccount != null)
            operatingAccountSid = operatingAccount.getSid().toString();
        String operatedAccountSid = null;
        if (operatedAccount != null)
            operatedAccountSid = operatedAccount.getSid().toString();

        if (!operatingAccountSid.equals(operatedAccountSid)) {
            Account account = accountsDao.getAccount(new Sid(operatedAccountSid));
            if (!operatingAccountSid.equals(String.valueOf(account.getAccountSid()))) {
                return AuthOutcome.FAILED;
            } else if (resourceAccountSid != null && !operatedAccountSid.equals(resourceAccountSid)) {
                return AuthOutcome.FAILED;
            }
        } else if (resourceAccountSid != null && !operatingAccountSid.equals(resourceAccountSid)) {
            return AuthOutcome.FAILED;
        }
        return AuthOutcome.OK;
    }

    /** Applies the following access control rules
     *
     * If an application Account Sid is given:
     *  - If operatingAccount is the same as the operated account and application resource belongs to operated account too
     *    acces is granted.
     * If no application Accouns Sid is given:
     *  - If operatingAccount is the same as the operated account access is granted.
     *
     * NOTE: Parent relationships on accounts do not grant access here.
     *
     * @param operatingAccount
     * @param operatedAccount
     * @param applicationAccountSid
     * @return
     */
    private AuthOutcome secureLevelControlApplications(Account operatingAccount, Account operatedAccount, String applicationAccountSid) {
        String operatingAccountSid = null;
        if (operatingAccount != null)
            operatingAccountSid = operatingAccount.getSid().toString();
        String operatedAccountSid = null;
        if (operatedAccount != null)
            operatedAccountSid = operatedAccount.getSid().toString();

        if (!operatingAccountSid.equals(String.valueOf(operatedAccountSid))) {
            return AuthOutcome.FAILED;
        } else if (applicationAccountSid != null && !operatingAccountSid.equals(applicationAccountSid)) {
            return AuthOutcome.FAILED;
        }
        return AuthOutcome.OK;
    }

    /** Applies the following access control rules:
     *
     * If the operating account is an administrator:
     *  - If it is the same or parent of the operated account access is granted.
     * If the operating accoutn is NOT an administrator:
     *  - If it is the same as the operated account access is granted.
     *
     * @param operatingAccount
     * @param operatedAccount
     * @return
     */
    private AuthOutcome secureLevelControlAccounts(Account operatingAccount, Account operatedAccount) {
        if (operatingAccount == null || operatedAccount == null)
            return AuthOutcome.FAILED;
        if (getAdministratorRole().equals(operatingAccount.getRole())) {
            // administrator can also operate on child accounts
            if (!String.valueOf(operatingAccount.getSid()).equals(String.valueOf(operatedAccount.getSid()))) {
                if (!String.valueOf(operatingAccount.getSid()).equals(String.valueOf(operatedAccount.getAccountSid()))) {
                    return AuthOutcome.FAILED;
                }
            }
        } else { // non-administrators

            if ( operatingAccount.getSid().equals(operatedAccount.getSid()) )
                return AuthOutcome.OK;
            else
                return AuthOutcome.FAILED;
        }
        return AuthOutcome.OK;
    }

    /**
     * Returns the string literal for the administrator role. This role is granted implicitly access from checkAuthenticatedAccount() method.
     * No need to explicitly apply it at each protected resource
     * .
     * @return the administrator role as string
     */
    protected String getAdministratorRole() {
        return "Administrator";
    }


    /**
     * Returns the OrgIdentity for the active organization (determined from the request). Null, if there
     * is no organization found or if the active organization is not secured.
     *.
     * @param request
     * @param orgIdentityDao
     * @return the OrgIdentity object mapped or null
     */
    private OrgIdentity findCurrentOrgIdentity(HttpServletRequest request, OrgIdentityDao orgIdentityDao) {
        if ( getOrganization() != null ) {
            if (identityContext.getAuthServerUrl() != null) {
                OrgIdentity orgIdentity = orgIdentityDao.getOrgIdentityByOrganizationSid(organization.getSid());
                return orgIdentity;
            }
        }
        return null;
    }

    protected OrgIdentity getOrgIdentity() {
        return this.orgIdentity;
    }

    private Organization findCurrentOrganization(HttpServletRequest request, OrganizationDao organizationDao) {
        String domainPrefix; // for telestax.restcomm.com it would be 'telestax'
        try {
            String domain = new URL(request.getRequestURL().toString()).getHost();
            domainPrefix = domain.split(".")[0];
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot get domain name while trying to determine current organization");
        }
        return organizationDao.getOrganizationByDomain(domainPrefix);
    }

    protected Organization getOrganization() {
        return this.organization;
    }
}
