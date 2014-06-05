package org.mobicents.servlet.restcomm.rvd.http;

import java.io.IOException;
import java.io.InputStream;

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

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.packaging.exception.PackagingDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.project.RvdProject;
import org.mobicents.servlet.restcomm.rvd.ras.RasService;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.validation.RappConfigValidator;
import org.mobicents.servlet.restcomm.rvd.validation.Validator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("/ras/app/")
public class RasRestService extends UploadRestService {
    static final Logger logger = Logger.getLogger(RasRestService.class.getName());

    @Context
    ServletContext servletContext;
    private RvdSettings settings;
    private ProjectStorage storage;
    private RasService rasService;
    private ProjectService projectService;

    @PostConstruct
    void init() {
        settings = RvdSettings.getInstance(servletContext);
        storage = new FsProjectStorage(settings);
        rasService = new RasService(storage);
        projectService = new ProjectService(storage, servletContext, settings);
    }

    
    @POST
    @Path("/package/config")
    public Response saveAppConfig(@Context HttpServletRequest request, @QueryParam("name") String projectName) {
        logger.info("saving app configuration for project " + projectName);
        try {
            try {
                rasService.saveRappConfig(request.getInputStream(), projectName);
                logger.info("saved app configuration for project " + projectName);
                return Response.ok().build();
            } catch (IOException e) {
                throw new RvdException("Error saving app configuration", e);
            }
        } catch (RvdException e) {
            logger.error(e, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.asJson()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Returns application configuration information. If there is no packaging data
     * for this project yet it returns 404/NOT_FOUND. If the project does not even
     * exist it returns 500/INTERNAL_SERVER_ERROR
     * @param projectName
     * @return
     */
    @GET
    @Path("/package/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppConfig(@QueryParam("name") String projectName) {
        logger.debug("retrieving app package for project " + projectName);

        try {
            if (! storage.hasRappConfig(projectName) )
                return buildErrorResponse(Status.NOT_FOUND, RvdResponse.Status.OK, null);
               //return Response.status(Status.NOT_FOUND).build();

            String appConfig;
            appConfig = storage.loadRappConfig(projectName);
            return Response.ok().entity(appConfig).build();
        } catch (StorageException e) {
            logger.error(e, e);
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        } catch (RvdException e){
            return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
        }
    }

    @GET
    @Path("/package/prepare")
    @Produces(MediaType.APPLICATION_JSON)
    public Response preparePackage(@QueryParam("name") String projectName) {
        logger.debug("preparig app zip for project " + projectName);

        try {
            if (storage.hasRappConfig(projectName) ) {
                Validator validator = new RappConfigValidator();
                //validator.validate(json)
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

    @GET
    @Path("/package/download")
    public Response downloadPackage(@QueryParam("name") String projectName) {
        logger.debug("downloading app zip for project " + projectName);

        try {
            if (storage.hasRappConfig(projectName) ) {
                Validator validator = new RappConfigValidator();
                InputStream zipStream = storage.getAppPackage(projectName);
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
    
    /**
     * Create a new application by uploading a ras package
     * @param projectName
     * @param request
     * @return
     */
    @POST
    @Path("/new")
    public Response newRasApp(@QueryParam("name") String projectName, @Context HttpServletRequest request) {
        logger.info("uploading new ras app");

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
                        rasService.importAppToWorkspace(item.openStream());
                        fileinfo.addProperty("name", item.getName());
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


}
