package org.mobicents.servlet.restcomm.rvd;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

public class RvdRestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        // register resources
        classes.add(RvdManagerResource.class);
        classes.add(RvdController.class);
        return classes;
    }
}


