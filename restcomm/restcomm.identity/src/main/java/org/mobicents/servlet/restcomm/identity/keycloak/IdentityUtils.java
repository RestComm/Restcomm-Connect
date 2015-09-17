package org.mobicents.servlet.restcomm.identity.keycloak;

import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator;

public class IdentityUtils {
    private static Logger logger = Logger.getLogger(IdentityUtils.class); // no concurrency issues here - see http://stackoverflow.com/questions/12059503/log4j-logger-and-concurrency


    public IdentityUtils() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Extracts and returns a token from a request. Returns null if varification fails or other error occurs (maybe we need to refine this behaviour).
     * @param configurator
     * @param request
     * @return
     */
    public static AccessToken extractToken(IdentityConfigurator configurator, HttpServletRequest request) {
        KeycloakDeployment deployment = configurator.getDeployment();
        String authHeader = request.getHeader("Authorization");
        String[] parts = authHeader.split(" ");
        if (parts.length >= 2 && parts[0].equals("Bearer")) {
            String tokenString = parts[1];
            AccessToken token;
            try {
                token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealmInfoUrl());
            } catch (VerificationException e) {
                logger.error("Cannot verity token.", e);
                return null;
            }
            return token;
        }
        return null;
    }

}
