package org.mobicents.servlet.restcomm.rvd.http;

import java.util.Set;

import org.apache.log4j.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.rvd.exceptions.UnauthorizedException;
import org.mobicents.servlet.restcomm.rvd.keycloak.IdentityContext;

public class SecuredRestService extends RestService {
    static final Logger logger = Logger.getLogger(SecuredRestService.class.getName());

    protected IdentityContext identityContext;

    public SecuredRestService() {
    }

    protected void init() {
        super.init();
        this.identityContext = new IdentityContext(configurator, request);
    }

    protected void secure(String role) throws UnauthorizedException {
        configurator.checkDeployment(); // TODO - implement a better mechanism: in all secure() calls (hopefully one per request) we check if the deployment was been updated and reload in that case
        KeycloakDeployment deployment = configurator.getDeployment();
        if ( deployment != null) {
            AccessToken token = identityContext.getOauthToken();
            String clientName = configurator.getDeployment().getResourceName();
            if (token != null) {
                Set<String> roleNames = token.getResourceAccess(clientName).getRoles();
                if ( roleNames.contains(role))
                    return;
            }
        } else {
            logger.warn("No keycloak adapter configuration was found. Access will be restricted.");
        }
        throw new UnauthorizedException();
    }

}
