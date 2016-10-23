package org.restcomm.connect.rvd.http.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.BuildService;
import org.restcomm.connect.rvd.ProjectApplicationsApi;
import org.restcomm.connect.rvd.ProjectService;
import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.RvdContext;
import org.restcomm.connect.rvd.exceptions.ApplicationAlreadyExists;
import org.restcomm.connect.rvd.exceptions.ApplicationApiNotSynchedException;
import org.restcomm.connect.rvd.exceptions.ApplicationsApiSyncException;
import org.restcomm.connect.rvd.exceptions.IncompatibleProjectVersion;
import org.restcomm.connect.rvd.exceptions.InvalidServiceParameters;
import org.restcomm.connect.rvd.exceptions.ProjectDoesNotExist;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.exceptions.project.ProjectException;
import org.restcomm.connect.rvd.exceptions.project.UnsupportedProjectVersion;
import org.restcomm.connect.rvd.http.RvdResponse;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.jsonvalidation.exceptions.ValidationException;
import org.restcomm.connect.rvd.model.CallControlInfo;
import org.restcomm.connect.rvd.model.ModelMarshaler;
import org.restcomm.connect.rvd.model.ProjectSettings;
import org.restcomm.connect.rvd.model.client.ProjectItem;
import org.restcomm.connect.rvd.model.client.ProjectState;
import org.restcomm.connect.rvd.model.client.StateHeader;
import org.restcomm.connect.rvd.model.client.WavItem;
import org.restcomm.connect.rvd.storage.FsCallControlInfoStorage;
import org.restcomm.connect.rvd.storage.FsProjectStorage;
import org.restcomm.connect.rvd.storage.WorkspaceStorage;
import org.restcomm.connect.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.restcomm.connect.rvd.storage.exceptions.ProjectAlreadyExists;
import org.restcomm.connect.rvd.storage.exceptions.StorageEntityNotFound;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;
import org.restcomm.connect.rvd.storage.exceptions.WavItemDoesNotExist;
import org.restcomm.connect.rvd.upgrade.UpgradeService;
import org.restcomm.connect.rvd.upgrade.exceptions.UpgradeException;
import org.restcomm.connect.rvd.utils.RvdUtils;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("projects")
public class ProjectRestService extends SecuredRestService {

    static final Logger logger = Logger.getLogger(ProjectRestService.class.getName());

    @Context
    HttpServletRequest request;

    private ProjectService projectService;
    private RvdConfiguration rvdSettings;
    private ProjectState activeProject;
    private ModelMarshaler marshaler;
    private WorkspaceStorage workspaceStorage;

    RvdContext rvdContext;

    @PostConstruct
    public void init() {
        super.init();
        rvdContext = new RvdContext(request, servletContext,applicationContext.getConfiguration());
        rvdSettings = rvdContext.getSettings();
        marshaler = rvdContext.getMarshaler();
        workspaceStorage = new WorkspaceStorage(rvdSettings.getWorkspaceBasePath(), marshaler);
        projectService = new ProjectService(rvdContext, workspaceStorage);
    }

    public ProjectRestService() {
    }

    ProjectRestService(UserIdentityContext context) {
        super(context);
    }

