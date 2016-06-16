package org.mobicents.servlet.restcomm.rvd.identity;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.http.client.utils.URIBuilder;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.restcomm.IdentityInstanceResponse;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves and caches identity instance information, request origin (dns) and keycloak deployments.
 *
 * The idea is to have a lazy-created repository of KeycloakDeployments mapped to by origins (e.g. wonderland.com).
 *
 * Here is how it works:
 *
 * - An incoming request arrives under http://wonderland.com:8080/restcomm-rvd. Its *origin* is extracted,
 * - The provider is not aware of it so it contacts restcomm at http://wonderland.com:8080/.../Identity/Instances/current
 *   to fetch an identity if exists (this restcomm method is public and returns only minimal info).
 * - Restcomm finds and returns the respective identity (see IdentityInstancesEndpointJson.java:getCurrentIdentityInstance()).
 * - Now that the provider has an identity instance ID is checks for cached keycloak deployments.
 * - If no such deployment is found a new one is created and stored in a hash table.
 * - The (cached or created) keycloak deployment is returned.
 * - RVD can now use this deployment to authorize the request.
 *
 * Note it's implemented as a lazily created singleton because restcommUrl is not available in RvdInitializationServlet :-(
 *
 * @author Orestis Tsakiridis
 */
public class IdentityProvider {
    String restcommUrl;
    String authServerUrl;
    String realm;
    String realmPublicKey;
    Map<String,KeycloakDeployment> deploymentsByInstanceId = new HashMap<String,KeycloakDeployment>();
    Map<String,String> instanceIdsByOrigin = new HashMap<String,String>();

    IdentityProvider(String restcommUrl, String authServerUrl, String realm, String realmPublicKey) {
        this.restcommUrl = restcommUrl;
        this.authServerUrl = authServerUrl;
        this.realm = realm;
        this.realmPublicKey = realmPublicKey;
    }

    /**
     * Returns a KeycloakDeployment object for an origin e.g. http://wonderland.com:8080 or null.
     * @param origin
     * @return
     */
    public KeycloakDeployment getKeycloak(RequestOrigin origin) {
        String parsedOrigin = origin.getOrigin();
        String instanceId = instanceIdsByOrigin.get(parsedOrigin);
        if (instanceId != null) {
            // ok, there is indeed an instance id. That MUST be a keycloak deployment too. Let's return it.
            return deploymentsByInstanceId.get(instanceId);
        } else {
            URI uri = buildIdentityInstanceQueryUrl(parsedOrigin);
            IdentityInstanceResponse identity = fetchIdentityInstance(uri);
            // TODO error handling ?
            // no identity instance is available for this origin. Let's return null
            if (identity == null)
                return null;
            instanceId = identity.getName();
            // create or get a cached deployment for this identity instance
            KeycloakDeployment deployment = deploymentsByInstanceId.get(instanceId);
            if (deployment == null) {
                deployment = buildDeployment(identity.getName());
                if (deployment != null) {
                    // store it in the cache
                    deploymentsByInstanceId.put(identity.getName(), deployment);
                }
            }
            return deployment;
        }
    }

    /**
     * Parses an origin like http://wonderland.com:8080 and makes sure it's ready to be used to access Restcomm
     * i.e. it looks like: http://wonderland.com:8080. It's important to have a clean syntax here since this value
     * is used as a key to the instanceIdsByOrigin map. That's the reason we have a separate function for that.
     *
     * @param rawOrigin
     */
    /*private String parseOrigin(String rawOrigin) {
        // TODO for now it just returns the rawOrigin as it is
        return rawOrigin;
    }*/

    /**
     * Create the url for the request that will retrieve the identity instance info.
     *
     * @param origin
     * @return
     */
    private URI buildIdentityInstanceQueryUrl(String origin) {
        try {
            URI uri = new URIBuilder(origin + "/restcomm/2012-04-24/Identity/Instances/current").build();
            return uri;
        } catch (URISyntaxException e) {
            // something really wrong has happened
            throw new RuntimeException(e);
        }
    }

    // TODO add error-handling
    private IdentityInstanceResponse fetchIdentityInstance(URI uri) {
        Client jerseyClient = Client.create();
        WebResource webResource = jerseyClient.resource(uri);
        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        String string_response = response.getEntity(String.class);
        Gson gson = new Gson();
        return gson.fromJson(string_response, IdentityInstanceResponse.class);


    }

    private KeycloakDeployment buildDeployment(String instanceName) {
        // build the adapter object
        AdapterConfig config = new AdapterConfig();
        config.setRealm(this.realm);
        config.setRealmKey(this.realmPublicKey);
        config.setAuthServerUrl(authServerUrl);
        //config.setSslRequired("all");
        config.setResource(instanceName + "-rvd-ui");
        config.setPublicClient(true);
        config.setUseResourceRoleMappings(true);

        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(config);
        return deployment;
    }

    // singleton stuff
    private static IdentityProvider instance;
    public static IdentityProvider getInstance() {
        if (instance == null) {
            RvdConfiguration config = RvdConfiguration.getInstance();
            instance = new IdentityProvider(config.getRestcommBaseUri().toString(), config.getAuthServerUrl(), config.getRealm(), config.getRealmPublicKey());
        }
        return instance;
    }
}
