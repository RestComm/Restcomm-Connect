package org.mobicents.servlet.restcomm.identity;

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;

public class IdentityUtils {
    // private static Logger logger = Logger.getLogger(IdentityUtils.class);

    private IdentityUtils() {
        // TODO Auto-generated constructor stub
    }

    public static KeycloakDeployment createDeployment(AdapterConfig adapterConfig) {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(adapterConfig);
        return deployment;
    }

    /*
    public static void writeAdapterConfigToFile(AdapterConfig adapterConfig, String filepath) {
        try {
            AdapterConfigEntity entity = new AdapterConfigEntity();

            entity.setRealm(adapterConfig.getRealm());
            entity.setRealmPublicKey(adapterConfig.getRealmKey());
            entity.setResource(adapterConfig.getResource());
            entity.setAuthServerUrl(adapterConfig.getAuthServerUrl());
            entity.setBearerOnly(adapterConfig.isBearerOnly());
            entity.setPublicClient(adapterConfig.isPublicClient());
            entity.setSslRequired(adapterConfig.getSslRequired());
            entity.setUseResourceRoleMappings(adapterConfig.isUseResourceRoleMappings());

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(filepath);
            gson.toJson(entity, writer);
            writer.close();

        } catch (IOException e) {
            logger.error("Error saving keycloak adapter configuration for '" + adapterConfig.getResource() + "' to '" + filepath + "'" );
        }
    }
    */
}
