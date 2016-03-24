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
import org.mobicents.servlet.restcomm.dao.IdentityInstancesDao;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.IdentityUtils;
import org.mobicents.servlet.restcomm.identity.shiro.RestcommRoles;

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
    RestcommRoles restcommRoles;
    ConcurrentHashMap<Sid,KeycloakDeployment> deployments = new ConcurrentHashMap<Sid,KeycloakDeployment>();
    IdentityInstancesDao dao;


    public IdentityContext(String realmName, String realmKey, String authServerUrl, RestcommRoles restcommRoles, IdentityInstancesDao identityInstancesDao) {
        this.realmName = realmName;
        this.realmKey = realmKey;
        this.authServerUrl = authServerUrl;
        this.restcommRoles = restcommRoles;
        this.dao = identityInstancesDao;
    }

    /**
     * Creates a new deployment out of an identity instance and puts in a hashmap. If it's allready there it returns it.
     *
     * @param instance
     * @return
     * @throws KeycloakDeploymentAlreadyCreated
     */
    public KeycloakDeployment addDeployment( IdentityInstance instance ) {
        KeycloakDeployment existingDeployment = deployments.get(instance.getSid());
        // if it's already there do nothing (return it)
        if (existingDeployment != null)
            return existingDeployment;
        else {
            KeycloakConfigurationBuilder confBuilder = new KeycloakConfigurationBuilder(realmName, realmKey, authServerUrl, instance.getName(), instance.getRestcommRestClientSecret());
            KeycloakDeployment deployment = IdentityUtils.createDeployment(confBuilder.getUnregisteredRestcommConfig());
            deployments.put(instance.getSid(), deployment);
            return deployment;
        }
    }

    /**
     * Returns a keycloak deployment for an identity instance sid in a lazy way (creates
     * it the deployment if not alread there).
     *
     * @param identityInstanceSid
     * @return existing keycloak deployment object of null
     */
    public KeycloakDeployment getDeployment(Sid identityInstanceSid) {
        KeycloakDeployment deployment = deployments.get(identityInstanceSid);
        if (deployment == null) {
            IdentityInstance addedInstance = dao.getIdentityInstance(identityInstanceSid);
            if (addedInstance != null) {
                return addDeployment(addedInstance);
            } else
                return null;
        } else {
            return deployment;
        }
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
