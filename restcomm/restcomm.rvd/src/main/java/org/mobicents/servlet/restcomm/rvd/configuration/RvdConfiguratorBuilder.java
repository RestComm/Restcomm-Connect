package org.mobicents.servlet.restcomm.rvd.configuration;

import javax.servlet.ServletContext;

public class RvdConfiguratorBuilder {

    private static RvdConfigurator cachedInstance;

    public static RvdConfigurator createOnce(ServletContext servletContext) {
        synchronized (RvdConfiguratorBuilder.class) {
            if (cachedInstance == null)
                cachedInstance = new RvdConfigurator(servletContext);
            return cachedInstance;
        }
    }

    public static RvdConfigurator get() {
        if (cachedInstance == null) {
            throw new IllegalStateException("RvdConfigurator has not been initialized");
        }
        return cachedInstance;
    }

}
