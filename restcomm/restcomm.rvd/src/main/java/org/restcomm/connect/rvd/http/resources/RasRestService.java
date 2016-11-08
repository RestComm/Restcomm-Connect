package org.restcomm.connect.rvd.http.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.reflect.TypeToken;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.BuildService;
import org.restcomm.connect.rvd.ProjectApplicationsApi;
import org.restcomm.connect.rvd.ProjectService;
import org.restcomm.connect.rvd.RasService;
import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.RvdContext;
import org.restcomm.connect.rvd.exceptions.ProjectDoesNotExist;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.exceptions.packaging.PackagingDoesNotExist;
import org.restcomm.connect.rvd.exceptions.project.ProjectException;
import org.restcomm.connect.rvd.exceptions.project.UnsupportedProjectVersion;
import org.restcomm.connect.rvd.exceptions.ras.InvalidRestcommAppPackage;
import org.restcomm.connect.rvd.exceptions.ras.RestcommAppAlreadyExists;
import org.restcomm.connect.rvd.exceptions.ras.UnsupportedRasApplicationVersion;
import org.restcomm.connect.rvd.http.RvdResponse;
import org.restcomm.connect.rvd.identity.UserIdentityContext;
import org.restcomm.connect.rvd.model.ModelMarshaler;
import org.restcomm.connect.rvd.model.RappItem;
import org.restcomm.connect.rvd.model.client.ProjectItem;
import org.restcomm.connect.rvd.model.client.ProjectState;
import org.restcomm.connect.rvd.model.packaging.Rapp;
import org.restcomm.connect.rvd.model.packaging.RappBinaryInfo;
import org.restcomm.connect.rvd.model.packaging.RappConfig;
import org.restcomm.connect.rvd.model.project.RvdProject;
import org.restcomm.connect.rvd.storage.FsPackagingStorage;
import org.restcomm.connect.rvd.storage.FsProjectStorage;
import org.restcomm.connect.rvd.storage.WorkspaceStorage;
import org.restcomm.connect.rvd.storage.exceptions.StorageEntityNotFound;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;
import org.restcomm.connect.rvd.utils.RvdUtils;
import org.restcomm.connect.rvd.validation.exceptions.RvdValidationException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("ras")
public class RasRestService extends SecuredRestService {
    static final Logger logger = Logger.getLogger(RasRestService.class.getName());

    private RvdConfiguration settings;
    private RasService rasService;
    private ProjectService projectService;
    private RvdContext rvdContext;
    private WorkspaceStorage workspaceStorage;
    private ModelMarshaler marshaler;

    @PostConstruct
    public void init() {
        super.init();
        rvdContext = new RvdContext(request, servletContext,applicationContext.getConfiguration());
        settings = rvdContext.getSettings();
        marshaler = rvdContext.getMarshaler();
        workspaceStorage = new WorkspaceStorage(settings.getWorkspaceBasePath(), marshaler);
        rasService = new RasService(rvdContext, workspaceStorage);
        projectService = new ProjectService(rvdContext,workspaceStorage);
    }

    public RasRestService() {
    }

    RasRestService(UserIdentityContext context) {
        super(context);
    }

    /**
     * Returns application package information. If there is no packaging data for this project yet it returns 404/NOT_FOUND. If
     * the project does not even exist it returns 500/INTERNAL_SERVER_ERROR
     *
     * @param applicationSid
     * @return
     */
    @GET
    @Path("/packaging/app")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppConfig(@QueryParam("applicationSid") String applicationSid) throws StorageException, ProjectDoesNotExist {
        secure();
        if(logger.isDebugEnabled()) {
            logger.debug("retrieving app package for project " + applicationSid);
        }
        if (!FsPackagingStorage.hasPackaging(applicationSid, workspaceStorage))
            return buildErrorResponse(Status.NOT_FOUND, RvdResponse.Status.OK, null);

        Rapp rapp = rasService.getApp(applicationSid);
        Gson gson = new Gson();

        return Response.ok().entity(gson.toJson(rapp)).build();
    }