    /**
     * Make sure the specified project has been loaded and is available for use. Checks logged user too. Also the loaded project
     * is placed in the activeProject variable
     *
     * @param projectName
     * @return
     * @throws StorageException, WebApplicationException/unauthorized
     * @throws ProjectDoesNotExist
     */
    void assertProjectAvailable(String projectName) throws StorageException, ProjectDoesNotExist {
        if (!FsProjectStorage.projectExists(projectName, workspaceStorage))
            throw new ProjectDoesNotExist("Project " + projectName + " does not exist");
        ProjectState project = FsProjectStorage.loadProject(projectName, workspaceStorage);
        if (project.getHeader().getOwner() != null) {
            // needs further checking
            String loggedUser = getUserIdentityContext().getAccountUsername();
            if (loggedUser != null && loggedUser.equals(project.getHeader().getOwner()) ) {
                this.activeProject = project;
                return;
            }
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        activeProject = project;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProjects(@Context HttpServletRequest request) {
        secure();
        List<ProjectItem> items;
        try {
            items = projectService.getAvailableProjectsByOwner(getLoggedUsername());
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

    @PUT
    @Path("{name}")
    public Response createProject(@PathParam("name") String name, @QueryParam("kind") String kind,
            @QueryParam("ticket") String ticket) {
        secure();
        ProjectApplicationsApi applicationsApi = null;
        String applicationSid = null;
        if(logger.isInfoEnabled()) {
            logger.info("Creating project " + name);
        }
        try {
            applicationsApi = new ProjectApplicationsApi(getUserIdentityContext(),applicationContext);
            applicationSid = applicationsApi.createApplication(name, kind);
            ProjectState projectState = projectService.createProject(applicationSid, kind, getLoggedUsername());
            BuildService buildService = new BuildService(workspaceStorage);
            buildService.buildProject(applicationSid, projectState);

        } catch (ProjectAlreadyExists e) {
            logger.error(e.getMessage(), e);
            try {
                applicationsApi.rollbackCreateApplication(applicationSid);
            } catch (ApplicationsApiSyncException e1) {
                logger.error(e1.getMessage(), e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Status.CONFLICT).build();
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (InvalidServiceParameters e) {
            logger.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (ApplicationAlreadyExists e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.CONFLICT).build();
        } catch (ApplicationsApiSyncException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        Gson gson = new Gson();
        JsonObject projectInfo = new JsonObject();
        projectInfo.addProperty("name", name);
        projectInfo.addProperty("sid", applicationSid);
        projectInfo.addProperty("kind", kind);
        return Response.ok(gson.toJson(projectInfo), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Retrieves project header information. Returns the project header or null if it does not exist (for old projects) as JSON
     * - OK status. Returns INTERNAL_SERVER_ERROR status and no response body for serious errors
     *
     * @param applicationSid - The application sid to get information for
     * @throws StorageException
     * @throws ProjectDoesNotExist
     */
    @GET
    @Path("{applicationSid}/info")
    public Response projectInfo(@PathParam("applicationSid") String applicationSid) throws StorageException, ProjectDoesNotExist {
        secure();
        assertProjectAvailable(applicationSid);

        StateHeader header = activeProject.getHeader();
        return Response.status(Status.OK).entity(marshaler.getGson().toJson(header)).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("{applicationSid}")
    public Response updateProject(@Context HttpServletRequest request, @PathParam("applicationSid") String applicationSid) {
        secure();
        if (applicationSid != null && !applicationSid.equals("")) {
            if(logger.isInfoEnabled()) {
                logger.info("Saving project " + applicationSid);
            }
            try {
                ProjectState existingProject = FsProjectStorage.loadProject(applicationSid, workspaceStorage);

                if (getLoggedUsername().equals(existingProject.getHeader().getOwner())
                        || existingProject.getHeader().getOwner() == null) {
                    projectService.updateProject(request, applicationSid, existingProject);
                    return buildOkResponse();
                } else {
                    throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                }
            } catch (ValidationException e) {
                RvdResponse rvdResponse = new RvdResponse().setValidationException(e);
                return Response.status(Status.OK).entity(rvdResponse.asJson()).build();
                // return buildInvalidResponse(Status.OK, RvdResponse.Status.INVALID,e);
                // Gson gson = new Gson();
                // return Response.ok(gson.toJson(e.getValidationResult()), MediaType.APPLICATION_JSON).build();
            } catch (IncompatibleProjectVersion e) {
                logger.error(e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.asJson()).type(MediaType.APPLICATION_JSON)
                        .build();
            } catch (RvdException e) {
                logger.error(e.getMessage(), e);
                return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
                // return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            logger.warn("Empty project name specified for updating");
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * Store Call Control project information
     */
    @POST
    @Path("{applicationSid}/cc")
    public Response storeCcInfo(@PathParam("applicationSid") String applicationSid, @Context HttpServletRequest request) {
        secure();
        try {
            String data = IOUtils.toString(request.getInputStream(), Charset.forName("UTF-8"));
            CallControlInfo ccInfo = marshaler.toModel(data, CallControlInfo.class);
            if (ccInfo != null)
                FsCallControlInfoStorage.storeInfo(ccInfo, applicationSid, workspaceStorage);
            else
                FsCallControlInfoStorage.clearInfo(applicationSid, workspaceStorage);

            return Response.ok().build();
        } catch (IOException e) {
            logger.error(e, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageException e) {
            logger.error(e, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{applicationSid}/cc")
    public Response getCcInfo(@PathParam("applicationSid") String applicationSid) {
        secure();
        try {
            CallControlInfo ccInfo = FsCallControlInfoStorage.loadInfo(applicationSid, workspaceStorage);
            return Response.ok(marshaler.toData(ccInfo), MediaType.APPLICATION_JSON).build();
            // return buildOkResponse(ccInfo);
        } catch (StorageEntityNotFound e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (StorageException e) {
            logger.error(e, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("{applicationSid}/rename")
    public Response renameProject(@PathParam("applicationSid") String applicationSid, @QueryParam("newName") String projectNewName,
            @QueryParam("ticket") String ticket) throws StorageException, ProjectDoesNotExist {
        secure();
        if (!RvdUtils.isEmpty(applicationSid) && !RvdUtils.isEmpty(projectNewName)) {
            assertProjectAvailable(applicationSid);
            try {
                ProjectApplicationsApi applicationsApi = new ProjectApplicationsApi(getUserIdentityContext(),applicationContext);
                try {
                    applicationsApi.renameApplication(applicationSid, projectNewName);
                } catch (ApplicationApiNotSynchedException e) {
                    logger.warn(e.getMessage());
                }
                return Response.ok().build();
            } catch (ApplicationAlreadyExists e) {
                return Response.status(Status.CONFLICT).build();
            } catch (ApplicationsApiSyncException e) {
                logger.error(e.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @PUT
    @Path("{applicationSid}/upgrade")
    public Response upgradeProject(@PathParam("applicationSid") String applicationSid) {
        secure();

        // TODO IMPORTANT!!! sanitize the project name!!
        if (!RvdUtils.isEmpty(applicationSid)) {
            try {
                UpgradeService upgradeService = new UpgradeService(workspaceStorage);
                upgradeService.upgradeProject(applicationSid);
                if(logger.isInfoEnabled()) {
                    logger.info("project '" + applicationSid + "' upgraded to version " + RvdConfiguration.getRvdProjectVersion());
                }
                // re-build project
                BuildService buildService = new BuildService(workspaceStorage);
                buildService.buildProject(applicationSid, activeProject);
                if(logger.isInfoEnabled()) {
                    logger.info("project '" + applicationSid + "' built");
                }
                return Response.ok().build();
            } catch (StorageException e) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (UpgradeException e) {
                logger.error(e.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.asJson()).type(MediaType.APPLICATION_JSON)
                        .build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @DELETE
    @Path("{applicationSid}")
    public Response deleteProject(@PathParam("applicationSid") String applicationSid, @QueryParam("ticket") String ticket)
            throws ProjectDoesNotExist {
        secure();
        if (!RvdUtils.isEmpty(applicationSid)) {
            try {
                ProjectApplicationsApi applicationsApi = new ProjectApplicationsApi(getUserIdentityContext(),applicationContext);
                applicationsApi.removeApplication(applicationSid);
                projectService.deleteProject(applicationSid);
                return Response.ok().build();
            } catch (StorageException e) {
                logger.error("Error deleting project '" + applicationSid + "'", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (ApplicationsApiSyncException e) {
                logger.error("Error deleting project '" + applicationSid + "' through the API", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @GET
    @Path("{applicationSid}/archive")
    public Response downloadArchive(@PathParam("applicationSid") String applicationSid,
            @QueryParam("projectName") String projectName)
            throws StorageException, ProjectDoesNotExist, UnsupportedEncodingException, EncoderException {
        secure();
        if(logger.isDebugEnabled()) {
            logger.debug("downloading raw archive for project " + applicationSid);
        }
        assertProjectAvailable(applicationSid);

        InputStream archiveStream;
        try {
            archiveStream = projectService.archiveProject(applicationSid);
            String dispositionHeader = "attachment; filename*=UTF-8''" + RvdUtils.myUrlEncode(projectName + ".zip");
            return Response.ok(archiveStream, "application/zip").header("Content-Disposition", dispositionHeader).build();

        } catch (StorageException e) {
            logger.error(e, e);
            return null;
        }
    }

    @POST
    // @Path("{name}/archive")
    public Response importProjectArchive(@Context HttpServletRequest request, @QueryParam("ticket") String ticket) {
        secure();
        if(logger.isInfoEnabled()) {
            logger.info("Importing project from raw archive");
        }
        ProjectApplicationsApi applicationsApi = null;
        String applicationSid = null;

        try {
            if (request.getHeader("Content-Type") != null
                    && request.getHeader("Content-Type").startsWith("multipart/form-data")) {
                Gson gson = new Gson();
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator iterator = upload.getItemIterator(request);

                JsonArray fileinfos = new JsonArray();

                while (iterator.hasNext()) {
                    FileItemStream item = iterator.next();
                    JsonObject fileinfo = new JsonObject();
                    fileinfo.addProperty("field", item.getFieldName());

                    // is this a file part (talking about multipart requests, there might be parts that are not actual files).
                    // They will be ignored
                    if (item.getName() != null) {
                        // Create application
                        String tempName = "RvdImport-" + UUID.randomUUID().toString().replace("-", "");
                        applicationsApi = new ProjectApplicationsApi(getUserIdentityContext(),applicationContext);
                        applicationSid = applicationsApi.createApplication(tempName, "");

                        String effectiveProjectName = null;

                        try {
                            // Import application
                            projectService.importProjectFromRawArchive(item.openStream(), applicationSid, getLoggedUsername());
                            effectiveProjectName = FilenameUtils.getBaseName(item.getName());
                            // buildService.buildProject(effectiveProjectName);

                            // Load project kind
                            String projectString = FsProjectStorage.loadProjectString(applicationSid, workspaceStorage);
                            ProjectState state = marshaler.toModel(projectString, ProjectState.class);
                            String projectKind = state.getHeader().getProjectKind();

                            // Update application
                            applicationsApi.updateApplication(applicationSid, effectiveProjectName, null, projectKind);
                            if(logger.isInfoEnabled()) {
                                logger.info("Successfully imported project '" + applicationSid + "' from raw archive '" + item.getName() + "'");
                            }

                        } catch (Exception e) {
                            applicationsApi.rollbackCreateApplication(applicationSid);
                            throw e;
                        }

                        //fileinfo.addProperty("name", item.getName());
                        fileinfo.addProperty("name", effectiveProjectName);
                        fileinfo.addProperty("id", applicationSid);

                    }
                    if (item.getName() == null) {
                        logger.warn("non-file part found in upload");
                        fileinfo.addProperty("value", read(item.openStream()));
                    }
                    fileinfos.add(fileinfo);
                }
                return Response.ok(gson.toJson(fileinfos), MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Status.BAD_REQUEST).build();
            }
        } catch (StorageException | UnsupportedProjectVersion e) {
            logger.warn(e, e);
            if(logger.isDebugEnabled()) {
                logger.debug(e, e);
            }
            return buildErrorResponse(Status.BAD_REQUEST, RvdResponse.Status.ERROR, e);
        } catch (ApplicationAlreadyExists e) {
            logger.warn(e, e);
            if(logger.isDebugEnabled()) {
                logger.debug(e, e);
            }
            try {
                applicationsApi.rollbackCreateApplication(applicationSid);
            } catch (ApplicationsApiSyncException e1) {
                logger.error(e1.getMessage(), e1);
                return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
            }
            return buildErrorResponse(Status.CONFLICT, RvdResponse.Status.ERROR, e);
        } catch (Exception e /* TODO - use a more specific type !!! */) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{applicationSid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response openProject(@PathParam("applicationSid") String applicationSid, @Context HttpServletRequest request)
            throws StorageException,
            ProjectDoesNotExist {
        secure();
        assertProjectAvailable(applicationSid);
        return Response.ok().entity(marshaler.toData(activeProject)).build();
        /*
         * try { String projectState = projectService.openProject(name); return Response.ok().entity(projectState).build(); }
         * catch (StorageException e) { logger.error("Error loading project '" + name + "'", e); return
         * Response.status(Status.INTERNAL_SERVER_ERROR).build(); } catch (ProjectDoesNotExist e) { return
         * Response.status(Status.NOT_FOUND).entity(e.asJson()).type(MediaType.APPLICATION_JSON).build(); } catch
         * (IncompatibleProjectVersion e) { logger.error(e.getMessage(), e); return
         * Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.asJson()).type(MediaType.APPLICATION_JSON).build(); }
         */
    }

    @POST
    @Path("{applicationSid}/wavs")
    public Response uploadWavFile(@PathParam("applicationSid") String applicationSid, @Context HttpServletRequest request)
            throws StorageException, ProjectDoesNotExist {
        secure();
        if(logger.isInfoEnabled()) {
            logger.info("running /uploadwav");
        }
        assertProjectAvailable(applicationSid);
        try {
            if (request.getHeader("Content-Type") != null
                    && request.getHeader("Content-Type").startsWith("multipart/form-data")) {
                Gson gson = new Gson();
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator iterator = upload.getItemIterator(request);

                JsonArray fileinfos = new JsonArray();

                while (iterator.hasNext()) {
                    FileItemStream item = iterator.next();
                    JsonObject fileinfo = new JsonObject();
                    fileinfo.addProperty("fieldName", item.getFieldName());

                    // is this a file part (talking about multipart requests, there might be parts that are not actual files).
                    // They will be ignored
                    if (item.getName() != null) {
                        projectService.addWavToProject(applicationSid, item.getName(), item.openStream());
                        fileinfo.addProperty("name", item.getName());
                        // fileinfo.addProperty("size", size(item.openStream()));
                    }
                    if (item.getName() == null) {
                        logger.warn("non-file part found in upload");
                        fileinfo.addProperty("value", read(item.openStream()));
                    }
                    fileinfos.add(fileinfo);
                }

                return Response.ok(gson.toJson(fileinfos), MediaType.APPLICATION_JSON).build();

            } else {

                String json_response = "{\"result\":[{\"size\":" + size(request.getInputStream()) + "}]}";
                return Response.ok(json_response, MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e /* TODO - use a more specific type !!! */) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("{applicationSid}/wavs")
    public Response removeWavFile(@PathParam("applicationSid") String applicationSid, @QueryParam("filename") String wavname,
            @Context HttpServletRequest request) throws StorageException, ProjectDoesNotExist {
        secure();
        assertProjectAvailable(applicationSid);
        try {
            projectService.removeWavFromProject(applicationSid, wavname);
            return Response.ok().build();
        } catch (WavItemDoesNotExist e) {
            logger.warn("Cannot delete " + wavname + " from " + applicationSid + " app");
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("{applicationSid}/wavs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listWavs(@PathParam("applicationSid") String applicationSid) throws StorageException, ProjectDoesNotExist {
        secure();
        assertProjectAvailable(applicationSid);
        List<WavItem> items;
        try {

            items = projectService.getWavs(applicationSid);
            Gson gson = new Gson();
            return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
        } catch (BadWorkspaceDirectoryStructure e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageException e) {
            logger.error("Error getting wav list for project '" + applicationSid + "'", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * Return a wav file from the project. It's the same as getWav() but it has the Query parameters converted to Path
     * parameters
     */
    @GET
    @Path("{applicationSid}/wavs/{filename}.wav")
    public Response getWavNoQueryParams(@PathParam("applicationSid") String applicationSid,
            @PathParam("filename") String filename) {
        InputStream wavStream;
        try {
            wavStream = FsProjectStorage.getWav(applicationSid, filename + ".wav", workspaceStorage);
            return Response.ok(wavStream, "audio/x-wav").header("Content-Disposition", "attachment; filename = " + filename)
                    .build();
        } catch (WavItemDoesNotExist e) {
            return Response.status(Status.NOT_FOUND).build(); // ordinary error page is returned since this will be consumed
                                                              // either from restcomm or directly from user
        } catch (StorageException e) {
            // return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build(); // ordinary error page is returned since this will be
                                                                          // consumed either from restcomm or directly from user
        }
    }

    @POST
    @Path("{applicationSid}/build")
    public Response buildProject(@PathParam("applicationSid") String applicationSid) throws StorageException,
            ProjectDoesNotExist {
        secure();
        assertProjectAvailable(applicationSid);
        BuildService buildService = new BuildService(workspaceStorage);
        try {
            buildService.buildProject(applicationSid, activeProject);
            return Response.ok().build();
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("{applicationSid}/settings")
    public Response saveProjectSettings(@PathParam("applicationSid") String applicationSid) {
        secure();
        if(logger.isInfoEnabled()) {
            logger.info("saving project settings for " + applicationSid);
        }
        String data;
        try {
            data = IOUtils.toString(request.getInputStream(), Charset.forName("UTF-8"));
            ProjectSettings projectSettings = marshaler.toModel(data, ProjectSettings.class);
            FsProjectStorage.storeProjectSettings(projectSettings, applicationSid, workspaceStorage);
            return Response.ok().build();
        } catch (StorageException e) {
            logger.error(e, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            logger.error(e, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GET
    @Path("{applicationSid}/settings")
    public Response getProjectSettings(@PathParam("applicationSid") String applicationSid) {
        secure();
        try {
            ProjectSettings projectSettings = FsProjectStorage.loadProjectSettings(applicationSid, workspaceStorage);
            return Response.ok(marshaler.toData(projectSettings)).build();
        } catch (StorageEntityNotFound e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (StorageException e) {
            logger.error(e, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
