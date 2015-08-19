package org.mobicents.servlet.restcomm.http;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.identity.KeycloakClient;
import org.mobicents.servlet.restcomm.identity.KeycloakConfigurator;
import org.mobicents.servlet.restcomm.identity.KeycloakClient.KeycloakClientException;
import org.mobicents.servlet.restcomm.identity.KeycloakConfigurator.IdentityMode;

import com.google.gson.Gson;

@Path("/instance")
public class IdentityEndpoint extends AbstractEndpoint {

    public class IdentityInstanceEntity {
        private String instanceName;

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }
    }

    //public static String IDENTITY_PROXY_URL = "https://identity.restcomm.com/instance-manager";
    private KeycloakConfigurator keycloakConfigurator;



    public IdentityEndpoint() {
        // TODO Auto-generated constructor stub
    }

    @PostConstruct
    private void init() {
        keycloakConfigurator = (KeycloakConfigurator) context.getAttribute(KeycloakConfigurator.class.getName());
    }


    @POST
    @Path("/register")
    public Response registerInstance(@FormParam("restcommBaseUrl") String baseUrl, @FormParam("authUrl") String authUrl, @FormParam("username") String username, @FormParam("password") String password, @FormParam("instanceSecret") String instanceSecret ) throws KeycloakClientException {
        String instanceName = generateInstanceName(username);
        if (StringUtils.isEmpty(instanceSecret))
            instanceSecret = generateInstanceSecret();

        KeycloakClient keycloakClient = new KeycloakClient(username, password, "restcomm-identity-rest", "restcomm", authUrl);
        keycloakClient.getToken();
        keycloakClient.addParam("name", instanceName); // what we put here??
        keycloakClient.addParam("prefix", baseUrl);
        keycloakClient.addParam("secret", instanceSecret);
        keycloakClient.makePostRequest(keycloakConfigurator.getIdentityProxyUrl() + "/api/instances"); // we assume that the identity proxy lives together with the authorization server

        // We're now registered. Update configuration. For now we will just store to RAM until a way is found to update restcomm.xml on the fly.
        keycloakConfigurator.setAuthServerUrlBase(authUrl);
        keycloakConfigurator.setMode(IdentityMode.cloud);
        keycloakConfigurator.setRestcommClientSecret(instanceSecret);
        keycloakConfigurator.setCloudInstanceId(instanceName);
        keycloakConfigurator.updateRestcommXml(); // not effective until i find a way update restcomm.xml or store to database

        logger.info( "User '" + username + "' registed this instance as '" + instanceName + "' to authorization server " + authUrl);

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

}
