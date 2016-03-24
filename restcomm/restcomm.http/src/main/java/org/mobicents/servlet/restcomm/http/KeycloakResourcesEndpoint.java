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
import org.mobicents.servlet.restcomm.identity.keycloak.KeycloakConfigurationBuilder;

@Path("/config")
public class KeycloakResourcesEndpoint extends SecuredEndpoint {

    //private MutableIdentityConfigurationSet mutableIdentityConfig;
    private IdentityConfigurationSet identityConfig;
    private KeycloakConfigurationBuilder confBuilder;

    public KeycloakResourcesEndpoint() {
        super();
    }

    @PostConstruct
    private void init() {
        //this.mutableIdentityConfig = RestcommConfiguration.getInstance().getMutableIdentity();
        this.identityConfig = RestcommConfiguration.getInstance().getIdentity();
        this.confBuilder = new KeycloakConfigurationBuilder(identityConfig.getRealm(), identityConfig.getRealmkey(), identityConfig.getAuthServerUrl(), getActiveIdentityInstance().getName(), getActiveIdentityInstance().getRestcommRestClientSecret());
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
