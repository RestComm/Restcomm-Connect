package org.mobicents.servlet.restcomm.rvd.keycloak;


import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.rvd.configuration.RvdConfigurator;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

public class IdentityContext {
    private static Logger logger = Logger.getLogger(IdentityContext.class);

    final String oauthTokenString;
    final AccessToken oauthToken;
    final String loggedUsername;

    public IdentityContext(RvdConfigurator configurator, HttpServletRequest request) {
        configurator.checkDeployment();
        final String tokenString = extractOauthTokenString(request, configurator);
        if ( ! RvdUtils.isEmpty(tokenString) ) {
            oauthToken = verifyToken(tokenString, configurator);
            if (oauthToken != null) {
                loggedUsername = oauthToken.getPreferredUsername();
                oauthTokenString = tokenString;
            } else {
                oauthTokenString = null;
                loggedUsername = null;
            }
        }
        else {
            this.oauthToken = null;
            this.oauthTokenString = null;
            loggedUsername = null;
        }
    }

    private String extractOauthTokenString(HttpServletRequest request, RvdConfigurator configurator) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            String[] parts = authHeader.split(" ");
            if (parts.length >= 2 && parts[0].equals("Bearer")) {
                String tokenString = parts[1];
                return tokenString;
            }
        }
        return null;
    }

    private AccessToken verifyToken(String tokenString, RvdConfigurator configurator ) {
        KeycloakDeployment deployment = configurator.getDeployment();
        if (deployment == null)
            return null;

        AccessToken token;
        try {
            token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealmInfoUrl());
            return token;
        } catch (VerificationException e) {
            logger.error("Cannot verity token.", e);
            return null;
        }
    }

    public AccessToken getOauthToken() {
        return oauthToken;
    }

    public String getOauthTokenString() {
        return oauthTokenString;
    }

    public String getLoggedUsername() {
        return loggedUsername;
    }

}
