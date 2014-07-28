package org.mobicents.servlet.restcomm.rvd;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.FsStorageBase;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;

public class RvdContext {

    protected ModelMarshaler marshaler;
    protected RvdSettings settings;
    protected ProjectStorage projectStorage;
    protected ServletContext servletContext;
    protected FsStorageBase storageBase;
    protected HttpServletRequest request;


    public RvdContext(HttpServletRequest request,  ServletContext servletContext) {
        this.servletContext = servletContext;
        this.request = request;
        this.marshaler = new ModelMarshaler();
        this.settings = RvdSettings.getInstance(servletContext);
        this.storageBase = new FsStorageBase(this.settings.getWorkspaceBasePath(), marshaler);
        this.projectStorage = new FsProjectStorage(storageBase, marshaler);

    }

    public Interpreter createInterpreter(String appName, MultivaluedMap<String, String> requestParams) {
        throw new UnsupportedOperationException("You'll need a ProjectAwareRvdContext to use an interpreter");
    }

    public ProjectLogger getProjectLogger() {
        throw new UnsupportedOperationException("You'll need a ProjectAwareRvdContext to use ProjectLogger");
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
