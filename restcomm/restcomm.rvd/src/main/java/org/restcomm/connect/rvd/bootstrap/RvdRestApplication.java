package org.restcomm.connect.rvd.bootstrap;

import javax.ws.rs.core.Application;

//import org.restcomm.connect.rvd.http.GenericExceptionMapper;
import org.restcomm.connect.rvd.http.ResponseWrapperExceptionMapper;
import org.restcomm.connect.rvd.http.resources.NotificationsRestService;
import org.restcomm.connect.rvd.http.AuthorizationExceptionMapper;
import org.restcomm.connect.rvd.http.ProjectDoesNotExistMapper;
import org.restcomm.connect.rvd.http.StorageExceptionMapper;
import org.restcomm.connect.rvd.http.resources.DesignerRestService;
import org.restcomm.connect.rvd.http.resources.LoginRestService;
import org.restcomm.connect.rvd.http.resources.ProjectRestService;
import org.restcomm.connect.rvd.http.resources.RasRestService;
import org.restcomm.connect.rvd.http.resources.RvdController;
import org.restcomm.connect.rvd.http.resources.SettingsRestService;

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
        classes.add(DesignerRestService.class);
        classes.add(NotificationsRestService.class);
        // and exception mappers
        classes.add(ProjectDoesNotExistMapper.class);
        classes.add(StorageExceptionMapper.class);
        classes.add(AuthorizationExceptionMapper.class);
        classes.add(ResponseWrapperExceptionMapper.class);
        //classes.add(GenericExceptionMapper.class);
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