package org.mobicents.servlet.restcomm.rvd.bootstrap;

import javax.ws.rs.core.Application;

import org.mobicents.servlet.restcomm.rvd.http.ProjectDoesNotExistMapper;
import org.mobicents.servlet.restcomm.rvd.http.StorageExceptionMapper;
import org.mobicents.servlet.restcomm.rvd.http.resources.LoginRestService;
import org.mobicents.servlet.restcomm.rvd.http.resources.ProjectRestService;
import org.mobicents.servlet.restcomm.rvd.http.resources.RasRestService;
import org.mobicents.servlet.restcomm.rvd.http.resources.RvdController;
import org.mobicents.servlet.restcomm.rvd.http.resources.SettingsRestService;

import java.util.HashSet;
import java.util.Set;

/**
 * This is a workaround for dealing with Jersey's automatic scanning of packages for resource classes in JBoss. It manually
 * declares the resource classes and will be removed as soon a better solution is found.
 *
 */
public class RvdRestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        // register resources
        classes.add(ProjectRestService.class);
        classes.add(RvdController.class);
        classes.add(RasRestService.class);
        classes.add(LoginRestService.class);
        classes.add(SettingsRestService.class);
        classes.add(ProjectDoesNotExistMapper.class);
        classes.add(StorageExceptionMapper.class);
        return classes;
    }

    /*
    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.add(new TicketRepositoryProvider());
        return super.getSingletons();
    }
    */



}
