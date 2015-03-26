package org.mobicents.servlet.restcomm.rvd;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;

public class RvdContext {

    private ModelMarshaler marshaler;
    private RvdConfiguration settings;
    private ServletContext servletContext;
    protected WorkspaceStorage workspaceStorage;

    public RvdContext(HttpServletRequest request,  ServletContext servletContext) {
        this.settings = RvdConfiguration.getInstance(servletContext);
        this.marshaler = new ModelMarshaler();
        this.workspaceStorage = new WorkspaceStorage(settings.getWorkspaceBasePath(), marshaler);
        this.servletContext = servletContext;
    }

    public ProjectLogger getProjectLogger() {
        throw new UnsupportedOperationException("You'll need a ProjectAwareRvdContext to use ProjectLogger");
    }

    public ModelMarshaler getMarshaler() {
        return marshaler;
    }

    public RvdConfiguration getSettings() {
        return settings;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public WorkspaceStorage getWorkspaceStorage() {
        return workspaceStorage;
    }

}
