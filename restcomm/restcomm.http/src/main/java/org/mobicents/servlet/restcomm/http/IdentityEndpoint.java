package org.mobicents.servlet.restcomm.http;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.identity.IdentityConfigurator;
import org.mobicents.servlet.restcomm.identity.KeycloakClient;
import org.mobicents.servlet.restcomm.identity.KeycloakClient.KeycloakClientException;
import org.mobicents.servlet.restcomm.identity.IdentityConfigurator.IdentityMode;

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
    private IdentityConfigurator identityConfigurator;
    private XMLConfiguration restcommConfiguration;



    public IdentityEndpoint() {
        // TODO Auto-generated constructor stub
    }

    @PostConstruct
    private void init() {
        identityConfigurator = (IdentityConfigurator) context.getAttribute(IdentityConfigurator.class.getName());
        restcommConfiguration = (XMLConfiguration) context.getAttribute(Configuration.class.getName());
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
        keycloakClient.makePostRequest(identityConfigurator.getIdentityProxyUrl() + "/api/instances"); // we assume that the identity proxy lives together with the authorization server

        // We're now registered. Update configuration. For now we will just store to RAM until a way is found to update restcomm.xml on the fly.
        identityConfigurator.setAuthServerUrlBase(authUrl);
        identityConfigurator.setMode(IdentityMode.cloud);
        identityConfigurator.setRestcommClientSecret(instanceSecret);
        identityConfigurator.setCloudInstanceId(instanceName);
        identityConfigurator.updateRestcommXml(restcommConfiguration);

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
