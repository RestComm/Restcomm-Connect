package org.mobicents.servlet.restcomm.http;

import java.util.UUID;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.identity.KeycloakClient;
import org.mobicents.servlet.restcomm.identity.KeycloakClient.KeycloakClientException;

@Path("/instance")
public class IdentityEndpoint extends AbstractEndpoint {

    public static String IDENTITY_PROXY_URL = "https://identity.restcomm.com/instance-manager";

    public IdentityEndpoint() {
        // TODO Auto-generated constructor stub
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
        keycloakClient.makePostRequest(IDENTITY_PROXY_URL + "/api/instances"); // we assume that the identity proxy lives with the authorization server

        logger.info( "User '" + username + "' registed this instance as '" + instanceName + "' at authorization server " + authUrl);
        return Response.ok().build();
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
