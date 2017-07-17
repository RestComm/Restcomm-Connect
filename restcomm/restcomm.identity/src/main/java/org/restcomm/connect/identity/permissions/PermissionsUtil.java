package org.restcomm.connect.identity.permissions;

import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.restcomm.connect.commons.exceptions.InsufficientPermission;
import org.restcomm.connect.identity.AuthOutcome;
import org.restcomm.connect.identity.IdentityContext;
import org.restcomm.connect.identity.UserIdentityContext;
import org.restcomm.connect.identity.shiro.RestcommRoles;

public class PermissionsUtil {
    private static PermissionsUtil instance;

    protected Logger logger = Logger.getLogger(PermissionsUtil.class);

    private IdentityContext identityContext;
    private ServletContext context;
    private UserIdentityContext userIdentityContext;

    public static PermissionsUtil getInstance(ServletContext context) {
        if (instance == null) {
            instance = new PermissionsUtil(context);
        }
        return instance;
    }

    private PermissionsUtil(ServletContext context) {
        this.context = context;
        this.identityContext = (IdentityContext) context.getAttribute(IdentityContext.class.getName());
    }

    /**
     * Grants access by permission. If the effective account has a role that resolves
     * to the specified permission (accoording to mappings of restcomm.xml) access is granted.
     * Administrator is granted access regardless of permissions.
     *
     * @param permission - e.g. 'RestComm:Create:Accounts'
     */
    public void checkPermission(final String permission) {
        //checkAuthenticatedAccount(); // ok there is a valid authenticated account
        if ( checkPermission(permission, userIdentityContext.getEffectiveAccountRoles()) != AuthOutcome.OK )
            throw new InsufficientPermission();
    }

    public AuthOutcome checkPermission(String neededPermissionString, Set<String> roleNames) {
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
     * Returns the string literal for the administrator role. This role is granted implicitly access from checkAuthenticatedAccount() method.
     * No need to explicitly apply it at each protected resource
     * .
     * @return the administrator role as string
     */
    protected String getAdministratorRole() {
        return "Administrator";
    }

    public void setUserIdentityContext(UserIdentityContext userIdentityContext){
        this.userIdentityContext = userIdentityContext;
    }

    public UserIdentityContext getUserIdentityContext(){
        return this.userIdentityContext;
    }
}
