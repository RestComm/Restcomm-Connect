package org.mobicents.servlet.restcomm.rvd;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class ProjectService {
    static final Logger logger = Logger.getLogger(BuildService.class.getName());
    private ServletContext servletContext; // TODO we have to find way other that directly through constructor parameter.

    ProjectStorage projectStorage;
    RvdSettings settings;

    // configuration parameters
    private static final String workspaceDirectoryName = "workspace";
    private static final String wavsDirectoryName = "wavs";

    //private String workspaceBasePath;

    public ProjectService(ProjectStorage projectStorage, ServletContext servletContext, RvdSettings settings) {
        this.servletContext = servletContext;
        this.projectStorage = projectStorage;
        this.settings = settings;
        //workspaceBasePath = this.servletContext.getRealPath(File.separator) + workspaceDirectoryName;
    }
    public static String getWorkspacedirectoryname() {
        return workspaceDirectoryName;
    }
    public static String getWavsdirectoryname() {
        return wavsDirectoryName;
    }

    /**
     * Builds the startUrl for an application based on the application name and the incoming httpRequest. It is depending on the
     * initial REST request that called this function. Usually this httpRequest comes either from user's browser when he runs
     * Admin-UI or RVD. However, this url will be used in Restcomm too. Make sure that Restcomm can access the generated the
     * same way client's browser does.
     *
     * @param projectName
     * @param httpRequest
     * @return An absolute url pointing to the starting URL of the application
     * @throws UnsupportedEncodingException
     * @throws URISyntaxException
     */
    public static String getStartUrlForProject(String projectName, HttpServletRequest httpRequest) throws URISyntaxException {
        URI startURI = new URI(httpRequest.getScheme(), null, httpRequest.getServerName(),
                (httpRequest.getServerPort() == 80 ? -1 : httpRequest.getServerPort()), httpRequest.getContextPath()
                        + httpRequest.getServletPath() + "/apps/" + projectName + "/controller", null, null);

        /*
         * String startUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + (httpRequest.getServerPort() == 80
         * ? "" : (":"+ httpRequest.getServerPort())) + httpRequest.getContextPath() + httpRequest.getServletPath() + "/apps/" +
         * projectName + "/controller"; return startUrl;
         */

        return startURI.toASCIIString();
    }

    /**
     * Populates an application list with startup urls for each application
     *
     * @param items
     * @param httpRequest
     * @throws URISyntaxException
     * @throws UnsupportedEncodingException
     */
    public static void fillStartUrlsForProjects(List<ProjectItem> items, HttpServletRequest httpRequest)
            throws URISyntaxException {
        for (ProjectItem item : items) {
            item.setStartUrl(getStartUrlForProject(item.getName(), httpRequest));
        }
    }

    public List<ProjectItem> getAvailableProjects() throws StorageException {

        List<ProjectItem> items = new ArrayList<ProjectItem>();
        for (String entry : projectStorage.listProjectNames() ) {
            ProjectItem item = new ProjectItem();
            item.setName(entry);
            //item.setStartUrl(entry.getName());
            items.add(item);
        }
        return items;
    }

    public String openProject(String projectName) throws ProjectDoesNotExist, StorageException {
        if ( !projectExists(projectName) )
            throw new ProjectDoesNotExist();

        return projectStorage.loadProjectState(projectName);
    }

    public boolean projectExists(String projectName) throws BadWorkspaceDirectoryStructure {
        return projectStorage.projectExists(projectName);
    }

    public void createProject(String projectName) throws StorageException {
        projectStorage.cloneProject(settings.getOption("protoProjectName"), projectName);
    }

    public void updateProject(HttpServletRequest request, String projectName) throws IOException, StorageException {
        String state = IOUtils.toString(request.getInputStream());
        projectStorage.updateProjectState(projectName, state);

    }

    public void renameProject(String projectName, String newProjectName) throws ProjectDoesNotExist, StorageException {
        if (  ! projectStorage.projectExists(projectName) ) {
            throw new ProjectDoesNotExist();
        } else if ( projectStorage.projectExists(newProjectName) ) {
            throw new ProjectDirectoryAlreadyExists();
        }
        projectStorage.renameProject(projectName, newProjectName);
    }

    public void deleteProject(String projectName) throws ProjectDoesNotExist, StorageException {
        if (! projectStorage.projectExists(projectName))
            throw new ProjectDoesNotExist();
        projectStorage.deleteProject(projectName);
    }

    public void addWavToProject(String projectName, String wavName, InputStream wavStream) throws StorageException {
        projectStorage.storeWav(projectName, wavName, wavStream);
    }

    public List<WavItem> getWavs(String appName) throws StorageException {
        return projectStorage.listWavs(appName);
    }

    public void removeWavFromProject(String projectName, String wavName) throws WavItemDoesNotExist {
        projectStorage.deleteWav(projectName, wavName);
    }
}
