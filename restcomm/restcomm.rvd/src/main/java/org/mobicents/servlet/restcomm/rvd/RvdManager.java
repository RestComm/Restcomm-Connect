package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

import org.mobicents.servlet.restcomm.rvd.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDirectoryAlreadyExists;
import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.model.client.ActiveProjectInfo;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.WavFileItem;

@Path("/manager/projects")
public class RvdManager {

    private static final String projectSessionAttribute = "project"; // the name of the session variable where the active
                                                                     // project will be stored

    @Context
    ServletContext servletContext;
    private ProjectService projectService;

    @PostConstruct
    void init() {
        projectService = new ProjectService(servletContext);
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
            e.printStackTrace(); // TODO remove this and log the error
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (URISyntaxException e) {
            e.printStackTrace(); // probably caused by a bad project name
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/wavlist")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listWavs(@QueryParam("name") String name) {

        List<WavFileItem> items;
        try {
            if (!projectService.projectExists(name))
                return Response.status(Status.NOT_FOUND).build();
            items = projectService.getWavs(name);
        } catch (BadWorkspaceDirectoryStructure e) {
            e.printStackTrace(); // TODO remove this and log the error
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        Gson gson = new Gson();
        return Response.ok(gson.toJson(items), MediaType.APPLICATION_JSON).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProject(@QueryParam("name") String name) {

        // TODO IMPORTANT!!! sanitize the project name!!

        try {
            projectService.createProject(name);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (ProjectDirectoryAlreadyExists e) {
            e.printStackTrace();
            return Response.status(Status.CONFLICT).build();
        }

        return Response.ok().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProject(@Context HttpServletRequest request, @QueryParam("name") String projectName) {

        // TODO IMPORTANT!!! sanitize the project name!!

        if (projectName != null && !projectName.equals("")) {
            System.out.println("savingProject " + projectName);
            if (projectService.updateProject(request, projectName))
                return Response.ok().build();
            else
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @PUT
    @Path("/rename")
    public Response renameProject(@QueryParam("name") String projectName, @QueryParam("newName") String projectNewName) {

        // TODO IMPORTANT!!! sanitize the project name!!
        if ((projectName != null && !projectName.equals("")) && (projectNewName != null && !projectNewName.equals(""))) {
            try {
                if (projectService.renameProject(projectName, projectNewName))
                    return Response.ok().build();
                else
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (ProjectDoesNotExist e) {
                e.printStackTrace();
                return Response.status(Status.NOT_FOUND).build();
            } catch (ProjectDirectoryAlreadyExists e) {
                e.printStackTrace();
                return Response.status(Status.CONFLICT).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @DELETE
    @Path("/delete")
    public Response deleteProject(@QueryParam("name") String projectName) {

        // TODO IMPORTANT!!! sanitize the project name!!
        if (projectName != null && !projectName.equals("")) {
            try {
                if (projectService.deleteProject(projectName))
                    return Response.ok().build();
                else
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } catch (ProjectDoesNotExist e) {
                e.printStackTrace();
                return Response.status(Status.NOT_FOUND).build();
            }
        } else
            return Response.status(Status.BAD_REQUEST).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response openProject(@QueryParam("name") String name, @Context HttpServletRequest request) {

        // TODO CAUTION!!! sanitize name
        // ...

        String workspaceBasePath = projectService.getWorkspaceBasePath();
        File stateFile = new File(workspaceBasePath + File.separator + name + File.separator + "state");
        try {
            FileInputStream stateFileStream = new FileInputStream(stateFile);
            request.getSession().setAttribute(projectSessionAttribute, name); // mark the open project in the session
            return Response.ok().entity(stateFileStream).build();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return Response.status(Status.BAD_REQUEST).build(); // TODO This is not the correct return code for all cases of error
    }

    @GET
    @Path("/active")
    public Response getActiveProject(@Context HttpServletRequest request) {
        String name = (String) request.getSession().getAttribute(projectSessionAttribute);
        if (name != null && !name.equals("")) {
            ActiveProjectInfo projectInfo = new ActiveProjectInfo();
            projectInfo.setName(name);

            Gson gson = new Gson(); // TODO - maybe inject this and create it all the time. See
                                    // https://bitbucket.org/telestax/telscale-restcomm/src/dec355993594e902f3155324f49b57ee76727548/restcomm/restcomm.http/src/main/java/org/mobicents/servlet/restcomm/http/IncomingPhoneNumbersEndpoint.java?at=ts713#cl-242
            return Response.ok(gson.toJson(projectInfo), MediaType.APPLICATION_JSON).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    @GET
    @Path("/close")
    public Response closeActiveProject(@Context HttpServletRequest request) {
        if (request.getSession().getAttribute(projectSessionAttribute) != null)
            request.getSession().removeAttribute(projectSessionAttribute);

        return Response.ok().build();
    }

    @POST
    @Path("/build")
    public Response buildProject(@Context HttpServletRequest request) {
        String name = (String) request.getSession().getAttribute(projectSessionAttribute);
        if (name != null && !name.equals("")) {

            String workspaceBasePath = projectService.getWorkspaceBasePath();
            File projectDir = new File(workspaceBasePath + File.separator + name);

            if (projectDir.exists()) {

                String projectPath = workspaceBasePath + File.separator + name + File.separator;
                File dataDir = new File(projectPath + "data");

                // delete all files in directory
                for (File anyfile : dataDir.listFiles()) {
                    anyfile.delete();
                }

                // and now process state
                try {

                    String state_json = FileUtils.readFileToString(new File(projectPath + "state"), "UTF-8");
                    System.out.println("state: " + state_json);
                    BuildService buildService = new BuildService();
                    buildService.buildProject(state_json, projectPath);

                    return Response.ok().build();

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Status.NOT_FOUND).build();
    }

}
