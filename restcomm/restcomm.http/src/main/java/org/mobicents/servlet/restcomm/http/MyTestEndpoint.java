package org.mobicents.servlet.restcomm.http;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.shiro.ShiroResources;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

@Path("/testing")
@ThreadSafe
public class MyTestEndpoint extends AbstractEndpoint {

    private Logger logger = Logger.getLogger(MyTestEndpoint.class);
    private String loggedUsername;

    @Context
    HttpServletRequest request;

    public MyTestEndpoint() {
        // TODO Auto-generated constructor stub
    }

    @PostConstruct
    void init() {
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        if (session.getToken() != null) {
            this.loggedUsername = session.getToken().getPreferredUsername();
            logger.info("logged username: " + this.loggedUsername);
        }
    }

    @GET
    public Response runTest() {
        logger.info("IN runTest 667");
        ShiroResources shiroResources = ShiroResources.getInstance();
        RestcommRoles restcommRoles = shiroResources.get(RestcommRoles.class);
        String rolesString = restcommRoles.toString();
        //logger.info("roles: " + rolesString);

        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = session.getToken();
        if (accessToken != null) {
            logger.info("username: " + accessToken.getPreferredUsername() );
            Set<String> roleNames = accessToken.getRealmAccess().getRoles();
            String message = "has the following roles: ";
            for (String rolename: roleNames) {
                message += rolename + ", ";
            }
            logger.info(message);

            String neededPermissionString = "domain:restcomm:read:accounts";
            WildcardPermissionResolver resolver = new WildcardPermissionResolver();
            Permission neededPermission = resolver.resolvePermission(neededPermissionString);
            //Permission neededPermission = new DomainPermission(neededPermissionString); // Developer has: RestComm:*:Calls

            // build the authorization token
            SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo(roleNames);

            // check the neededPermission against all roles of the user
            for (String roleName: roleNames) {
                SimpleRole simpleRole = restcommRoles.getRole(roleName);
                if ( simpleRole == null)
                    logger.info("Cannot map keycloak role '" + roleName + "' to local restcomm configuration. Ignored." );
                else {
                    // simpleRole, neededPermission
                    logger.info("checking role " + roleName);

                    Set<Permission> permissions = simpleRole.getPermissions();
                    // check the permissions one by one

                    for (Permission permission: permissions) {
                        logger.info("Testing " + neededPermissionString + " against " + permission.toString() );
                        if (permission.implies(neededPermission)) {
                            logger.info("permission granted!");
                        }

                    }

                    logger.info("role " + roleName + " does not allow " + neededPermissionString);
                }
            }
        }

        return Response.ok().build();
    }

    /*
    protected void secure2(final Account account, final String permission) throws AuthorizationException {

        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        IDToken token = session.getToken();
        if (token != null) {
            String loggedUsername = to.getPreferredUsername();
            //logger.info("logged username: " + loggedUsername);


        }

        //final Subject subject = SecurityUtils.getSubject();
        final Sid accountSid = account.getSid();
        if (account.getStatus().equals(Account.Status.ACTIVE) && (subject.hasRole("Administrator") || (subject.getPrincipal().equals(accountSid) && subject.isPermitted(permission)))) {
            return;
        } else {
            throw new AuthorizationException();
        }
    }
    */

}
