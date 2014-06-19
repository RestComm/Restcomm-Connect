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
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.PathParam;

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
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.exceptions.IncompatibleProjectVersion;
import org.mobicents.servlet.restcomm.rvd.exceptions.InvalidServiceParameters;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.jsonvalidation.exceptions.ValidationException;
import org.mobicents.servlet.restcomm.rvd.jsonvalidation.exceptions.ValidationFrameworkException;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.security.Ticket;
import org.mobicents.servlet.restcomm.rvd.security.TicketRepository;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadProjectHeader;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.upgrade.UpgradeService;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.UpgradeException;


//@Path("/manager/projects")
@Path("projects")
public class RvdManager extends UploadRestService {

    static final Logger logger = Logger.getLogger(RvdManager.class.getName());

    //@Resource
    //TicketRepository ticketRepository;


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

    @GET
    @Path("login")
    public Response login() {
        logger.debug("Running login");

        // get username/password from request and authenticate against Restcomm
        // ...
        String userId = "otsakir@gmail.com";
        //String password = "pass";

        // if authentication succeeds create a ticket for this user and return its id
        TicketRepository tickets = TicketRepository.getInstance();
        Ticket ticket = new Ticket(userId);
        tickets.putTicket( ticket );

        return Response.ok().cookie( new NewCookie(RvdSettings.TICKET_COOKIE_NAME, ticket.getTicketId() )).build();
    }

    @GET
    //@Path("list")
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
    @Path("{name}")
    public Response createProject(@PathParam("name") String name, @QueryParam("kind") String kind) {

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
    //@Path("info")
    @Path("{name}/info")
    public Response projectInfo(@PathParam("name") String name) {
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
    @Path("{name}")
    public Response updateProject(@Context HttpServletRequest request, @PathParam("name") String projectName) {

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
    //@Path("rename")
    @Path("{name}/rename")
    public Response renameProject(@PathParam("name") String projectName, @QueryParam("newName") String projectNewName) {

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
    //@Path("upgrade")
    @Path("{name}/upgrade")
    public Response upgradeProject(@PathParam("name") String projectName) {

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
    //@Path("delete")
    @Path("{name}")
    public Response deleteProject(@PathParam("name") String projectName) {

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
    //@Path("archive")
    @Path("{name}/archive")
    public Response downloadArchive(@PathParam("name") String projectName) {
        logger.debug("downloading raw archive for project " + projectName);

        InputStream archiveStream;
        try {
            archiveStream = projectService.archiveProject(projectName);
            return Response.ok(archiveStream, "application/zip").header("Content-Disposition", "attachment; filename = " + projectName + ".zip").build();
        } catch (StorageException e) {
            logger.error(e,e);
            return null;
        }
    }

    @POST
    //@Path("archive")
    @Path("{name}/archive")
    public Response importProjectArchive(@PathParam("name") String projectName, @Context HttpServletRequest request) {
        logger.debug("importing project from raw archive: " + projectName);

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
        } catch ( Exception e /* TODO - use a more specific  type !!! */) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response openProject(@PathParam("name") String name, @Context HttpServletRequest request) {

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
    //@Path("/uploadwav")
    @Path("{name}/wavs")
    public Response uploadWavFile(@PathParam("name") String projectName, @Context HttpServletRequest request) {
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
    //@Path("/removewav")
    @Path("{name}/wavs")
    public Response removeWavFile(@PathParam("name") String projectName, @QueryParam("filename") String wavname, @Context HttpServletRequest request) {
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
    //@Path("/wavlist")
    @Path("{name}/wavs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listWavs(@PathParam("name") String name) {
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
    @Path("{name}/build")
    //@Path("/build")
    public Response buildProject(@PathParam("name") String name) {

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
}
