package org.mobicents.servlet.restcomm.identity;

import org.apache.log4j.Logger;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;

public class IdentityUtils {
    private static Logger logger = Logger.getLogger(IdentityUtils.class);

    private IdentityUtils() {
        // TODO Auto-generated constructor stub
    }

    public static AccessToken verifyToken(String tokenString,  KeycloakDeployment deployment ) {
        //KeycloakDeployment deployment = configurator.getDeployment();
        AccessToken token;
        try {
            token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealmInfoUrl());
            return token;
        } catch (VerificationException e) {
            logger.error("Cannot verity token.", e);
            return null;
        }
    }

}
