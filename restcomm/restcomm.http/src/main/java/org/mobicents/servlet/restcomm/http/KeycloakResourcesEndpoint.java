package org.mobicents.servlet.restcomm.http;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import org.keycloak.util.JsonSerialization;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator.IdentityNotSet;
import org.mobicents.servlet.restcomm.identity.entities.IdentityModeEntity;

import com.google.gson.Gson;

@Path("/config")
public class KeycloakResourcesEndpoint extends AbstractEndpoint {

    private IdentityConfigurator identityConfigurator;

    public KeycloakResourcesEndpoint() {
        super();
    }

    @PostConstruct
    private void init() {
        identityConfigurator = (IdentityConfigurator) context.getAttribute(IdentityConfigurator.class.getName());
    }

    @GET
    @Path("/mode")
    public Response getMode() {
        IdentityModeEntity modeEntity = new IdentityModeEntity();
        modeEntity.setMode(identityConfigurator.getMode());
        modeEntity.setAuthServerUrlBase(identityConfigurator.getAuthServerUrlBase());
        Gson gson = new Gson();
        return Response.ok(gson.toJson(modeEntity),MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/restcomm-ui.json")
    @Produces("application/json")
    public Response getRestcommUIConfig() throws IOException {
        BaseAdapterConfig config;
        try {
            config = identityConfigurator.getRestcommUIConfig();
        } catch (IdentityNotSet e) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/restcomm-rvd-ui.json")
    @Produces("application/json")
    public Response getRestcommRvdUIConfig() throws IOException {
        BaseAdapterConfig config;
        try {
            config = identityConfigurator.getRestcommRvdUIConfig();
        } catch (IdentityNotSet e) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

}
