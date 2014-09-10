package org.mobicents.servlet.restcomm.rvd;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.FsStorageBase;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;

public class RvdContext {

    private ModelMarshaler marshaler;
    private RvdConfiguration settings;
    private ProjectStorage projectStorage;
    private ServletContext servletContext;
    private FsStorageBase storageBase;
    protected WorkspaceStorage workspaceStorage;

    public RvdContext(HttpServletRequest request,  ServletContext servletContext) {
        this.settings = RvdConfiguration.getInstance(servletContext);
        this.marshaler = new ModelMarshaler();
        this.workspaceStorage = new WorkspaceStorage(settings.getWorkspaceBasePath(), marshaler);
        this.servletContext = servletContext;
        this.storageBase = new FsStorageBase(this.settings.getWorkspaceBasePath(), marshaler);
        this.projectStorage = new FsProjectStorage(storageBase, marshaler);

    }

//    public Interpreter createInterpreter(String appName, MultivaluedMap<String, String> requestParams) {
//       throw new UnsupportedOperationException("You'll need a ProjectAwareRvdContext to use an interpreter");
//    }

    public ProjectLogger getProjectLogger() {
        throw new UnsupportedOperationException("You'll need a ProjectAwareRvdContext to use ProjectLogger");
    }

    public ModelMarshaler getMarshaler() {
        return marshaler;
    }

    public RvdConfiguration getSettings() {
        return settings;
    }

    public ProjectStorage getProjectStorage() {
        return projectStorage;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public FsStorageBase getStorageBase() {
        return storageBase;
    }

    public WorkspaceStorage getWorkspaceStorage() {
        return workspaceStorage;
    }

}