    /**
     * Creates or updates an app
     * @param request
     * @param applicationSid
     * @return
     */
    @POST
    @Path("/packaging/app/save")
    public Response saveApp(@Context HttpServletRequest request, @QueryParam("applicationSid") String applicationSid) {
        secure();
        if(logger.isInfoEnabled()) {
            logger.info("saving restcomm app '" + applicationSid + "'");
        }
        try {
            String rappData;
            rappData = IOUtils.toString(request.getInputStream(), Charset.forName("UTF-8"));

            Gson gson = new Gson();
            Rapp rapp = gson.fromJson(rappData, Rapp.class);
            if ( !FsPackagingStorage.hasPackaging(applicationSid, workspaceStorage) ) {
                rasService.createApp(rapp, applicationSid);
            } else {
                rasService.saveApp(rapp, applicationSid);
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
    @Path("/packaging/app/prepare")
    @Produces(MediaType.APPLICATION_JSON)
    public Response preparePackage(@QueryParam("applicationSid") String applicationSid) {
        secure();
        if(logger.isDebugEnabled()) {
            logger.debug("preparig app zip for project " + applicationSid);
        }
        try {
            if (FsPackagingStorage.hasPackaging(applicationSid, workspaceStorage)) {
                RvdProject project = projectService.load(applicationSid);
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

    /**
     * Returns info about a zipped package (binary) including if it is available or not
     *
     * @param applicationSid
     * @return
     */
    @GET
    @Path("/packaging/binary/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBinaryStatus(@QueryParam("applicationSid") String applicationSid) {
        secure();
        if(logger.isDebugEnabled()) {
            logger.debug("getting binary info for project " + applicationSid);
        }

        RappBinaryInfo binaryInfo = rasService.getBinaryInfo(applicationSid);
        return buildOkResponse(binaryInfo);
    }

    @GET
    @Path("/packaging/download")
    public Response downloadPackage(@QueryParam("projectName") String projectName, @QueryParam("applicationSid") String applicationSid) {
        secure();
        if(logger.isDebugEnabled()) {
            logger.debug("downloading app zip for project " + applicationSid);
        }

        try {
            if (FsPackagingStorage.hasPackaging(applicationSid, workspaceStorage)) {
                //Validator validator = new RappConfigValidator();
                InputStream zipStream = FsPackagingStorage.getRappBinary(applicationSid, workspaceStorage);
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

    /*
    @GET
    @Path("apps")
    public Response listRapps(@Context HttpServletRequest request) {
        secure();
        List<ProjectItem> items;
        List<String> projectNames = new ArrayList<String>();
        try {
            items = projectService.getAvailableProjectsByOwner(getLoggedUsername());
            for (ProjectItem project : items) {
                projectNames.add(project.getName());
            }
            List<RappItem> rapps = FsProjectStorage.listRapps(projectNames, workspaceStorage, projectService);
            return buildOkResponse(rapps);
        } catch (StorageException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
        } catch (ProjectException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
        }

    }*/

    @POST
    @Path("apps/metadata")
    public Response listRappsByProjectSid(@Context HttpServletRequest request) throws RvdException {
        secure();
        List<String> appSids = null;
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<String>>(){}.getType();
            appSids = gson.fromJson(new InputStreamReader(request.getInputStream()),type);
        } catch (IOException e) {
            throw new RvdException("Internal error while retrieving project Sids", e);
        }
        List<ProjectItem> items;
        try {
            // retrieve project summaries and filter out projects that belong to other users
            items = projectService.getProjectSummaries(appSids, getLoggedUsername());
            // re-build the list of project names to return
            appSids.clear();
            for (ProjectItem project : items) {
                appSids.add(project.getName());
            }
            List<RappItem> rapps = FsProjectStorage.listRapps(appSids, workspaceStorage, projectService);
            return buildOkResponse(rapps);
        } catch (StorageException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
        } catch (ProjectException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
        }

    }

    /**
     * Create a new application by uploading a ras package
     * param projectNameOverride - NOT IMPLEMENTED - if specified, the project should be named like this. Otherwise a best effort is made so
     * that the project is named according to the the package content
     * @param request
     * @return
     */
    @POST
    @Path("apps")
    public Response newRasApp(@Context HttpServletRequest request) {
        secure();
        if(logger.isInfoEnabled()) {
            logger.info("uploading new ras app");
        }
        BuildService buildService = new BuildService(workspaceStorage);
        //RvdUser loggedUser = (RvdUser) securityContext.getUserPrincipal();
        ProjectApplicationsApi applicationsApi = null;
        String applicationSid = null;

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
                        //projectService.addWavToProject(projectName, item.getName(), item.openStream());
                        // Create application
                        String tempName = "RasImport-" + UUID.randomUUID().toString().replace("-", "");
                        applicationsApi = new ProjectApplicationsApi(getUserIdentityContext(),applicationContext);
                        applicationSid = applicationsApi.createApplication(tempName, "");

                        String effectiveProjectName = null;

                        try {
                            // Import application
                            effectiveProjectName = rasService.importAppToWorkspace(applicationSid, item.openStream(),
                                    getLoggedUsername(), projectService);
                            ProjectState projectState = FsProjectStorage.loadProject(applicationSid, workspaceStorage);

                            // Update application
                            applicationsApi.updateApplication(applicationSid, effectiveProjectName,
                                    null, projectState.getHeader().getProjectKind());

                            // Build application
                            buildService.buildProject(applicationSid, projectState);
                        } catch (Exception e) {
                            applicationsApi.rollbackCreateApplication(applicationSid);
                            throw e;
                        }

                        fileinfo.addProperty("name", item.getName());
                        fileinfo.addProperty("projectName", effectiveProjectName);
                        fileinfo.addProperty("applicationSid", applicationSid);

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
        } catch ( RestcommAppAlreadyExists e ) {
            logger.warn(e);
            if(logger.isDebugEnabled()) {
                logger.debug(e,e);
            }
            return buildErrorResponse(Status.CONFLICT, RvdResponse.Status.ERROR, e);
        } catch ( UnsupportedRasApplicationVersion | UnsupportedProjectVersion e ) {
            logger.error(e.getMessage(), e);
            return buildErrorResponse(Status.BAD_REQUEST, RvdResponse.Status.ERROR, e);
        } catch ( InvalidRestcommAppPackage e )  {
            logger.error(e.getMessage(), e);
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        } catch ( Exception e /* TODO - use a more specific  type !!! */) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GET
    @Path("apps/{applicationSid}/config")
    public Response getConfig(@PathParam("applicationSid") String applicationSid) {
        secure();
        //logger.info("getting configuration options for " + projectName);

        RappConfig rappConfig;
        // first, try to return the 'Rapp' from the packaging directory
        if (FsProjectStorage.hasPackagingInfo(applicationSid, workspaceStorage)) {
            return getConfigFromPackaging(applicationSid);
            /*try {
                Rapp rapp = FsProjectStorage.loadRappFromPackaging(projectName, workspaceStorage);
                return buildOkResponse(rapp.getConfig());
            } catch (StorageException e) {
                logger.error(e.getMessage(), e);
                return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
            }*/
        } else {
            try {
                rappConfig = rasService.getRappConfig(applicationSid);
                return buildOkResponse(rappConfig);
            } catch (StorageEntityNotFound e) {
                return buildErrorResponse(Status.OK, RvdResponse.Status.NOT_FOUND, e);
            } catch (StorageException e) {
                logger.error(e.getMessage(), e);
                return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
            }
        }
    }

    @GET
    @Path("apps/{applicationSid}")
    public Response getRapp(@PathParam("applicationSid") String applicationSid) throws StorageException {
        secure();
        if(logger.isInfoEnabled()) {
            logger.info("getting info for " + applicationSid);
        }
        try {
            Rapp rapp;
            if (FsProjectStorage.hasPackagingInfo(applicationSid, workspaceStorage))
                rapp = FsProjectStorage.loadRappFromPackaging(applicationSid, workspaceStorage);
            else
                rapp = FsProjectStorage.loadRapp(applicationSid, workspaceStorage);
            return buildOkResponse(rapp);
        } catch (StorageEntityNotFound e) {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("apps/{applicationSid}/config/dev")
    public Response getConfigFromPackaging(@PathParam("applicationSid") String applicationSid) {
        secure();
        //logger.info("getting configuration options for " + projectName);
       try {
            Rapp rapp = FsProjectStorage.loadRappFromPackaging(applicationSid, workspaceStorage);
            return buildOkResponse(rapp.getConfig());
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        }
    }


    /**
     * Sets bootstrap parameters for the application.
     * @param request
     * @param applicationSid
     * @return
     */
    @POST
    @Path("apps/{applicationSid}/bootstrap")
    public Response setBootstrap(@Context HttpServletRequest request, @PathParam("applicationSid") String applicationSid) {
        secure();
        try {
            String bootstrapInfo;
            bootstrapInfo = IOUtils.toString(request.getInputStream(), Charset.forName("UTF-8"));

            FsProjectStorage.storeBootstrapInfo(bootstrapInfo, applicationSid, workspaceStorage);
            return buildOkResponse();

        } catch (StorageException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
        } catch (IOException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, new RvdException("Error reading from request stream", e));
        }
    }

    @GET
    @Path("apps/{applicationSid}/bootstrap")
    public Response getBootstrap(@PathParam("applicationSid") String applicationSid) {
        secure();
        try {
            if ( ! FsProjectStorage.hasBootstrapInfo(applicationSid, workspaceStorage) )
                return Response.status(Status.NOT_FOUND).build();

            String bootstrapInfo = FsProjectStorage.loadBootstrapInfo(applicationSid, workspaceStorage);
            return Response.ok(bootstrapInfo, MediaType.APPLICATION_JSON).build();
        } catch (StorageException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
