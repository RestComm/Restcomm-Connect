package org.mobicents.servlet.restcomm.rvd.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.exceptions.IncompatibleProjectVersion;
import org.mobicents.servlet.restcomm.rvd.exceptions.InvalidServiceParameters;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.packaging.exception.PackagingDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.project.RvdProject;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadProjectHeader;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.upgrade.UpgradeService;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.UpgradeException;
import org.mobicents.servlet.restcomm.rvd.validation.RappConfigValidator;
import org.mobicents.servlet.restcomm.rvd.validation.Validator;
import org.mobicents.servlet.restcomm.rvd.validation.exceptions.ValidationException;
import org.mobicents.servlet.restcomm.rvd.validation.exceptions.ValidationFrameworkException;

@Path("/manager/projects")
public class RvdManager extends UploadRestService {

    static final Logger logger = Logger.getLogger(RvdManager.class.getName());

    @Context
    ServletContext servletContext;
    private ProjectService projectService;

    private RvdSettings rvdSettings;
    private ProjectStorage projectStorage;

    @PostConstruct
    void init() {
        rvdSettings = RvdSettings.getInstance(servletContext);
        projectStorage = new FsProjectStorage(rvdSettings);
        projectService = new ProjectService(projectStorage, servletContext, rvdSettings);
    }

