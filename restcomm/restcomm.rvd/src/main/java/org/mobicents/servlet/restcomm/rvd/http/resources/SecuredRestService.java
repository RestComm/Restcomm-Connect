/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.rvd.http.resources;

import java.util.Set;

import org.apache.log4j.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.rvd.exceptions.UnauthorizedException;
import org.mobicents.servlet.restcomm.rvd.keycloak.IdentityContext;

public class SecuredRestService extends RestService {
    static final Logger logger = Logger.getLogger(SecuredRestService.class.getName());

    protected IdentityContext identityContext;

    public SecuredRestService() {
    }

    void init() {
        super.init();
        this.identityContext = new IdentityContext(configurator, request);
    }

    protected void secure(String role) throws UnauthorizedException {
        // configurator.checkDeployment(); // TODO - implement a better mechanism: in all secure() calls (hopefully one per request) we check if the deployment was been updated and reload in that case
        KeycloakDeployment deployment = configurator.getDeployment();
        if ( deployment != null) {
            AccessToken token = identityContext.getOauthToken();
            String clientName = configurator.getDeployment().getResourceName();
            if (token != null) {
                Set<String> roleNames = token.getResourceAccess(clientName).getRoles();
                if ( roleNames.contains(role))
                    return;
            }
        } else {
            logger.warn("No keycloak adapter configuration was found. Access will be restricted.");
        }
        throw new UnauthorizedException();
    }

}
