package org.mobicents.servlet.restcomm.rvd;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;

public class RvdContext {

    private ModelMarshaler marshaler;
    private RvdSettings settings;
    private ProjectStorage projectStorage;
    private ServletContext servletContext;

    public RvdContext(HttpServletRequest request,  ServletContext servletContext) {
        this.servletContext = servletContext;
        this.marshaler = new ModelMarshaler();
        this.settings = RvdSettings.getInstance(servletContext);
        this.projectStorage = new FsProjectStorage(settings, marshaler);
    }

    public ModelMarshaler getMarshaler() {
        return marshaler;
    }

    public RvdSettings getSettings() {
        return settings;
    }

    public ProjectStorage getProjectStorage() {
        return projectStorage;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }


}
