package org.mobicents.servlet.restcomm.rvd;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

import com.google.gson.Gson;

@Path("/apps/{appname}/controller")
public class RvdController {
    static final Logger logger = Logger.getLogger(RvdController.class.getName());

    @Context
    ServletContext servletContext;
    private RvdSettings rvdSettings;
    private ProjectStorage projectStorage;
    private ProjectService projectService;
    private Gson gson;

    @PostConstruct
    void init() {
        gson = new Gson();
        rvdSettings = new RvdSettings(servletContext);
        projectStorage = new FsProjectStorage(rvdSettings);
        projectService = new ProjectService(projectStorage, servletContext, rvdSettings);
    }

    private Response runInterpreter( String appname, HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams ) {
        try {
            if (!projectService.projectExists(appname))
                return Response.status(Status.NOT_FOUND).build();
        } catch (BadWorkspaceDirectoryStructure e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        String rcmlResponse;
        try {
            String targetParam = requestParams.getFirst("target");
            Interpreter interpreter = new Interpreter(projectStorage, targetParam, appname, httpRequest, requestParams);
            rcmlResponse = interpreter.interpret();
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        logger.debug(rcmlResponse);
        return Response.ok(rcmlResponse, MediaType.APPLICATION_XML).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerGet(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest, @Context UriInfo ui) {
        logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
        MultivaluedMap<String, String> requestParams = ui.getQueryParameters();

        return runInterpreter(appname, httpRequest, requestParams);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerPost(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams) {
        logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
        logger.debug("POST Params: " + requestParams.toString());

        return runInterpreter(appname, httpRequest, requestParams);
    }

}
