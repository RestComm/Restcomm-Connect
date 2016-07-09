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
package org.mobicents.servlet.restcomm.identity;

import org.apache.commons.configuration.Configuration;
import org.keycloak.adapters.KeycloakDeployment;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;
import org.mobicents.servlet.restcomm.dao.IdentityInstancesDao;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.keycloak.KeycloakAdapterConfBuilder;
import org.mobicents.servlet.restcomm.identity.shiro.RestcommRoles;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Identity Context holds all identity related entities whose lifecycle follows Restcomm lifecycle, such as
 * keycloak deployments and restcomm roles.
 *
 * In a typical use case you can  access to the IdentityContext from the ServletContext.
 *
 * @author "Tsakiridis Orestis"
 */
public class IdentityContext {
    RestcommRoles restcommRoles;
    // keycloak specific properties
    String realmName;
    String realmKey;
    String authServerUrl;
    ConcurrentHashMap<Sid,KeycloakDeployment> deployments = new ConcurrentHashMap<Sid,KeycloakDeployment>();
    IdentityInstancesDao dao;

    /**
     *
     * @param restcommConfiguration An apache configuration object representing <restcomm/> element of restcomm.xml
     * @param mainConfig
     */
    public IdentityContext(Configuration restcommConfiguration, MainConfigurationSet mainConfig, IdentityInstancesDao dao) {
        RestcommRoles roles = new RestcommRoles(restcommConfiguration.subset("runtime-settings").subset("security-roles"));
        if (mainConfig != null) {
            init(roles, mainConfig.getIdentityRealm(), mainConfig.getIdentityRealmPublicKey(), mainConfig.getIdentityAuthServerUrl(), dao);
        } else {
            init(roles, null, null, null, null);
        }
    }

    // no-keycloak constructor
    public IdentityContext(RestcommRoles restcommRoles) {
        init(restcommRoles, null, null, null, null);
    }

    public IdentityContext(RestcommRoles restcommRoles, String realmName, String realmKey, String authServerUrl, IdentityInstancesDao dao) {
        init(restcommRoles, realmName, realmKey, authServerUrl, dao);
    }

    private void init(RestcommRoles restcommRoles, String realmName, String realmKey, String authServerUrl, IdentityInstancesDao dao) {
        if (restcommRoles == null)
            throw  new IllegalArgumentException("Cannot create an IdentityContext object with null roles!");
        this.restcommRoles = restcommRoles;
        if ( authServerUrl != null && (realmKey == null || realmName == null || dao == null))
            throw new IllegalArgumentException();
        this.realmName = realmName;
        this.realmKey= realmKey;
        this.authServerUrl = authServerUrl;
        this.dao = dao;
    }

    /**
     * Creates a new deployment out of an identity instance and puts in a hashmap. If it's allready there it returns it.
     *
     * @param instance
     * @return
     */
    public KeycloakDeployment addDeployment( IdentityInstance instance ) {
        KeycloakDeployment existingDeployment = deployments.get(instance.getSid());
        // if it's already there do nothing (return it)
        if (existingDeployment != null)
            return existingDeployment;
        else {
            KeycloakAdapterConfBuilder confBuilder = new KeycloakAdapterConfBuilder(realmName, realmKey, authServerUrl, instance.getName(), instance.getRestcommClientSecret());
            KeycloakDeployment deployment = IdentityUtils.createDeployment(confBuilder.getRestcommConfig());
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
        if (identityInstanceSid == null)
            return null;
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

    public RestcommRoles getRestcommRoles() { return restcommRoles; }

    public String getAuthServerUrl() {
        return authServerUrl;
    }
}
