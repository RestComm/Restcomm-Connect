package org.mobicents.servlet.restcomm.identity.keycloak;

import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator.IdentityNotSet;

public interface KeycloakAdapterConfigBuilder {
    AdapterConfig getRestcommConfig() throws IdentityNotSet;
    BaseAdapterConfig getRestcommUIConfig() throws IdentityNotSet;
    BaseAdapterConfig getRestcommRvdUIConfig() throws IdentityNotSet;
}
