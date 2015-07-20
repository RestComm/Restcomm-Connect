package org.mobicents.servlet.restcomm.identity;

import java.util.List;

import org.keycloak.representations.idm.RoleRepresentation;

public class KeycloakHelpers {

    public static RoleRepresentation getRoleByName(String roleName, List<RoleRepresentation> availableKeycloakRoles) {
        if ( roleName == null )
            return null;

        // TODO Optimize!
        for ( RoleRepresentation role: availableKeycloakRoles ) {
            if ( roleName.equals(role.getName()))
                return role;
        }
        return null;
    }

}
