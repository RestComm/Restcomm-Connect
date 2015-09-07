package org.mobicents.servlet.restcomm.rvd.http.resources;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.PathParam;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RasService;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.exceptions.project.ProjectException;
import org.mobicents.servlet.restcomm.rvd.exceptions.ras.InvalidRestcommAppPackage;
import org.mobicents.servlet.restcomm.rvd.exceptions.ras.RestcommAppAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.exceptions.ras.UnsupportedRasApplicationVersion;
import org.mobicents.servlet.restcomm.rvd.http.RestService;
import org.mobicents.servlet.restcomm.rvd.http.RvdResponse;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.RappItem;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.model.packaging.Rapp;
import org.mobicents.servlet.restcomm.rvd.model.packaging.RappConfig;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageEntityNotFound;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("/apps")
public class RasRestService extends RestService {
    static final Logger logger = Logger.getLogger(RasRestService.class.getName());

    @Context
    ServletContext servletContext;
    @Context
    SecurityContext securityContext;
    @Context
    HttpServletRequest request;

    private RvdConfiguration settings;
    private RasService rasService;
    private ProjectService projectService;
    private RvdContext rvdContext;
    private WorkspaceStorage workspaceStorage;
    private ModelMarshaler marshaler;

    @PostConstruct
    void init() {
        rvdContext = new RvdContext(request, servletContext);
        settings = rvdContext.getSettings();
        marshaler = rvdContext.getMarshaler();
        workspaceStorage = new WorkspaceStorage(settings.getWorkspaceBasePath(), marshaler);
        rasService = new RasService(rvdContext, workspaceStorage);
        projectService = new ProjectService(rvdContext,workspaceStorage);
    }

    @GET
    public Response listRapps(@Context HttpServletRequest request) {
        ProjectService projectService = new ProjectService(rvdContext, workspaceStorage);
        try {
            List<String> projectNames = FsProjectStorage.listProjectNames(workspaceStorage);
            List<RappItem> rapps = FsProjectStorage.listRapps(projectNames, workspaceStorage, projectService);
            return buildOkResponse(rapps);
        } catch (StorageException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
        } catch (ProjectException e) {
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
    public Response newRasApp(@Context HttpServletRequest request) {
        logger.info("uploading new ras app");

        BuildService buildService = new BuildService(workspaceStorage);
        String loggedUser = securityContext.getUserPrincipal() == null ? null : securityContext.getUserPrincipal().getName();


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
                        String effectiveProjectName = rasService.importAppToWorkspace(item.openStream(), loggedUser, projectService);
                        ProjectState projectState = FsProjectStorage.loadProject(effectiveProjectName,workspaceStorage);
                        buildService.buildProject(effectiveProjectName, projectState);

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
        } catch ( RestcommAppAlreadyExists e ) {
            logger.warn(e);
            logger.debug(e,e);
            return buildErrorResponse(Status.CONFLICT, RvdResponse.Status.ERROR, e);
        } catch (InvalidRestcommAppPackage e ) {
            logger.error(e.getMessage(), e);
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        } catch (UnsupportedRasApplicationVersion e) {
            logger.warn(e.getMessage());
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        } catch ( Exception e /* TODO - use a more specific  type !!! */) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GET
    @Path("{name}/config")
    public Response getConfig(@PathParam("name") String projectName) {
        logger.info("getting configuration options for " + projectName);

        RappConfig rappConfig;
        // first, try to return the 'Rapp' from the packaging directory
        if ( FsProjectStorage.hasPackagingInfo(projectName, workspaceStorage) ) {
            return getConfigFromPackaging(projectName);
            /*try {
                Rapp rapp = FsProjectStorage.loadRappFromPackaging(projectName, workspaceStorage);
                return buildOkResponse(rapp.getConfig());
            } catch (StorageException e) {
                logger.error(e.getMessage(), e);
                return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
            }*/
        } else {
            try {
                rappConfig = rasService.getRappConfig(projectName);
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
    @Path("{name}")
    public Response getRapp(@PathParam("name") String projectName) throws StorageException {
        logger.info("getting info for " + projectName);
        try {
            Rapp rapp;
            if ( FsProjectStorage.hasPackagingInfo(projectName, workspaceStorage) )
                rapp = FsProjectStorage.loadRappFromPackaging(projectName, workspaceStorage);
            else
                rapp = FsProjectStorage.loadRapp(projectName, workspaceStorage);
            return buildOkResponse(rapp);
        } catch (StorageEntityNotFound e) {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("{name}/config/dev")
    public Response getConfigFromPackaging(@PathParam("name") String projectName) {
        logger.info("getting configuration options for " + projectName);
       try {
            Rapp rapp = FsProjectStorage.loadRappFromPackaging(projectName, workspaceStorage);
            return buildOkResponse(rapp.getConfig());
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
    @Path("{name}/parameters")
    public Response setBootstrap(@Context HttpServletRequest request, @PathParam("name") String projectName) {
        try {
            String bootstrapInfo;
            bootstrapInfo = IOUtils.toString(request.getInputStream());

            FsProjectStorage.storeBootstrapInfo(bootstrapInfo, projectName, workspaceStorage);
            return buildOkResponse();

        } catch (StorageException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, e);
        } catch (IOException e) {
            return buildErrorResponse(Status.OK, RvdResponse.Status.ERROR, new RvdException("Error reading from request stream", e));
        }
    }

    @GET
    @Path("{name}/parameters")
    public Response getBootstrap(@PathParam("name") String projectName) {
        try {
            if ( ! FsProjectStorage.hasBootstrapInfo(projectName, workspaceStorage) )
                return Response.status(Status.NOT_FOUND).build();

            String bootstrapInfo = FsProjectStorage.loadBootstrapInfo(projectName, workspaceStorage);
            return Response.ok(bootstrapInfo, MediaType.APPLICATION_JSON).build();
        } catch (StorageException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }




}
