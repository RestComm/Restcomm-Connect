package org.mobicents.servlet.restcomm.http;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurationSet.IdentityMode;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator;
import org.mobicents.servlet.restcomm.identity.keycloak.KeycloakClient;
import org.mobicents.servlet.restcomm.identity.keycloak.KeycloakClient.KeycloakClientException;

import com.google.gson.Gson;

@Path("/instance")
public class IdentityEndpoint extends AbstractEndpoint {

    private IdentityConfigurator identityConfigurator;

    public IdentityEndpoint() {
        // TODO Auto-generated constructor stub
    }

    @PostConstruct
    private void init() {
        identityConfigurator = (IdentityConfigurator) context.getAttribute(IdentityConfigurator.class.getName());
    }


    @POST
    @Path("/register")
    public Response registerInstance(@FormParam("restcommBaseUrl") String baseUrl, @FormParam("username") String username, @FormParam("password") String password, @FormParam("instanceSecret") String instanceSecret ) throws KeycloakClientException {
        String instanceName = generateInstanceName(username);
        if (StringUtils.isEmpty(instanceSecret))
            instanceSecret = generateInstanceSecret();

        // fail if no auth server url is set
        String authUrlBase = identityConfigurator.getAuthServerUrlBase();
        if (StringUtils.isEmpty(authUrlBase)) {
            logger.error("Missing identity.auth-server-url-base configuration setting.");
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Invalid configuration").build();
        }

        KeycloakClient keycloakClient = new KeycloakClient(username, password, "restcomm-identity-rest", "restcomm", authUrlBase);
        keycloakClient.getToken();
        keycloakClient.addParam("name", instanceName); // what we put here??
        keycloakClient.addParam("prefix", baseUrl);
        keycloakClient.addParam("secret", instanceSecret);
        keycloakClient.makePostRequest(identityConfigurator.getIdentityProxyUrl() + "/api/instances"); // we assume that the identity proxy lives together with the authorization server

        // We're now registered. Update configuration.
        //identityConfigurator.setAuthServerUrlBase(authUrl);
        identityConfigurator.setMode(IdentityMode.cloud); // TODO maybe turn this to 'standalone'. Is there a difference after all between 'cloud' and 'standalone'
        identityConfigurator.setRestcommClientSecret(instanceSecret);
        identityConfigurator.setInstanceId(instanceName);
        identityConfigurator.save();

        logger.info( "User '" + username + "' registed this instance as '" + instanceName + "' to authorization server " + authUrlBase);

        IdentityInstanceEntity instanceEntity = new IdentityInstanceEntity();
        instanceEntity.setInstanceName(instanceName);
        Gson gson = new Gson();

        return Response.ok().entity(gson.toJson(instanceEntity)).build();
    }

    // generate a random secret for the instance/restcomm-rest client if none specified in the request
    protected String generateInstanceSecret() {
        return UUID.randomUUID().toString();
    }

    // convention: username-UUID
    protected String generateInstanceName(String username) {
        return username + "-" + UUID.randomUUID().toString().split("-")[0];
    }

    public class IdentityInstanceEntity {
        private String instanceName;

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }
    }

}
