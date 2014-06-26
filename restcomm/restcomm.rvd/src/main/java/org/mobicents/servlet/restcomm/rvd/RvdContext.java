package org.mobicents.servlet.restcomm.rvd;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.FsStorageBase;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;

public class RvdContext {

    private ModelMarshaler marshaler;
    private RvdSettings settings;
    private ProjectStorage projectStorage;
    private ServletContext servletContext;
    private FsStorageBase storageBase;

    public RvdContext(HttpServletRequest request,  ServletContext servletContext) {
        this.servletContext = servletContext;
        this.marshaler = new ModelMarshaler();
        this.settings = RvdSettings.getInstance(servletContext);
        this.storageBase = new FsStorageBase(this.settings.getWorkspaceBasePath());
        this.projectStorage = new FsProjectStorage(storageBase, marshaler);
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

    public FsStorageBase getStorageBase() {
        return storageBase;
    }


}
