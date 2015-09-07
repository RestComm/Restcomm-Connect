package org.mobicents.servlet.restcomm.http.applications;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.mobicents.servlet.restcomm.http.IdentityEndpoint;
import org.mobicents.servlet.restcomm.http.KeycloakResourcesEndpoint;

/**
 * A separate Web application for public resources. Automatic binding to endpoints
 * cannot work here. There is already the default web application under /services
 * that gets bound with all other resources automatically.
 *
 * @author "Tsakiridis Orestis"
 *
 */
@ApplicationPath("/identity")
public class KeycloakResources extends Application {

    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<Class<?>>();
        resources.add(IdentityEndpoint.class);
        resources.add(KeycloakResourcesEndpoint.class);
        return resources;
    }
}
