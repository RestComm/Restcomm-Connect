package org.mobicents.servlet.restcomm.rvd.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.PathParam;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.packaging.exception.PackagingDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappBinaryInfo;
import org.mobicents.servlet.restcomm.rvd.packaging.model.RappConfig;
import org.mobicents.servlet.restcomm.rvd.project.RvdProject;
import org.mobicents.servlet.restcomm.rvd.ras.RappItem;
import org.mobicents.servlet.restcomm.rvd.ras.RasService;
import org.mobicents.servlet.restcomm.rvd.storage.FsPackagingStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.FsRasStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.validation.exceptions.RvdValidationException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("ras")
public class RasRestService extends RestService {
    static final Logger logger = Logger.getLogger(RasRestService.class.getName());

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest request;

    private RvdSettings settings;
    private ProjectStorage projectStorage;
    private FsRasStorage rasStorage;
    private FsPackagingStorage packagingStorage;
    private RasService rasService;
    private ProjectService projectService;
    private RvdContext rvdContext;

    @PostConstruct
    void init() {
        rvdContext = new RvdContext(request, servletContext);
        settings = rvdContext.getSettings();
        projectStorage = rvdContext.getProjectStorage();
        rasStorage = new FsRasStorage(rvdContext.getStorageBase());
        packagingStorage = new FsPackagingStorage(rvdContext.getStorageBase());

        rasService = new RasService(rvdContext);
        projectService = new ProjectService(rvdContext);
    }


    /**
     * Returns application package information. If there is no packaging data
     * for this project yet it returns 404/NOT_FOUND. If the project does not even
     * exist it returns 500/INTERNAL_SERVER_ERROR
     * @param projectName
     * @return
     */
    @GET
    @Path("/packaging/app")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppConfig(@QueryParam("name") String projectName) throws StorageException, ProjectDoesNotExist {
        logger.debug("retrieving app package for project " + projectName);

        //try {

            if (! packagingStorage.hasPackaging(projectName) )
                return buildErrorResponse(Status.NOT_FOUND, RvdResponse.Status.OK, null);

            Rapp rapp = rasService.getApp(projectName);
            Gson gson = new Gson();

            return Response.ok().entity(gson.toJson(rapp)).build();

        //} catch (StorageException e) {
        //    logger.error(e, e);
        //    return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        //} catch (RvdException e){
        //    return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        //}
    }


    /**
     * Creates or updates an app
     * @param request
     * @param projectName
     * @return
     */
    @POST
    @Path("/packaging/app/save")
    public Response saveApp(@Context HttpServletRequest request, @QueryParam("name") String projectName) {
        logger.info("saving restcomm app '" + projectName + "'");
        try {
            String rappData;
            rappData = IOUtils.toString(request.getInputStream());

            Gson gson = new Gson();
            Rapp rapp = gson.fromJson(rappData, Rapp.class);
            if ( !packagingStorage.hasPackaging(projectName) ) {
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
            return buildInvalidResponce(Status.OK, RvdResponse.Status.INVALID, e.getReport());
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
    public Response preparePackage(@QueryParam("name") String projectName) {
        logger.debug("preparig app zip for project " + projectName);

        try {
            if (packagingStorage.hasPackaging(projectName) ) {
                RvdProject project = projectService.load(projectName);
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
     * @param projectName
     * @return
     */
    @GET
    @Path("/packaging/binary/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBinaryStatus(@QueryParam("name") String projectName) {
        logger.debug("getting binary info for project " + projectName);

        RappBinaryInfo binaryInfo = rasService.getBinaryInfo(projectName);
        return buildOkResponse(binaryInfo);
    }

    @GET
    @Path("/packaging/download")
    public Response downloadPackage(@QueryParam("name") String projectName) {
        logger.debug("downloading app zip for project " + projectName);

        try {
            if (packagingStorage.hasPackaging(projectName) ) {
                //Validator validator = new RappConfigValidator();
                InputStream zipStream = packagingStorage.getRappBinary(projectName);
                return Response.ok(zipStream, "application/zip").header("Content-Disposition", "attachment; filename = rapp.zip").build();
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

    @GET
    @Path("apps")
    public Response listRapps(@Context HttpServletRequest request) {
        try {
            List<String> projectNames = projectStorage.listProjectNames();
            List<RappItem> rapps = rasStorage.listRapps(projectNames);
            return buildOkResponse(rapps);
        } catch (StorageException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
        }

    }

    /**
     * Create a new application by uploading a ras package
     * @param projectNameOverride - NOT IMPLEMENTED - if specified, the project should be named like this. Otherwise a best effort is made so
     * that the project is named according to the the package content
     * @param request
     * @return
     */
    @POST
    @Path("apps/{name}")
    public Response newRasApp(@PathParam("name") String projectNameOverride, @Context HttpServletRequest request) {
        logger.info("uploading new ras app");

        BuildService buildService = new BuildService(projectStorage);

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
                        String effectiveProjectName = rasService.importAppToWorkspace(item.openStream());
                        buildService.buildProject(effectiveProjectName);

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
        } catch ( Exception e /* TODO - use a more specific  type !!! */) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GET
    @Path("apps/{name}/config")
    public Response getConfig(@PathParam("name") String projectName) {
        logger.info("getting configuration options for " + projectName);

        RappConfig rappConfig;
        try {
            rappConfig = rasService.getRappConfig(projectName);
            return buildOkResponse(rappConfig);
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        }
    }

    /**
     * Sets bootstrap parameters for the application.
     * @param request
     * @param projectName
     * @return
     */
    @POST
    @Path("apps/{name}/bootstrap")
    public Response setBootstrap(@Context HttpServletRequest request, @PathParam("name") String projectName) {
        logger.info("saving bootstrap parameters for app '" + projectName + "'");
        try {
            String bootstrapInfo;
            bootstrapInfo = IOUtils.toString(request.getInputStream());

            rasStorage.storeBootstrapInfo(bootstrapInfo, projectName);
            return buildOkResponse();

        } catch (StorageException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
        } catch (IOException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, new RvdException("Error reading from request stream", e));
        }
    }




}
