package org.restcomm.connect.rvd;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.restcomm.connect.rvd.model.ProjectSettings;
import org.restcomm.connect.rvd.storage.FsProjectStorage;
import org.restcomm.connect.rvd.storage.exceptions.StorageEntityNotFound;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

public class ProjectAwareRvdContext extends RvdContext {

    private String projectName;
    private ProjectLogger projectLogger;
    private ProjectSettings projectSettings;

    public ProjectAwareRvdContext(String projectName, HttpServletRequest request, ServletContext servletContext, RvdConfiguration configuration) throws StorageException {
        super(request, servletContext, configuration);
        if (projectName == null)
            throw new IllegalArgumentException();
        setProjectName(projectName);
    }

    public ProjectAwareRvdContext(HttpServletRequest request, ServletContext servletContext, RvdConfiguration configuration) {
        super(request, servletContext, configuration);
    }

    public ProjectLogger getProjectLogger() {
        return projectLogger;
    }

    public ProjectSettings getProjectSettings() {
        return projectSettings;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
        if (projectName != null) {
            this.projectLogger = new ProjectLogger(projectName, getSettings(), getMarshaler());
            try {
                this.projectSettings = FsProjectStorage.loadProjectSettings(projectName, workspaceStorage);
            } catch (StorageEntityNotFound e) {
                this.projectSettings = ProjectSettings.createDefault();
            } catch (StorageException e) {
                throw new RuntimeException(e); // serious error
            }
        } else {
            this.projectLogger = null;
            this.projectSettings = null;
        }
    }

    public String getProjectName() {
        return projectName;
    }
}
