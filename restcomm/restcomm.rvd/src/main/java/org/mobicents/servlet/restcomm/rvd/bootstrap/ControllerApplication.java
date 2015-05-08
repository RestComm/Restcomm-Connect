package org.mobicents.servlet.restcomm.rvd.bootstrap;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.mobicents.servlet.restcomm.rvd.http.ProjectDoesNotExistMapper;
import org.mobicents.servlet.restcomm.rvd.http.StorageExceptionMapper;
import org.mobicents.servlet.restcomm.rvd.http.UnauthorizedExceptionMapper;
import org.mobicents.servlet.restcomm.rvd.http.resources.RvdController;

@ApplicationPath("/services")
public class ControllerApplication extends Application {
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(RvdController.class);
        s.add(ProjectDoesNotExistMapper.class);
        s.add(StorageExceptionMapper.class);
        s.add(UnauthorizedExceptionMapper.class);
        return s;
    }
}
