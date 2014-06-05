package org.mobicents.servlet.restcomm.rvd.http;

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
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;

import com.google.gson.Gson;

@Path("/apps/{appname}/controller")
public class RvdController extends RestService {
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
        rvdSettings = RvdSettings.getInstance(servletContext);
        projectStorage = new FsProjectStorage(rvdSettings);
        projectService = new ProjectService(projectStorage, servletContext, rvdSettings);
    }

    private Response runInterpreter( String appname, HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams ) {
        String rcmlResponse;
        try {
            if (!projectService.projectExists(appname))
                return Response.status(Status.NOT_FOUND).build();

            String targetParam = requestParams.getFirst("target");
            Interpreter interpreter = new Interpreter(rvdSettings, projectStorage, targetParam, appname, httpRequest, requestParams);
            rcmlResponse = interpreter.interpret();

        } catch ( RvdException e ) {
            logger.error(e.getMessage(), e);
            rcmlResponse = "<Response><Hangup/></Response>";
        }


        logger.debug(rcmlResponse);
        return Response.ok(rcmlResponse, MediaType.APPLICATION_XML).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerGet(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest, @Context UriInfo ui) {
        logger.info("Received Restcomm GET request");
        logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
        MultivaluedMap<String, String> requestParams = ui.getQueryParameters();

        return runInterpreter(appname, httpRequest, requestParams);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerPost(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams) {
        logger.info("Received Restcomm POST request");
        logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
        logger.debug("POST Params: " + requestParams.toString());

        return runInterpreter(appname, httpRequest, requestParams);
    }

}
