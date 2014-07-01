package org.mobicents.servlet.restcomm.rvd.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.Principal;
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

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.exceptions.IncompatibleProjectVersion;
import org.mobicents.servlet.restcomm.rvd.exceptions.InvalidServiceParameters;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.jsonvalidation.exceptions.ValidationException;
import org.mobicents.servlet.restcomm.rvd.jsonvalidation.exceptions.ValidationFrameworkException;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.model.client.WavItem;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.upgrade.UpgradeService;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.UpgradeException;


@Path("projects")
public class RvdManager extends RestService {

    static final Logger logger = Logger.getLogger(RvdManager.class.getName());

    //@Resource
    //TicketRepository ticketRepository;

    @Context
    ServletContext servletContext;
    @Context
    SecurityContext securityContext;
    @Context
    HttpServletRequest request;

    private ProjectService projectService;
    private RvdSettings rvdSettings;
    private ProjectStorage projectStorage;
    private ProjectState activeProject;
    private ModelMarshaler marshaler;

    RvdContext rvdContext;

    @PostConstruct
    void init() {
        logger.debug("inside init()");
        rvdContext = new RvdContext(request, servletContext);
        rvdSettings = rvdContext.getSettings();
        marshaler = rvdContext.getMarshaler();
        projectStorage = rvdContext.getProjectStorage();
        projectService = new ProjectService(rvdContext);
    }

    /**
     * Make sure the specified project has been loaded and is available for use. Checks logged user too.
     * Also the loaded project is placed in the activeProject variable
     * @param projectName
     * @return
     * @throws StorageException, WebApplicationException/unauthorized
     * @throws ProjectDoesNotExist
     */
    void assertProjectAvailable(String projectName) throws StorageException, ProjectDoesNotExist {
        if (! projectStorage.projectExists(projectName))
            throw new ProjectDoesNotExist("Project " + projectName + " does not exist");
        ProjectState project = projectStorage.loadProject(projectName);
        if ( project.getHeader().getOwner() != null ) {
            // needs further checking
            if ( securityContext.getUserPrincipal() != null ) {
                String loggedUser = securityContext.getUserPrincipal().getName();
                if ( loggedUser.equals(project.getHeader().getOwner() ) ) {
                    this.activeProject = project;
                    return;
                }
            }
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        activeProject = project;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProjects(@Context HttpServletRequest request) {
        logger.debug("SecurityContext: " + securityContext);
        logger.debug("User principal: " + securityContext.getUserPrincipal());
        logger.debug("isSecure: " + securityContext.isSecure());

        Principal loggedUser = securityContext.getUserPrincipal();
        List<ProjectItem> items;
        try {
            items = projectService.getAvailableProjectsByOwner(loggedUser.getName()); // there has to be a user in the context. Only logged users are allowed to to run project manager services
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
        Principal loggedUser = securityContext.getUserPrincipal();

        try {
            projectService.createProject(name, kind, loggedUser.getName());
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
     * @throws StorageException
     * @throws ProjectDoesNotExist
     */
    @GET
    @Path("{name}/info")
    public Response projectInfo(@PathParam("name") String name) throws StorageException, ProjectDoesNotExist {
        assertProjectAvailable(name);

        StateHeader header = activeProject.getHeader();
        return Response.status(Status.OK).entity(marshaler.getGson().toJson(header)).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("{name}")
    public Response updateProject(@Context HttpServletRequest request, @PathParam("name") String projectName) {
        if (projectName != null && !projectName.equals("")) {
            logger.info("savingProject " + projectName);
            try {
                ProjectState existingProject = projectStorage.loadProject(projectName);
                Principal loggedUser = securityContext.getUserPrincipal();
                if (loggedUser.getName().equals(existingProject.getHeader().getOwner())  ||  existingProject.getHeader().getOwner() == null ) {
                    projectService.updateProject(request, projectName, existingProject);
                    return Response.ok().build();
                } else {
                    throw new WebApplicationException(Response.Status.UNAUTHORIZED);
                }
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
    @Path("{name}/rename")
    public Response renameProject(@PathParam("name") String projectName, @QueryParam("newName") String projectNewName) throws StorageException, ProjectDoesNotExist {
        if ( !RvdUtils.isEmpty(projectName) && ! RvdUtils.isEmpty(projectNewName) ) {
            assertProjectAvailable(projectName);
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
    @Path("{name}")
    public Response deleteProject(@PathParam("name") String projectName) throws ProjectDoesNotExist {
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

    @GET
    @Path("{name}/archive")
    public Response downloadArchive(@PathParam("name") String projectName) throws StorageException, ProjectDoesNotExist {
        logger.debug("downloading raw archive for project " + projectName);
        assertProjectAvailable(projectName);

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
    public Response openProject(@PathParam("name") String name, @Context HttpServletRequest request) throws StorageException, ProjectDoesNotExist {
        assertProjectAvailable(name);
        return Response.ok().entity(marshaler.toData(activeProject)).build();
        /*
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
        */
    }

    @POST
    @Path("{name}/wavs")
    public Response uploadWavFile(@PathParam("name") String projectName, @Context HttpServletRequest request) throws StorageException, ProjectDoesNotExist {
        logger.info("running /uploadwav");
        assertProjectAvailable(projectName);
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
    public Response removeWavFile(@PathParam("name") String projectName, @QueryParam("filename") String wavname, @Context HttpServletRequest request) throws StorageException, ProjectDoesNotExist {
        assertProjectAvailable(projectName);
        try {
            projectService.removeWavFromProject(projectName, wavname);
            return Response.ok().build();
        } catch (WavItemDoesNotExist e) {
            logger.warn( "Cannot delete " + wavname + " from " + projectName + " app" );
            return Response.status(Status.NOT_FOUND).build();
        }
    }


    @GET
    @Path("{name}/wavs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listWavs(@PathParam("name") String name) throws StorageException, ProjectDoesNotExist {
        assertProjectAvailable(name);
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
    @Path("{name}/build")
    public Response buildProject(@PathParam("name") String name) throws StorageException, ProjectDoesNotExist {
        assertProjectAvailable(name);
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
