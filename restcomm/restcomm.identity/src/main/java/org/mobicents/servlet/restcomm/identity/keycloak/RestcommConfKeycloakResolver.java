package org.mobicents.servlet.restcomm.identity.keycloak;

import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.keycloak.adapters.HttpFacade.Request;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurationSet.IdentityMode;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator.IdentityNotSet;

public class RestcommConfKeycloakResolver implements KeycloakConfigResolver {

    //private final Map<String, KeycloakDeployment> cache = new ConcurrentHashMap<String, KeycloakDeployment>();
    private KeycloakDeployment cache;
    private KeycloakDeployment emptyDeployment = new KeycloakDeployment();

    public RestcommConfKeycloakResolver() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public KeycloakDeployment resolve(Request request) {

        if ( authenticateUsingApiKey(request)) {
            return emptyDeployment;
        } else {
            IdentityConfigurator configurator = IdentityConfigurator.getInstance();
            if ( configurator.getMode() == IdentityMode.init ) {
                // no caching here if we're in init mode return an empty deployment
                return emptyDeployment;
            } else {
                try {
                    cache = KeycloakDeploymentBuilder.build(configurator.getRestcommConfig());
                } catch (IdentityNotSet e) {
                    throw new IllegalStateException("No cloud identity set in restcomm.xml");
                }
            }
            return cache;
        }
    }

    /**
     * Returns true if there are is a Restcomm API Key in the request.
     * @param request
     * @return
     */
    boolean authenticateUsingApiKey(Request request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Basic")) {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring("Basic".length()).trim();
            String credentials = new String(Base64.decodeBase64(base64Credentials), Charset.forName("UTF-8"));
            // credentials = username:password
            final String[] values = credentials.split(":",2);
            if (values.length >= 2)
                if ( Sid.valid(values[0]) )
                        return true;
        }
        return false; // it looks like no proper SID exists in the request
    }

}
