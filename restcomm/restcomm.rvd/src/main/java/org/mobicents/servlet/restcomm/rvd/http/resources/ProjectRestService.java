package org.mobicents.servlet.restcomm.rvd.http.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.keycloak.KeycloakSecurityContext;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RasService;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.exceptions.IncompatibleProjectVersion;
import org.mobicents.servlet.restcomm.rvd.exceptions.InvalidServiceParameters;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.exceptions.UnauthorizedException;
import org.mobicents.servlet.restcomm.rvd.exceptions.packaging.PackagingDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.project.ProjectException;
import org.mobicents.servlet.restcomm.rvd.http.RestService;
import org.mobicents.servlet.restcomm.rvd.http.RvdResponse;
import org.mobicents.servlet.restcomm.rvd.jsonvalidation.exceptions.ValidationException;
import org.mobicents.servlet.restcomm.rvd.model.CallControlInfo;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.ProjectSettings;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.model.packaging.Rapp;
import org.mobicents.servlet.restcomm.rvd.model.packaging.RappBinaryInfo;
import org.mobicents.servlet.restcomm.rvd.model.project.RvdProject;
import org.mobicents.servlet.restcomm.rvd.security.SecurityUtils;
import org.mobicents.servlet.restcomm.rvd.storage.FsCallControlInfoStorage;
import org.mobicents.servlet.restcomm.rvd.storage.FsPackagingStorage;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.ProjectAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageEntityNotFound;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.upgrade.UpgradeService;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.UpgradeException;
import org.mobicents.servlet.restcomm.rvd.validation.exceptions.RvdValidationException;


@Path("projects")
public class ProjectRestService extends RestService {

    static final Logger logger = Logger.getLogger(ProjectRestService.class.getName());

    @Context
    ServletContext servletContext;
    @Context
    SecurityContext securityContext;
    @Context
    HttpServletRequest request;

    private ProjectService projectService;
    private RvdConfiguration rvdSettings;
    private ModelMarshaler marshaler;
    private WorkspaceStorage workspaceStorage;
    private String loggedUsername;
    private RasService rasService;

    RvdContext rvdContext;

    @PostConstruct
    void init() {
        rvdContext = new RvdContext(request, servletContext);
        rvdSettings = rvdContext.getSettings();
        marshaler = rvdContext.getMarshaler();
        workspaceStorage = new WorkspaceStorage(rvdSettings.getWorkspaceBasePath(), marshaler);
        rasService = new RasService(rvdContext, workspaceStorage);
        projectService = new ProjectService(rvdContext,workspaceStorage);
        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        if (session.getToken() != null)
            loggedUsername = session.getToken().getPreferredUsername();
    }

