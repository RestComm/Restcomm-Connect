package org.restcomm.connect.identity.permissions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.restcomm.connect.commons.dao.Sid;
//import org.restcomm.connect.commons.exceptions.InsufficientPermission;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.AccountPermission;
import org.restcomm.connect.identity.AuthOutcome;
//import org.restcomm.connect.identity.IdentityContext;
//import org.restcomm.connect.identity.UserIdentityContext;
import org.restcomm.connect.identity.shiro.RestcommRoles;

public class PermissionsUtil {
    private static PermissionsUtil instance;

    protected Logger logger = Logger.getLogger(PermissionsUtil.class);

    private AccountsDao accountsDao;
    private DaoManager storage;
    private RestcommRoles restcommRoles;

    public static PermissionsUtil getInstance() {
        if (instance == null) {
            throw new RuntimeException();
        }
        return instance;
    }
    public static PermissionsUtil getInstance(ServletContext context, DaoManager storage, Configuration restcommConfiguration) {
        if (instance == null) {
            instance = new PermissionsUtil(context, storage, restcommConfiguration);
        }
        return instance;
    }

    private PermissionsUtil(ServletContext context, DaoManager storage, Configuration restcommConfiguration) {
        //this.context = context;
        this.storage = storage;
        //this.identityContext = (IdentityContext) context.getAttribute(IdentityContext.class.getName());
        this.setRestcommRoles(new RestcommRoles(restcommConfiguration.subset("runtime-settings").subset("security-roles")));
        accountsDao = this.storage.getAccountsDao();
    }

    /**
     * Grants access by permission. If the effective account has a role that resolves
     * to the specified permission (accoording to mappings of restcomm.xml) access is granted.
     * Administrator is granted access regardless of permissions.
     *
     * @param permission - e.g. 'RestComm:Create:Accounts'
     */
    public AuthOutcome checkPermission(String neededPermissionString, Sid accountSid) {
        WildcardPermissionResolver resolver = new WildcardPermissionResolver();
        Permission neededPermission = resolver.resolvePermission(neededPermissionString);
        List<org.restcomm.connect.dao.entities.Permission> accountPermissions = accountsDao.getAccountPermissions(accountSid);
        // check the neededPermission against all roles of the user
        RestcommRoles restcommRoles = this.getRestcommRoles();

        //FIXME: should an account have more than one role??
        Set<String> roleNames = new HashSet<String>();
        roleNames.add(accountsDao.getAccount(accountSid).getRole());
        Set<Permission> allRolePermissions = new HashSet<Permission>();
        for (String roleName: roleNames) {
            SimpleRole simpleRole = restcommRoles.getRole(roleName);
            if ( simpleRole == null) {
                logger.error(roleName+" doesnt exist");
            }else{

                try {
                    Set<Permission> rolePermissions = simpleRole.getPermissions();

                    allRolePermissions.addAll(rolePermissions);
                } catch (Exception e) {
                    // TODO: handle exception
                    logger.debug(e);
                }
            }
        }
        for(org.restcomm.connect.dao.entities.Permission p: accountPermissions){
            //String name = p.getName();
            //FIXME:cast problem??
            AccountPermission ap = (AccountPermission)p;

            //check if account permission is false and exists in rolePermissions
            //if it does, remove it from rolePermissions
            if(allRolePermissions.contains(ap) && ap.getValue()==false){
                allRolePermissions.remove(ap);
            }
            //check if account permission is true and does not exist in rolePermissions
            //add it to rolePermissions
            if(!allRolePermissions.contains(ap) && ap.getValue()){
                allRolePermissions.add(ap);
            }
        }
        //check if neededPermission is implied in all permissions
        //WildcardPermission checkPerm = new WildcardPermission(neededPermissionString);
        //allRolePermissions.containsKey(checkPerm);
        //allRolePermissions.implies(checkPerm);
        //FIXME:can we not loop through this again?
        //TODO: add memoization table
        for(Permission p : allRolePermissions){
            if(p.implies(neededPermission)){
                if (logger.isDebugEnabled()) {
                    logger.debug("Granted access by permission " + p.toString());
                }
                return AuthOutcome.OK;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("No permissions " + neededPermissionString);
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

    public RestcommRoles getRestcommRoles() {
        return restcommRoles;
    }

    public void setRestcommRoles(RestcommRoles restcommRoles) {
        this.restcommRoles = restcommRoles;
    }
}
