package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mobicents.servlet.restcomm.rvd.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.BadExternalServiceResponse;

import com.google.gson.Gson;

@Path("/apps/{appname}/controller")
public class RvdController {

    // configuration parameters
    // private static final String workspaceDirectoryName = "workspace";
    // private static final String protoDirectoryName = "_proto"; // the prototype project directory name

    //private String workspaceBasePath;

    @Context
    ServletContext servletContext;
    private ProjectService projectService; // use a proper way to initialize this in init()
    private Gson gson;

    @PostConstruct
    void init() {
        // workspaceBasePath = servletContext.getRealPath(File.separator) + workspaceDirectoryName;
        gson = new Gson();
        projectService = new ProjectService(servletContext);
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerGet(@PathParam("appname") String appname, @QueryParam("target") String targetParam,
            @Context HttpServletRequest httpRequest) {

        try {
            if (!projectService.projectExists(appname))
                return Response.status(Status.NOT_FOUND).build();
        } catch (BadWorkspaceDirectoryStructure e) {
            e.printStackTrace(); // TODO Auto-generated catch block
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        String projectBasePath = projectService.getWorkspaceBasePath() + File.separator + appname;
        Interpreter interpreter = new Interpreter();

        String rcmlResponse;
        try {
            rcmlResponse = interpreter.interpret(targetParam, projectBasePath, appname, httpRequest);
        } catch (BadExternalServiceResponse e) {
            System.out.println( "[ERROR] " + "BadExternalServiceResponse");
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (InterpreterException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        System.out.println(rcmlResponse);
        return Response.ok(rcmlResponse, MediaType.APPLICATION_XML).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerPost(@PathParam("appname") String appname, @QueryParam("target") String targetParam,
            @Context HttpServletRequest httpRequest) {

        try {
            if (!projectService.projectExists(appname))
                return Response.status(Status.NOT_FOUND).build();
        } catch (BadWorkspaceDirectoryStructure e) {
            e.printStackTrace(); // TODO Auto-generated catch block
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        String projectBasePath = projectService.getWorkspaceBasePath() + File.separator + appname;
        Interpreter interpreter = new Interpreter();

        String rcmlResponse;
        try {
            rcmlResponse = interpreter.interpret(targetParam, projectBasePath, appname, httpRequest);
        } catch (InterpreterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        System.out.println(rcmlResponse);
        return Response.ok(rcmlResponse, MediaType.APPLICATION_XML).build();
    }

}
