package org.mobicents.servlet.restcomm.rvd.bootstrap;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.mobicents.servlet.restcomm.rvd.http.ProjectDoesNotExistMapper;
import org.mobicents.servlet.restcomm.rvd.http.StorageExceptionMapper;
import org.mobicents.servlet.restcomm.rvd.http.UnauthorizedExceptionMapper;
import org.mobicents.servlet.restcomm.rvd.http.resources.DesignerRestService;
import org.mobicents.servlet.restcomm.rvd.http.resources.ProjectRestService;
import org.mobicents.servlet.restcomm.rvd.http.resources.RasRestService;
import org.mobicents.servlet.restcomm.rvd.http.resources.RootRestService;

@ApplicationPath("/api")
public class RvdApplication extends Application {
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(DesignerRestService.class);
        s.add(ProjectRestService.class);
        s.add(RasRestService.class);
        s.add(RootRestService.class);
        s.add(ProjectDoesNotExistMapper.class);
        s.add(StorageExceptionMapper.class);
        s.add(UnauthorizedExceptionMapper.class);
        return s;
    }
}
