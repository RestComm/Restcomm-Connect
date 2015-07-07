package org.mobicents.servlet.restcomm.http;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.configuration.Configuration;
import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import org.keycloak.util.JsonSerialization;
import org.mobicents.servlet.restcomm.http.keycloak.KeycloakConfigurator;

@Path("/config")
public class KeycloakResourcesEndpoint extends AbstractEndpoint {

    public KeycloakResourcesEndpoint() {
        super();
    }

    @PostConstruct
    private void init() {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        Configuration runtime_configuration = configuration.subset("runtime-settings");
        super.init(runtime_configuration);
    }

    @GET
    @Path("/restcomm-ui.json")
    @Produces("application/json")
    public Response getRestcommUIConfig() throws IOException {
        String instanceId = getConfigInstanceId();
        // If we are not hooked up to a keycloak application online return NOT_FOUND
        if (instanceId == null || instanceId.trim().isEmpty())
            return Response.status(Status.NOT_FOUND).build();

        KeycloakConfigurator configurator = new KeycloakConfigurator();
        BaseAdapterConfig config = configurator.getRestcommUIConfig(instanceId);
        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/restcomm-rvd-ui.json")
    @Produces("application/json")
    public Response getRestcommRvdUIConfig() throws IOException {
        String instanceId = getConfigInstanceId();
        // If we are not hooked up to an keycloak application online return NOT_FOUND
        if (instanceId == null || instanceId.trim().isEmpty())
            return Response.status(Status.NOT_FOUND).build();

        KeycloakConfigurator configurator = new KeycloakConfigurator();
        BaseAdapterConfig config = configurator.getRestcommRvdUIConfig(instanceId);
        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

    private String getConfigInstanceId() {
        Configuration identityConf = configuration.subset("runtime-settings").subset("identity");
        String instanceId = identityConf.getString("instance-id");

        return instanceId;
    }

}
