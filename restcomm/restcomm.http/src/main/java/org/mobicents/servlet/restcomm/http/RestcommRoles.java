package org.mobicents.servlet.restcomm.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.permission.DomainPermission;
import org.mobicents.servlet.restcomm.entities.shiro.ShiroResources;

public class RestcommRoles {
    private Logger logger = Logger.getLogger(RestcommRoles.class);
    private volatile Map<String, SimpleRole> roles;

    public RestcommRoles() {
        getRole("Developer");
    }

    public SimpleRole getRole(final String role) {
        if (roles != null) {
            return roles.get(role);
        } else {
            synchronized (this) {
                if (roles == null) {
                    roles = new HashMap<String, SimpleRole>();
                    final ShiroResources services = ShiroResources.getInstance();
                    final Configuration configuration = services.get(Configuration.class);
                    loadSecurityRoles(configuration.subset("security-roles"));
                }
            }
            return roles.get(role);
        }
    }

    private void loadSecurityRoles(final Configuration configuration) {
        @SuppressWarnings("unchecked")
        final List<String> roleNames = (List<String>) configuration.getList("role[@name]");
        final int numberOfRoles = roleNames.size();
        if (numberOfRoles > 0) {
            for (int roleIndex = 0; roleIndex < numberOfRoles; roleIndex++) {
                StringBuilder buffer = new StringBuilder();
                buffer.append("role(").append(roleIndex).append(")").toString();
                final String prefix = buffer.toString();
                final String name = configuration.getString(prefix + "[@name]");
                @SuppressWarnings("unchecked")
                final List<String> permissions = configuration.getList(prefix + ".permission");

                if (name != null) {
                    if (permissions.size() > 0 ) {
                        final SimpleRole role = new SimpleRole(name);
                        for (String permissionString: permissions) {
                            logger.info("loading permission " + permissionString + " into " + name + " role");
                            final Permission permission = new DomainPermission(permissionString);
                            role.add(permission);
                        }
                        roles.put(name, role);
                    }
                }

                /*final int numberOfPermissions = permissions.size();
                if (name != null) {
                    if (numberOfPermissions > 0) {
                        final SimpleRole role = new SimpleRole(name);
                        for (int permissionIndex = 0; permissionIndex < numberOfPermissions; permissionIndex++) {
                            buffer = new StringBuilder();
                            buffer.append(prefix).append(".permission(").append(permissionIndex).append(")");
                            logger.info("loading permission: " + buffer.toString() );
                            final Permission permission = new DomainPermission(buffer.toString());
                            role.add(permission);
                        }
                        roles.put(name, role);
                    }
                }*/
            }
        }
    }

    @Override
    public String toString() {
        if ( roles == null || roles.size() == 0 )
            return "no roles defined";
        else {
            StringBuffer buffer = new StringBuffer();
            for ( String role: roles.keySet() ) {
                buffer.append(role);
                SimpleRole simpleRole = roles.get(role);
                Set<Permission> permissions = simpleRole.getPermissions();
                buffer.append("[");
                for (Permission permission: permissions) {
                    buffer.append(permission.toString());
                    buffer.append(",");
                }
                buffer.append("]");
            }
            return buffer.toString();
        }
    }

}



