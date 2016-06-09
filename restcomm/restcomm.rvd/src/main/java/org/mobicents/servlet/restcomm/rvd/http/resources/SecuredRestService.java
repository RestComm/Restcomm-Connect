package org.mobicents.servlet.restcomm.rvd.http.resources;

import org.keycloak.adapters.KeycloakDeployment;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.AuthorizationException;
import org.mobicents.servlet.restcomm.rvd.http.RestService;
import org.mobicents.servlet.restcomm.rvd.identity.AccountProvider;
import org.mobicents.servlet.restcomm.rvd.identity.UserIdentityContext;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommAccountInfoResponse;
import org.mobicents.servlet.restcomm.rvd.identity.IdentityProvider;
import org.mobicents.servlet.restcomm.rvd.identity.RequestOrigin;

/**
 * @author Orestis Tsakiridis
 */
public class SecuredRestService extends RestService {
    private UserIdentityContext userIdentityContext;

    public void init() {
        RvdConfiguration config = RvdConfiguration.getInstance();
        AccountProvider accountProvider = AccountProvider.getInstance();
        KeycloakDeployment deployment = null;
        // if it is secured by keycloak try to create a deployment too
        if (config.keycloakEnabled()) {
            IdentityProvider identityProvider = IdentityProvider.getInstance();
            deployment = identityProvider.getKeycloak(new RequestOrigin(request));
        }
        String authorizationHeader = request.getHeader("Authorization");
        userIdentityContext = new UserIdentityContext(authorizationHeader, deployment, accountProvider);
    }

    public UserIdentityContext getUserIdentityContext() {
        return userIdentityContext;
    }

    /**
     * Makes sure the request is done by an authenticated user.
     */
    protected void secure() {
        RestcommAccountInfoResponse account = userIdentityContext.getAccountInfo();
        if (account == null || !"active".equals(account.getStatus()) ) {
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
