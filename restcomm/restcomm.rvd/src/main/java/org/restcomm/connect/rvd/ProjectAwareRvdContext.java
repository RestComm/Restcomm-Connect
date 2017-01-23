package org.restcomm.connect.rvd;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.restcomm.connect.rvd.concurrency.ResidentProjectInfo;
import org.restcomm.connect.rvd.exceptions.ProjectDoesNotExist;
import org.restcomm.connect.rvd.logging.ProjectLogger;
import org.restcomm.connect.rvd.model.ProjectSettings;
import org.restcomm.connect.rvd.storage.FsProjectStorage;
import org.restcomm.connect.rvd.storage.exceptions.StorageEntityNotFound;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

public class ProjectAwareRvdContext extends RvdContext {

    private String projectName;
    private ProjectLogger projectLogger;
    private ProjectSettings projectSettings;

    public ProjectAwareRvdContext(String projectName, ResidentProjectInfo residentInfo, HttpServletRequest request, ServletContext servletContext, RvdConfiguration configuration) throws ProjectDoesNotExist {
        super(request, servletContext, configuration);
        if (projectName == null)
            throw new IllegalArgumentException();
        setProjectName(projectName);
        // setup application logging
        this.projectLogger = new ProjectLogger(projectName, getSettings(), getMarshaler(), residentInfo.logRotationSemaphore);
        // initialize project settings
        try {
            this.projectSettings = FsProjectStorage.loadProjectSettings(projectName, workspaceStorage);
        } catch (StorageEntityNotFound e) {
            this.projectSettings = ProjectSettings.createDefault();
        } catch (StorageException e) {
            throw new RuntimeException(e); // serious error
        }

    }

//    public ProjectAwareRvdContext(HttpServletRequest request, ServletContext servletContext, RvdConfiguration configuration) {
//        super(request, servletContext, configuration);
//    }

    public ProjectLogger getProjectLogger() {
        return projectLogger;
    }

    public ProjectSettings getProjectSettings() {
        return projectSettings;
    }

    void setProjectName(String projectName) throws ProjectDoesNotExist {
        this.projectName = projectName;
        // make sure the project exists
        if (!FsProjectStorage.projectExists(projectName, workspaceStorage)) {
            throw new ProjectDoesNotExist("Project '" + projectName + "' does not exist.");
        }
    }

    public String getProjectName() {
        return projectName;
    }
}
