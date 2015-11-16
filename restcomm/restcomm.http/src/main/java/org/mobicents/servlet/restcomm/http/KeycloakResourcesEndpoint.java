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
import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.identity.entities.IdentityModeEntity;
import org.mobicents.servlet.restcomm.identity.keycloak.KeycloakConfigurationBuilder;

import com.google.gson.Gson;

@Path("/config")
public class KeycloakResourcesEndpoint extends AbstractEndpoint {

    private IdentityConfigurationSet identityConfig;
    private KeycloakConfigurationBuilder confBuilder;

    public KeycloakResourcesEndpoint() {
        super();
    }

    @PostConstruct
    private void init() {
        this.identityConfig = RestcommConfiguration.getInstance().getIdentity();
        this.confBuilder = new KeycloakConfigurationBuilder(identityConfig.getRealm(), identityConfig.getRealmKey(), identityConfig.getAuthServerUrl(), identityConfig.getInstanceId(), identityConfig.getRestcommClientSecret());
    }

    @GET
    @Path("/mode")
    public Response getMode() {
        IdentityModeEntity modeEntity = new IdentityModeEntity();
        modeEntity.setMode(identityConfig.getMode());
        modeEntity.setAuthServerUrlBase(identityConfig.getAuthServerBaseUrl());
        Gson gson = new Gson();
        return Response.ok(gson.toJson(modeEntity),MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/restcomm-ui.json")
    @Produces("application/json")
    public Response getRestcommUIConfig() throws IOException {
        BaseAdapterConfig config;
        config = confBuilder.getRestcommUIConfig();
        if (config == null)
            return Response.status(Status.NOT_FOUND).build();
        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/restcomm-rvd-ui.json")
    @Produces("application/json")
    public Response getRestcommRvdUIConfig() throws IOException {
        BaseAdapterConfig config;
        config = confBuilder.getRestcommRvdUIConfig();
        if (config == null)
            return Response.status(Status.NOT_FOUND).build();
        return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON).build();
    }

}
