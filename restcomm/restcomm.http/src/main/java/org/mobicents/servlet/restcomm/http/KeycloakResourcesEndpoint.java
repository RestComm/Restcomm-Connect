package org.mobicents.servlet.restcomm.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import org.keycloak.util.JsonSerialization;

@Path("/keycloak/config")
public class KeycloakResourcesEndpoint extends AbstractEndpoint {

    public KeycloakResourcesEndpoint() {
        // TODO Auto-generated constructor stub
    }

    @GET
    @Path("/restcomm.json")
    @Produces("application/json")
    public Response getRestcommConfig() throws IOException {
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm("restcomm");
        config.setRealmKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB");
        config.setAuthServerUrl("https://identity.restcomm.com/auth");
        config.setSslRequired("all");
        config.setResource("restcomm-rest");
        config.setEnableBasicAuth(true);
        config.setCors(true);

        Map<String,String> credentials = new HashMap<String,String>();
        credentials.put("secret", "password");
        config.setCredentials(credentials);

        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/restcomm-ui.json")
    @Produces("application/json")
    public Response getRestcommUIConfig() throws IOException {
        BaseAdapterConfig config = new BaseAdapterConfig();
        config.setRealm("restcomm");
        config.setRealmKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB");
        config.setAuthServerUrl("https://identity.restcomm.com/auth");
        config.setSslRequired("all");
        config.setResource("restcomm-ui");
        config.setPublicClient(true);

        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

}
