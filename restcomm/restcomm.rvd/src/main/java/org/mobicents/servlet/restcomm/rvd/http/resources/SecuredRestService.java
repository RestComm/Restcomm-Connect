package org.mobicents.servlet.restcomm.rvd.http.resources;

import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.AuthorizationException;
import org.mobicents.servlet.restcomm.rvd.http.RestService;
import org.mobicents.servlet.restcomm.rvd.identity.AccountProvider;
import org.mobicents.servlet.restcomm.rvd.identity.UserIdentityContext;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author Orestis Tsakiridis
 */
public class SecuredRestService extends RestService {
    private UserIdentityContext userIdentityContext;

    public void init() {
        RvdConfiguration config = RvdConfiguration.getInstance();
        AccountProvider accountProvider = AccountProvider.getInstance();
        // if it is secured by keycloak try to create a deployment too
        if (config.keycloakEnabled()) {
            throw new NotImplementedException();
        }
        String authorizationHeader = request.getHeader("Authorization");
        userIdentityContext = new UserIdentityContext(authorizationHeader, accountProvider);
    }

    public UserIdentityContext getUserIdentityContext() {
        return userIdentityContext;
    }

    /**
     * Makes sure the request is done by an authenticated user.
     */
    protected void secure() {
        if (userIdentityContext.getAccountInfo() == null) {
            throw new AuthorizationException();
        }
    }

    /**
     * Convenience function to quickly access logged username without going through getUserIdentityContext().get...
     * @return
     */
    protected String getLoggedUsername() {
        return userIdentityContext.getAccountUsername();
    }
}
