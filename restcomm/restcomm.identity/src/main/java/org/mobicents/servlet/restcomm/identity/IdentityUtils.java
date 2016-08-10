package org.mobicents.servlet.restcomm.identity;

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;

public class IdentityUtils {
    // private static Logger logger = Logger.getLogger(IdentityUtils.class);

    public static String RESTCOMM_CLIENT_SUFFIX = "restcomm_connect";

    private IdentityUtils() {
        // TODO Auto-generated constructor stub
    }

    public static KeycloakDeployment createDeployment(AdapterConfig adapterConfig) {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(adapterConfig);
        return deployment;
    }

    /**
     * Builds the name of the role needed to access an Organization Identity.
     *
     * Example
     *
     *  telestax-access
     *
     * @param orgIdentityName
     * @return the name of the role
     */
    public static String buildKeycloakClientRole(String orgIdentityName) {
        return orgIdentityName + "-access";
    }

    /**
     * Builds the name of the Keycloak Client that corresponds to the specified
     * Organization Identity name passed.
     *
     * Example
     *  telestax-restcomm
     *
     * @param orgIdentityName
     * @return the nanme of the Keycloak Client
     */
    public static String buildKeycloakClientName(String orgIdentityName) {
        return orgIdentityName + "-" + RESTCOMM_CLIENT_SUFFIX;
    }
}
