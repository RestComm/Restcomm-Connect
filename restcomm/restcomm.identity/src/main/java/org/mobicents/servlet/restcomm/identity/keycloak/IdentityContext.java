/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.servlet.restcomm.identity.keycloak;

import org.keycloak.adapters.KeycloakDeployment;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.IdentityUtils;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityResourceNames;
import org.mobicents.servlet.restcomm.identity.exceptions.KeycloakDeploymentAlreadyCreated;
import org.mobicents.servlet.restcomm.identity.shiro.RestcommRoles;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Identity Context holds all identity related entities whose lifecycle follows Restcomm lifecycle, such as
 * keycloak deployment and restcomm roles.
 *
 * In a typical use case you can  access to the IdentityContext from the ServletContext.
 *
 * @author "Tsakiridis Orestis"
 */
public class IdentityContext {
    // keycloak specific properties
    String realmName;
    String realmKey;
    String authServerUrl;
    private RestcommRoles restcommRoles;
    ConcurrentHashMap<Sid,KeycloakDeployment> deployments = new ConcurrentHashMap<Sid,KeycloakDeployment>();

    public IdentityContext(String realmName, String realmKey, String authServerUrl, RestcommRoles restcommRoles) {
        this.realmName = realmName;
        this.realmKey = realmKey;
        this.authServerUrl = authServerUrl;
        this.restcommRoles = restcommRoles;
    }

    public void addDeployment( IdentityInstance instance ) throws KeycloakDeploymentAlreadyCreated {
        if ( ! deployments.contains(instance.getSid()) ) {
            KeycloakConfigurationBuilder confBuilder = new KeycloakConfigurationBuilder(realmName, realmKey, authServerUrl, instance.getName(), instance.getRestcommRestClientSecret());
            KeycloakDeployment deployment = IdentityUtils.createDeployment(confBuilder.getUnregisteredRestcommConfig());
            deployments.put(instance.getSid(), deployment);
        } else {
            throw new KeycloakDeploymentAlreadyCreated("Keycloak deployment is already created for identity instance '" + instance.getName() + "'");
        }
    }

    /**
     * Returns a keycloak deployment for the specified identity instance or null if non exists.
     *
     * @param identityInstanceSid
     * @return existing keycloak deployment object of null
     */
    public KeycloakDeployment getDeployment(Sid identityInstanceSid) {
        return deployments.get(identityInstanceSid);
    }



    public String getRealmName() {
        return realmName;
    }

    public String getRealmKey() {
        return realmKey;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public RestcommRoles getRestcommRoles() { return restcommRoles; }

//    public String getClientName(IdentityResourceNames clientType) {
//        switch (clientType) {
//            case RESTCOMM_REST: return instanceId + "-restcomm-rest";
//            case RESTCOMM_UI: return instanceId + "-restcomm-ui";
//            case RESTCOMM_RVD_REST: return instanceId + "-restcomm-rvd-rest";
//            case RESTCOMM_RVD_UI: return instanceId + "-restcomm-rvd-ui";
//        }
//        throw new IllegalStateException("Invalid IdentityResourceName found: " + clientType.toString());
//    }

}
