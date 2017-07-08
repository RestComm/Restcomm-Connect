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
package org.restcomm.connect.identity;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.identity.shiro.RestcommRoles;


/**
 * Identity Context holds all identity related entities whose lifecycle follows Restcomm lifecycle, such as
 * keycloak deployment (to be added) and restcomm roles.
 *
 * In a typical use case you can  access to the IdentityContext from the ServletContext.
 *
 * @author "Tsakiridis Orestis"
 */
public class IdentityContext {
    RestcommRoles restcommRoles;

    /**
     * @param restcommConfiguration An apache configuration object representing <restcomm/> element of restcomm.xml
     */
    public IdentityContext(Configuration restcommConfiguration) {
        this.restcommRoles = new RestcommRoles(restcommConfiguration.subset("runtime-settings").subset("security-roles"));
    }

    public IdentityContext(RestcommRoles restcommRoles) {
        if (restcommRoles == null)
            throw  new IllegalArgumentException("Cannot create an IdentityContext object with null roles!");
        this.restcommRoles = restcommRoles;
    }

    public RestcommRoles getRestcommRoles() { return restcommRoles; }

}
