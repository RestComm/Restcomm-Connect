package org.mobicents.servlet.restcomm.http;

import java.io.IOException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import org.keycloak.util.JsonSerialization;
import org.mobicents.servlet.restcomm.http.keycloak.KeycloakConfigurator;

@Path("/config")
public class KeycloakResourcesEndpoint extends AbstractEndpoint {

    public KeycloakResourcesEndpoint() {
        // TODO Auto-generated constructor stub
    }

    @GET
    @Path("/restcomm-ui.json")
    @Produces("application/json")
    public Response getRestcommUIConfig() throws IOException {
        KeycloakConfigurator configurator = new KeycloakConfigurator();
        BaseAdapterConfig config = configurator.getRestcommUIConfig();
        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/restcomm-rvd-ui.json")
    @Produces("application/json")
    public Response getRestcommRvdUIConfig() throws IOException {
        KeycloakConfigurator configurator = new KeycloakConfigurator();
        BaseAdapterConfig config = configurator.getRestcommRvdUIConfig();
        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

}