    /*
     * Load a project and make sure current user has access to it
     */
    ProjectState loadUserProject(String projectName) throws StorageException, ProjectDoesNotExist {
        if (! FsProjectStorage.projectExists(projectName,workspaceStorage))
            throw new ProjectDoesNotExist("Project " + projectName + " does not exist");
        ProjectState project = FsProjectStorage.loadProject(projectName, workspaceStorage);
        if ( project.getHeader().getOwner() != null ) {
            if ( loggedUsername != null ) {
                if ( loggedUsername.equals(project.getHeader().getOwner() ) ) {
                    return project;
                }
            }
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        return project;
    }

    void assertUserLogged() {
        if (loggedUsername == null)
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProjects(@Context HttpServletRequest request) throws UnauthorizedException {
        //assertUserLogged();
        List<ProjectItem> items;
        try {
            secureByRole("RestcommUser", getKeycloakAccessToken());
            items = projectService.getAvailableProjectsByOwner(loggedUsername); // there has to be a user in the context. Only logged users are allowed to to run project manager services
            projectService.fillStartUrlsForProjects(items, request);
        } catch (BadWorkspaceDirectoryStructure e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (ProjectException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
    }
    
    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProject(@PathParam("name") String name, @Context HttpServletRequest request) throws StorageException, ProjectDoesNotExist {
        ProjectState activeProject = loadUserProject(name);
        return Response.ok().entity(marshaler.toData(activeProject)).build();
    }    

    @PUT
    @Path("{name}")
    public Response createProject(@PathParam("name") String name, @QueryParam("kind") String kind) throws UnauthorizedException {
        //assertUserLogged();
        secureByRole("Developer", getKeycloakAccessToken());
        logger.info("Creating project " + name);
        try {

            ProjectState projectState = projectService.createProject(name, kind, loggedUsername);
            BuildService buildService = new BuildService(workspaceStorage);
            buildService.buildProject(name, projectState);
        } catch (ProjectAlreadyExists e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.CONFLICT).build();
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (InvalidServiceParameters e) {
            logger.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }
    
    @POST
    @Path("{name}")
    public Response updateProject(@Context HttpServletRequest request, @PathParam("name") String projectName) throws UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        if (projectName != null && !projectName.equals("")) {
            logger.info("Saving project " + projectName);
            try {
                ProjectState existingProject = FsProjectStorage.loadProject(projectName, workspaceStorage);
                if (loggedUsername != null && (loggedUsername.equals(existingProject.getHeader().getOwner())  ||  existingProject.getHeader().getOwner() == null )) {
                    projectService.updateProject(request, projectName, existingProject);
                    return buildOkResponse();
                } else {
                    throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                }
            } catch (ValidationException e) {
                RvdResponse rvdResponse = new RvdResponse().setValidationException(e);
                return Response.status(Status.OK).entity(rvdResponse.asJson()).build();
                //return buildInvalidResponse(Status.OK, RvdResponse.Status.INVALID,e);
                //Gson gson = new Gson();
                //return Response.ok(gson.toJson(e.getValidationResult()), MediaType.APPLICATION_JSON).build();
            } catch (IncompatibleProjectVersion e) {
                logger.error(e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.asJson()).type(MediaType.APPLICATION_JSON).build();
            } catch (RvdException e) {
                logger.error(e.getMessage(), e);
                return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
                //return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            logger.warn("Empty project name specified for updating");
            return Response.status(Status.BAD_REQUEST).build();
        }
    }    
    
    @DELETE
    @Path("{name}")
    public Response deleteProject(@PathParam("name") String projectName) throws ProjectDoesNotExist, UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        if ( ! RvdUtils.isEmpty(projectName) ) {
            try {
                projectService.deleteProject(projectName);
                return Response.ok().build();
            } catch (StorageException e) {
                logger.error("Error deleting project '" + projectName + "'", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }    

    @PUT
    @Path("{name}/rename")
    public Response renameProject(@PathParam("name") String projectName, @QueryParam("newName") String projectNewName) throws StorageException, ProjectDoesNotExist, UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        if ( !RvdUtils.isEmpty(projectName) && ! RvdUtils.isEmpty(projectNewName) ) {
            loadUserProject(projectName);
            try {
                projectService.renameProject(projectName, projectNewName);
                return Response.ok().build();
            } catch (ProjectDirectoryAlreadyExists e) {
                logger.error(e.getMessage(), e);
                return Response.status(Status.CONFLICT).build();
            } catch (StorageException e) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @PUT
    @Path("{name}/upgrade")
    public Response upgradeProject(@PathParam("name") String projectName) throws StorageException, ProjectDoesNotExist, UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        // TODO IMPORTANT!!! sanitize the project name!!
        ProjectState activeProject = loadUserProject(projectName);
        if ( !RvdUtils.isEmpty(projectName) ) {
            try {
                UpgradeService upgradeService = new UpgradeService(workspaceStorage);
                upgradeService.upgradeProject(projectName);
                logger.info("project '" + projectName + "' upgraded to version " + RvdConfiguration.getRvdProjectVersion() );
                // re-build project
                BuildService buildService = new BuildService(workspaceStorage);
                buildService.buildProject(projectName, activeProject);
                logger.info("project '" + projectName + "' built");
                return Response.ok().build();
            }
            catch (StorageException e) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (UpgradeException e) {
                logger.error(e.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.asJson()).type(MediaType.APPLICATION_JSON).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }
    
    /*
     * Retrieves project header information.
     * Returns INTERNAL_SERVER_ERROR status and no response body for serious errors
     */
    @GET
    @Path("{name}/info")
    public Response projectInfo(@PathParam("name") String name) throws StorageException, ProjectDoesNotExist, UnauthorizedException {
        //secureByRole("Developer", getKeycloakAccessToken());
        //assertUserLogged();
        ProjectState project = ProjectService.loadProject(name, workspaceStorage);
        if (!SecurityUtils.userCanAccessProject(loggedUsername, project))
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        StateHeader header = project.getHeader();
        return Response.status(Status.OK).entity(marshaler.getGson().toJson(header)).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * Store Call Control project information
     * @throws UnauthorizedException
     */
    @POST
    @Path("{name}/cc")
    public Response storeCcInfo(@PathParam("name") String projectName, @Context HttpServletRequest request) throws UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        try {
            String data = IOUtils.toString(request.getInputStream(), Charset.forName("UTF-8"));
            CallControlInfo ccInfo = marshaler.toModel(data, CallControlInfo.class);
            if ( ccInfo != null )
                FsCallControlInfoStorage.storeInfo( ccInfo, projectName, workspaceStorage);
            else
                FsCallControlInfoStorage.clearInfo(projectName, workspaceStorage);

            return Response.ok().build();
        } catch (IOException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{name}/cc")
    public Response getCcInfo(@PathParam("name") String projectName) throws UnauthorizedException {
        //secureByRole("Developer", getKeycloakAccessToken());
        try {
            CallControlInfo ccInfo = FsCallControlInfoStorage.loadInfo(projectName, workspaceStorage);
            return Response.ok(marshaler.toData(ccInfo), MediaType.APPLICATION_JSON).build();
            //return buildOkResponse(ccInfo);
        } catch (StorageEntityNotFound e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (StorageException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }



    @GET
    @Path("{name}/archive")
    public Response downloadArchive(@PathParam("name") String projectName) throws StorageException, ProjectDoesNotExist, UnsupportedEncodingException, EncoderException {
        logger.debug("downloading raw archive for project " + projectName);
        loadUserProject(projectName);

        InputStream archiveStream;
        try {
            archiveStream = projectService.archiveProject(projectName);
            String dispositionHeader = "attachment; filename*=UTF-8''" + RvdUtils.myUrlEncode(projectName + ".zip");
            return Response.ok(archiveStream, "application/zip").header("Content-Disposition", dispositionHeader ).build();

        } catch (StorageException e) {
            logger.error(e,e);
            return null;
        }
    }

    @POST
    public Response importProjectArchive(@Context HttpServletRequest request) throws UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        logger.info("Importing project from raw archive");

        try {
            if (request.getHeader("Content-Type") != null && request.getHeader("Content-Type").startsWith("multipart/form-data")) {
                Gson gson = new Gson();
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator iterator = upload.getItemIterator(request);

                JsonArray fileinfos = new JsonArray();

                while (iterator.hasNext()) {
                    FileItemStream item = iterator.next();
                    JsonObject fileinfo = new JsonObject();
                    fileinfo.addProperty("fieldName", item.getFieldName());

                    // is this a file part (talking about multipart requests, there might be parts that are not actual files). They will be ignored
                    if (item.getName() != null) {
                        String effectiveProjectName = projectService.importProjectFromArchive(item.openStream(), item.getName());
                        //buildService.buildProject(effectiveProjectName);

                        fileinfo.addProperty("name", item.getName());
                        fileinfo.addProperty("projectName", effectiveProjectName);

                    }
                    if (item.getName() == null) {
                        logger.warn( "non-file part found in upload");
                        fileinfo.addProperty("value", read(item.openStream()));
                    }
                    fileinfos.add(fileinfo);
                }
                return Response.ok(gson.toJson(fileinfos), MediaType.APPLICATION_JSON).build();
            } else {
                String json_response = "{\"result\":[{\"size\":" + size(request.getInputStream()) + "}]}";
                return Response.ok(json_response,MediaType.APPLICATION_JSON).build();
            }
        } catch ( StorageException e ) {
            logger.warn(e,e);
            logger.debug(e,e);
            return buildErrorResponse(Status.BAD_REQUEST, RvdResponse.Status.ERROR, e);
        } catch ( Exception e /* TODO - use a more specific  type !!! */) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GET
    @Path("{name}/wavs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listWavs(@PathParam("name") String name) throws StorageException, ProjectDoesNotExist {
        loadUserProject(name);
        List<WavItem> items;
        try {

            items = projectService.getWavs(name);
            Gson gson = new Gson();
            return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
        } catch (BadWorkspaceDirectoryStructure e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageException e) {
            logger.error("Error getting wav list for project '" + name + "'", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("{name}/wavs")
    public Response uploadWavFile(@PathParam("name") String projectName, @Context HttpServletRequest request) throws StorageException, ProjectDoesNotExist, UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        logger.info("running /uploadwav");
        loadUserProject(projectName);
        try {
            if (request.getHeader("Content-Type") != null && request.getHeader("Content-Type").startsWith("multipart/form-data")) {
                Gson gson = new Gson();
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator iterator = upload.getItemIterator(request);

                JsonArray fileinfos = new JsonArray();

                while (iterator.hasNext()) {
                    FileItemStream item = iterator.next();
                    JsonObject fileinfo = new JsonObject();
                    fileinfo.addProperty("fieldName", item.getFieldName());

                    // is this a file part (talking about multipart requests, there might be parts that are not actual files). They will be ignored
                    if (item.getName() != null) {
                        projectService.addWavToProject(projectName, item.getName(), item.openStream());
                        fileinfo.addProperty("name", item.getName());
                        //fileinfo.addProperty("size", size(item.openStream()));
                    }
                    if (item.getName() == null) {
                        logger.warn( "non-file part found in upload");
                        fileinfo.addProperty("value", read(item.openStream()));
                    }
                    fileinfos.add(fileinfo);
                }

                return Response.ok(gson.toJson(fileinfos), MediaType.APPLICATION_JSON).build();

            } else {

                String json_response = "{\"result\":[{\"size\":" + size(request.getInputStream()) + "}]}";
                return Response.ok(json_response,MediaType.APPLICATION_JSON).build();
            }
        } catch ( Exception e /* TODO - use a more specific  type !!! */) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("{name}/wavs")
    public Response removeWavFile(@PathParam("name") String projectName, @QueryParam("filename") String wavname, @Context HttpServletRequest request) throws StorageException, ProjectDoesNotExist, UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        loadUserProject(projectName);
        try {
            projectService.removeWavFromProject(projectName, wavname);
            return Response.ok().build();
        } catch (WavItemDoesNotExist e) {
            logger.warn( "Cannot delete " + wavname + " from " + projectName + " app" );
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    /*
     * Return a wav file from the project. It's the same as getWav() but it has the Query parameters converted to Path parameters
     */
    @GET
    @Path("{name}/wavs/{filename}.wav")
    public Response getWavNoQueryParams(@PathParam("name") String projectName, @PathParam("filename") String filename ) {
       InputStream wavStream;
        try {
            wavStream = FsProjectStorage.getWav(projectName, filename + ".wav", workspaceStorage );
            return Response.ok(wavStream, "audio/x-wav").header("Content-Disposition", "attachment; filename = " + filename).build();
        } catch (WavItemDoesNotExist e) {
            return Response.status(Status.NOT_FOUND).build(); // ordinary error page is returned since this will be consumed either from restcomm or directly from user
        } catch (StorageException e) {
            //return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build(); // ordinary error page is returned since this will be consumed either from restcomm or directly from user
        }
    }

    @POST
    @Path("{name}/build")
    public Response buildProject(@PathParam("name") String name) throws StorageException, ProjectDoesNotExist, UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        ProjectState activeProject = loadUserProject(name);
        BuildService buildService = new BuildService(workspaceStorage);
        try {
            buildService.buildProject(name, activeProject);
            return Response.ok().build();
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GET
    @Path("{name}/settings")
    public Response getProjectSettings(@PathParam("name") String name) {
        try {
            ProjectSettings projectSettings = FsProjectStorage.loadProjectSettings(name, workspaceStorage);
            return Response.ok(marshaler.toData(projectSettings)).build();
        } catch (StorageEntityNotFound e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (StorageException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @POST
    @Path("{name}/settings")
    public Response saveProjectSettings(@PathParam("name") String name) throws UnauthorizedException {
        secureByRole("Developer", getKeycloakAccessToken());
        logger.info("saving project settings for " + name);
        String data;
        try {
            data = IOUtils.toString(request.getInputStream());
            ProjectSettings projectSettings = marshaler.toModel(data, ProjectSettings.class);
            FsProjectStorage.storeProjectSettings(projectSettings, name, workspaceStorage);
            return Response.ok().build();
        } catch (StorageException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }
    
    /**
     * Returns application package information. If there is no packaging data
     * for this project yet it returns 404/NOT_FOUND. If the project does not even
     * exist it returns 500/INTERNAL_SERVER_ERROR
     * @param projectName
     * @return
     */
    @GET
    @Path("{name}/packaging")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppConfig(@PathParam("name") String projectName) throws StorageException, ProjectDoesNotExist {
        logger.debug("retrieving app package for project " + projectName);

        if (! FsPackagingStorage.hasPackaging(projectName, workspaceStorage) )
            return buildErrorResponse(Status.NOT_FOUND, RvdResponse.Status.OK, null);

        Rapp rapp = rasService.getApp(projectName);
        Gson gson = new Gson();

        return Response.ok().entity(gson.toJson(rapp)).build();
    }
    
    /**
     * Creates or updates an app
     * @param request
     * @param projectName
     * @return
     */
    @POST
    @Path("{name}/packaging")
    public Response saveApp(@Context HttpServletRequest request, @PathParam("name") String projectName) {
        logger.info("saving restcomm app '" + projectName + "'");
        try {
            String rappData;
            rappData = IOUtils.toString(request.getInputStream());

            Gson gson = new Gson();
            Rapp rapp = gson.fromJson(rappData, Rapp.class);
            if ( !FsPackagingStorage.hasPackaging(projectName, workspaceStorage) ) {
                rasService.createApp(rapp, projectName);
            } else {
                rasService.saveApp(rapp, projectName);
            }
            return buildOkResponse();

        } catch (IOException e) {
            RvdException returnedError = new RvdException("Error saving rapp",e);
            logger.error(returnedError,returnedError);
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, returnedError);
        } catch (RvdValidationException e) {
            return buildInvalidResponse(Status.OK, RvdResponse.Status.INVALID, e.getReport());
        } catch (StorageException e) {
            logger.error(e,e);
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        } catch (ProjectDoesNotExist e) {
            logger.warn(e,e);
            return buildErrorResponse(Status.NOT_FOUND, RvdResponse.Status.ERROR,e);
        }
    }
    
    @GET
    @Path("{name}/packaging/build")
    @Produces(MediaType.APPLICATION_JSON)
    public Response preparePackage(@PathParam("name") String projectName) {
        logger.debug("preparig app zip for project " + projectName);

        try {
            if (FsPackagingStorage.hasPackaging(projectName, workspaceStorage) ) {
                RvdProject project = projectService.load(projectName);
                project.getState().getHeader().setOwner(null); //  no owner should in the exported project
                rasService.createZipPackage(project);
                return buildErrorResponse(Status.OK, RvdResponse.Status.OK, null);
            } else {
                return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, new PackagingDoesNotExist());
            }
        } catch (RvdException e) {
            logger.error(e,e);
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        }
    }
    
    @GET
    @Path("{name}/packaging/binary/download")
    public Response downloadPackage(@PathParam("name") String projectName) {
        logger.debug("downloading app zip for project " + projectName);

        try {
            if (FsPackagingStorage.hasPackaging(projectName, workspaceStorage) ) {
                //Validator validator = new RappConfigValidator();
                InputStream zipStream = FsPackagingStorage.getRappBinary(projectName, workspaceStorage);
                return Response.ok(zipStream, "application/zip").header("Content-Disposition", "attachment; filename*=UTF-8''" + RvdUtils.myUrlEncode(projectName + ".ras.zip")).build();
            } else {
                return null;
                //return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, new PackagingDoesNotExist());
            }
        } catch (RvdException e) {
            logger.error(e,e);
            //return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
            return null;
        }
    }
    
    /**
     * Returns info about a zipped package (binary) including if it is available or not
     * @param projectName
     * @return
     */
    @GET
    @Path("{name}/packaging/binary/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBinaryStatus(@QueryParam("name") String projectName) {
        logger.debug("getting binary info for project " + projectName);

        RappBinaryInfo binaryInfo = rasService.getBinaryInfo(projectName);
        return buildOkResponse(binaryInfo);
    }
}
