package org.mobicents.servlet.restcomm.rvd;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.model.ProjectSettings;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class ProjectAwareRvdContext extends RvdContext {

    private String projectName;
    private ProjectLogger projectLogger;
    //private ProjectOptions runtimeProjectOptions;
    private ProjectSettings projectSettings;

    public ProjectAwareRvdContext(String projectName, HttpServletRequest request, ServletContext servletContext) throws StorageException {
        super(request, servletContext);
        this.projectName = projectName;
        this.projectLogger = new ProjectLogger(projectName, getSettings(), getMarshaler());
        //this.runtimeProjectOptions = FsProjectStorage.loadProjectOptions(projectName, workspaceStorage);
        this.projectSettings = FsProjectStorage.loadProjectSettings(projectName, workspaceStorage);
    }

    public ProjectLogger getProjectLogger() {
        return projectLogger;
    }

   /* public ProjectOptions getRuntimeProjectOptions() {
        return runtimeProjectOptions;
    }*/

    public ProjectSettings getProjectSettings() {
        return projectSettings;
    }


}