    Response buildErrorResponse(Response.Status httpStatus, RvdResponse.Status rvdStatus, RvdException exception) {
        RvdResponse rvdResponse = new RvdResponse(rvdStatus).setException(exception);
        return Response.status(httpStatus).entity(rvdResponse.asJson()).build();
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProjects(@Context HttpServletRequest request) {

        List<ProjectItem> items;
        try {
            items = projectService.getAvailableProjects();
            ProjectService.fillStartUrlsForProjects(items, request);

        } catch (BadWorkspaceDirectoryStructure e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
    }



    @PUT
    public Response createProject(@QueryParam("name") String name, @QueryParam("kind") String kind) {

        // TODO IMPORTANT!!! sanitize the project name!!

        try {
            projectService.createProject(name, kind);
        } catch (ProjectDirectoryAlreadyExists e) {
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

    /**
     * Retrieves project header information. Returns  the project header or null if it does not exist (for old projects) as JSON - OK status.
     * Returns INTERNAL_SERVER_ERROR status and no response body for serious errors
     * @param name - The project name to get information for
     */
    @GET
    @Path("info")
    public Response projectInfo(@QueryParam("name") String name) {
        StateHeader header = null;
        try {
            header = projectStorage.loadStateHeader(name);
        } catch ( BadProjectHeader e ) {
            logger.warn(e.getMessage());
        } catch (StorageException e) {
            logger.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        Gson gson = new Gson();
        return Response.status(Status.OK).entity(gson.toJson(header)).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    public Response updateProject(@Context HttpServletRequest request, @QueryParam("name") String projectName) {

        // TODO IMPORTANT!!! sanitize the project name!!

        if (projectName != null && !projectName.equals("")) {
            logger.info("savingProject " + projectName);
            try {
                projectService.updateProject(request, projectName);
                return Response.ok().build();
            } catch (StorageException e) {
                logger.error(e.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (ValidationFrameworkException e) {
                logger.error(e.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (ValidationException e) {
                Gson gson = new Gson();
                return Response.ok(gson.toJson(e.getValidationResult()), MediaType.APPLICATION_JSON).build();
            } catch (IncompatibleProjectVersion e) {
                logger.error(e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.asJson()).type(MediaType.APPLICATION_JSON).build();
            }
        } else {
            logger.warn("Empty project name specified for updating");
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    @PUT
    @Path("/rename")
    public Response renameProject(@QueryParam("name") String projectName, @QueryParam("newName") String projectNewName) {

        // TODO IMPORTANT!!! sanitize the project name!!
        if ( !RvdUtils.isEmpty(projectName) && ! RvdUtils.isEmpty(projectNewName) ) {
            try {
                projectService.renameProject(projectName, projectNewName);
                return Response.ok().build();
            } catch (ProjectDoesNotExist e) {
                logger.error(e.getMessage(), e);
                return Response.status(Status.NOT_FOUND).build();
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
    @Path("/upgrade")
    public Response upgradeProject(@QueryParam("name") String projectName) {

        // TODO IMPORTANT!!! sanitize the project name!!
        if ( !RvdUtils.isEmpty(projectName) ) {
            try {
                UpgradeService upgradeService = new UpgradeService(projectStorage);
                upgradeService.upgradeProject(projectName);
                logger.info("project '" + projectName + "' upgraded to version " + RvdSettings.getRvdProjectVersion() );
                // re-build project
                BuildService buildService = new BuildService(projectStorage);
                buildService.buildProject(projectName);
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

    @DELETE
    @Path("/delete")
    public Response deleteProject(@QueryParam("name") String projectName) {

        // TODO IMPORTANT!!! sanitize the project name!!
        if ( ! RvdUtils.isEmpty(projectName) ) {
            try {
                projectService.deleteProject(projectName);
                return Response.ok().build();
            } catch (ProjectDoesNotExist e) {
                logger.error(e.getMessage(), e);
                return Response.status(Status.NOT_FOUND).build();
            } catch (StorageException e) {
                logger.error("Error deleting project '" + projectName + "'", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response openProject(@QueryParam("name") String name, @Context HttpServletRequest request) {

        // TODO CAUTION!!! sanitize name
        // ...

        try {
            String projectState = projectService.openProject(name);
            return Response.ok().entity(projectState).build();
        } catch (StorageException e) {
            logger.error("Error loading project '" + name + "'", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (ProjectDoesNotExist e) {
            return Response.status(Status.NOT_FOUND).entity(e.asJson()).type(MediaType.APPLICATION_JSON).build();
        } catch (IncompatibleProjectVersion e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.asJson()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/uploadwav")
    public Response uploadWavFile(@QueryParam("name") String projectName, @Context HttpServletRequest request) {
        logger.info("running /uploadwav");

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
    @Path("/removewav")
    public Response removeWavFile(@QueryParam("name") String projectName, @QueryParam("filename") String wavname, @Context HttpServletRequest request) {
        // !!! Sanitize project name

        try {
            projectService.removeWavFromProject(projectName, wavname);
            return Response.ok().build();
        } catch (WavItemDoesNotExist e) {
            logger.warn( "Cannot delete " + wavname + " from " + projectName + " app" );
            return Response.status(Status.NOT_FOUND).build();
        }
    }


    @GET
    @Path("/wavlist")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listWavs(@QueryParam("name") String name) {
        List<WavItem> items;
        try {
            if (!projectService.projectExists(name))
                return Response.status(Status.NOT_FOUND).build();
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
    @Path("/build")
    public Response buildProject(@QueryParam("name") String name) {

        // !!! SANITIZE project name

        ProjectStorage projectStorage = new FsProjectStorage(rvdSettings);
        BuildService buildService = new BuildService(projectStorage);

        try {
            buildService.buildProject(name);
            return Response.ok().build();
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @POST
    @Path("/package/config")
    public Response saveAppConfig(@Context HttpServletRequest request, @QueryParam("name") String projectName) {
        logger.info("saving app configuration for project " + projectName);
        try {
            try {
                projectService.saveRappConfig(request.getInputStream(), projectName);
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
            if (! projectStorage.hasRappConfig(projectName) )
                return buildErrorResponse(Status.NOT_FOUND, RvdResponse.Status.OK, null);
               //return Response.status(Status.NOT_FOUND).build();

            String appConfig;
            appConfig = projectStorage.loadRappConfig(projectName);
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
            if (projectStorage.hasRappConfig(projectName) ) {
                Validator validator = new RappConfigValidator();
                //validator.validate(json)
                RvdProject project = projectService.load(projectName);
                projectService.createZipPackage(project);
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
            if (projectStorage.hasRappConfig(projectName) ) {
                Validator validator = new RappConfigValidator();
                InputStream zipStream = projectStorage.getAppPackage(projectName);
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

}
