package org.mobicents.servlet.restcomm.rvd;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.exceptions.IncompatibleProjectVersion;
import org.mobicents.servlet.restcomm.rvd.exceptions.InvalidServiceParameters;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadProjectHeader;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.upgrade.ProjectUpgrader;
import org.mobicents.servlet.restcomm.rvd.upgrade.ProjectUpgraderFactory;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.NoUpgradePathException;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.UpgradeException;
import org.mobicents.servlet.restcomm.rvd.validation.ProjectValidator;
import org.mobicents.servlet.restcomm.rvd.validation.ValidationResult;
import org.mobicents.servlet.restcomm.rvd.validation.exceptions.ValidationFrameworkException;
import org.mobicents.servlet.restcomm.rvd.validation.exceptions.ValidationException;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    /**
     * Populates a list of ProjectItems each representing a project. The project kind property defaults to
     * 'voice' if it does not exist.
     * @throws StorageException
     */
    public List<ProjectItem> getAvailableProjects() throws StorageException {

        List<ProjectItem> items = new ArrayList<ProjectItem>();
        for (String entry : projectStorage.listProjectNames() ) {

            String kind = "voice";
            try {
                StateHeader header = projectStorage.loadStateHeader(entry);
                kind = header.getProjectKind();
            } catch ( BadProjectHeader e ) {
                // for old projects
                JsonParser parser = new JsonParser();
                JsonObject root_element = parser.parse(projectStorage.loadProjectState(entry)).getAsJsonObject();
                JsonElement projectKind_element = root_element.get("projectKind");
                if ( projectKind_element != null ) {
                    kind = projectKind_element.getAsString();
                }
            }

            ProjectItem item = new ProjectItem();
            item.setName(entry);
            item.setKind(kind);
            items.add(item);
        }
        return items;
    }

    public String openProject(String projectName) throws ProjectDoesNotExist, StorageException, IncompatibleProjectVersion {
        if ( !projectExists(projectName) )
            throw new ProjectDoesNotExist();

        try {
            StateHeader header = projectStorage.loadStateHeader(projectName);
            if ( ! header.getVersion().equals(RvdSettings.getRvdProjectVersion()) )
                throw new IncompatibleProjectVersion("Error loading project '" + projectName + "'. Project version: " + header.getVersion() + " - RVD project version: " + RvdSettings.getRvdProjectVersion() );
        } catch ( BadProjectHeader e ) {
            throw new IncompatibleProjectVersion("Bad or missing project header for project '" + projectName + "'");
        }

        return projectStorage.loadProjectState(projectName);
    }

    public boolean projectExists(String projectName) throws BadWorkspaceDirectoryStructure {
        return projectStorage.projectExists(projectName);
    }

    public void createProject(String projectName, String kind) throws StorageException, InvalidServiceParameters {
        String protoSuffix = null;
        if ( "voice".equals(kind) )
            protoSuffix = "_voice";
        else
        if ( "ussd".equals(kind) )
            protoSuffix = "_ussd";
        else
            throw new InvalidServiceParameters("Invalid project kind specified - '" + kind + "'");

        projectStorage.cloneProject(settings.getOption("protoProjectName") + protoSuffix, projectName);
    }

    public void updateProject(HttpServletRequest request, String projectName) throws IOException, StorageException, ValidationFrameworkException, ValidationException, IncompatibleProjectVersion {
        String state = IOUtils.toString(request.getInputStream());
        try {
            StateHeader header = projectStorage.loadStateHeader(projectName);
            if ( !header.getVersion().equals(RvdSettings.getRvdProjectVersion()) )
                throw new IncompatibleProjectVersion("Won't save project '" + projectName + "'. Project version: " + header.getVersion() + " - " + "RVD supported version: " + RvdSettings.getRvdProjectVersion());

            ProjectValidator validator = new ProjectValidator();
            ValidationResult result = validator.validate(state);

            // always update behaviour. Maybe it should prevent update if validation fails. It's a matter of UX
            projectStorage.updateProjectState(projectName, state);
            if (!result.isSuccess())
                throw new ValidationException(result);
        } catch (ProcessingException e) {
            throw new ValidationFrameworkException("Internal validation error", e);
        }

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
    public void upgradeProject(String projectName) throws StorageException, UpgradeException {

        String[] versionPath = new String[] {"rvd714","1.0"};

        StateHeader header = null;
        String startVersion = null;
        try {
            header = projectStorage.loadStateHeader(projectName);
            startVersion = header.getVersion();
        } catch (BadProjectHeader e) {
            // it looks like this is an old project.
            startVersion = "rvd714"; // assume this is an rvd714 project. It could be 713 as well...
        }

        String version = startVersion;
        String source = projectStorage.loadProjectState(projectName);
        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(source);

        for ( int i = 0; i < versionPath.length; i ++ ) {
            if ( versionPath[i].equals(version) ) {
                // we found the version to start the upgrade
                ProjectUpgrader upgrader = ProjectUpgraderFactory.create(version);
                root = upgrader.upgrade(root);
                version = upgrader.getResultingVersion();

                if (version.equals(versionPath[versionPath.length-1] ) )
                    break;

                // if we haven't reached the final version yet keep upgrading
            }
        }

        if ( ! version.equals(versionPath[versionPath.length-1]) ) {
            throw new NoUpgradePathException("No upgrade path for project " + projectName + "Best effort from version: " + startVersion + " - to version: " + version);
        }

        //projectStorage.updateProjectState(projectName, root.toString());
    }
}
