package org.mobicents.servlet.restcomm.rvd.http;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.callcontrol.exceptions.CallControlException;
import org.mobicents.servlet.restcomm.rvd.callcontrol.exceptions.RestcommConfigNotFound;
import org.mobicents.servlet.restcomm.rvd.callcontrol.exceptions.RvdErrorParsingRestcommXml;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.ApiServerConfig;
import org.mobicents.servlet.restcomm.rvd.model.CallControlInfo;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.SettingsModel;
import org.mobicents.servlet.restcomm.rvd.storage.FsCallControlInfoStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageEntityNotFound;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;
import org.w3c.dom.Document;

import com.google.gson.Gson;

@Path("apps")
public class RvdController extends RestService {
    static final Logger logger = Logger.getLogger(RvdController.class.getName());

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest request;

    private RvdConfiguration rvdSettings;
    private ProjectStorage projectStorage;
    private ProjectService projectService;
    private Gson gson;
    private RvdContext rvdContext;



    @PostConstruct
    void init() {
        gson = new Gson();
        rvdContext = new RvdContext(request, servletContext);
        rvdSettings = rvdContext.getSettings();
        projectStorage = rvdContext.getProjectStorage();
        projectService = new ProjectService(rvdContext);
    }

    private Response runInterpreter( String appname, HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams ) {
        String rcmlResponse;
        try {
            if (!projectService.projectExists(appname))
                return Response.status(Status.NOT_FOUND).build();

            String targetParam = requestParams.getFirst("target");
            Interpreter interpreter = new Interpreter(rvdContext, targetParam, appname, httpRequest, requestParams);
            rcmlResponse = interpreter.interpret();

        } catch ( RvdException e ) {
            logger.error(e.getMessage(), e);
            rcmlResponse = "<Response><Hangup/></Response>";
        }


        logger.debug(rcmlResponse);
        return Response.ok(rcmlResponse, MediaType.APPLICATION_XML).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listApps(@Context HttpServletRequest request) {
        List<ProjectItem> items;
        try {
            items = projectService.getAvailableProjects(); // there has to be a user in the context. Only logged users are allowed to to run project manager services
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

    @GET
    @Path("{appname}/controller")
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerGet(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest, @Context UriInfo ui) {
        logger.info("Received Restcomm GET request");
        logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
        //logger.info("Using restcommPublicIP: " + rvdSettings.getEffectiveRestcommIp(httpRequest));
        MultivaluedMap<String, String> requestParams = ui.getQueryParameters();

        return runInterpreter(appname, httpRequest, requestParams);
    }

    @POST
    @Path("{appname}/controller")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerPost(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams) {
        logger.info("Received Restcomm POST request");
        logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
        logger.debug("POST Params: " + requestParams.toString());

        return runInterpreter(appname, httpRequest, requestParams);
    }

    @GET
    @Path("{appname}/resources/{filename}")
    public Response getWav(@PathParam("appname") String projectName, @PathParam("filename") String filename ) {
       InputStream wavStream;
        try {
            wavStream = projectStorage.getWav(projectName, filename);
            return Response.ok(wavStream, "audio/x-wav").header("Content-Disposition", "attachment; filename = " + filename).build();
        } catch (WavItemDoesNotExist e) {
            return Response.status(Status.NOT_FOUND).build(); // ordinary error page is returned since this will be consumed either from restcomm or directly from user
        } catch (StorageException e) {
            //return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build(); // ordinary error page is returned since this will be consumed either from restcomm or directly from user
        }
    }

    // **********************************
    // *** Call control functionality ***
    // **********************************

    private String extractRecordingsUrlFromRestcommConfig(File file) throws CallControlException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (file);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/restcomm/runtime-settings/recordings-uri/text()");
            String recordingsUrl = (String) expr.evaluate(doc, XPathConstants.STRING);

            return recordingsUrl;
        } catch (Exception e) {
            throw new CallControlException("Error parsing restcomm config file: " + file.getPath(), e);
        }

    }
    /**
     * Retrieves restcomm.xml dependent information, host ip and port
     */
    private ApiServerConfig getApiServerConfig( String filesystemContextPath) throws CallControlException {
        ApiServerConfig config = new ApiServerConfig();

        // Load restcomm configuration. Only the fields we are interested in. See RestcommXml model class
        String restcommConfigPath = filesystemContextPath + "../restcomm.war/WEB-INF/conf/restcomm.xml";
        File file = new File(restcommConfigPath);
        if ( !file.exists() ) {
            throw new RestcommConfigNotFound("Cannot find restcomm configuration file at: " + restcommConfigPath);
        }
        String recordingsUrl = extractRecordingsUrlFromRestcommConfig(file);

        // Extract the settings we are interested in from the recordings url. We could also any other containing host and port information
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(recordingsUrl);
            config.setHost( uriBuilder.getHost() );
            config.setPort( uriBuilder.getPort() );
            return config;
        } catch (URISyntaxException e) {
            throw new RvdErrorParsingRestcommXml("Error extracting host and port information from recordings-uri in restcomm.xml: " + recordingsUrl);
        }
    }

    @GET
    @Path("{appname}/start")
    public Response executeAction(@PathParam("appname") String projectName ) {        
        
        WorkspaceStorage workspaceStorage = new WorkspaceStorage(rvdSettings.getWorkspaceBasePath(), rvdContext.getMarshaler());

        try {
            // Load configuration from Restcomm
            ApiServerConfig apiServerConfig = getApiServerConfig(servletContext.getRealPath(File.separator));
            logger.info("using restcomm host: " + apiServerConfig.getHost() + " and port: " + apiServerConfig.getPort());
            
            // Load CC info from project
            CallControlInfo info = FsCallControlInfoStorage.loadInfo(projectName, workspaceStorage);
            
            // Load rvd settings
            SettingsModel settingsModel = null;
            if ( workspaceStorage.entityExists(".settings", "") )
                settingsModel = workspaceStorage.loadEntity(".settings", "", SettingsModel.class);
            
            // All required dependencies are in place. proceed with the request
            // ...
            
            return Response.ok().build();
        } catch (CallControlException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageEntityNotFound e) {
            logger.error(e,e);
            return Response.status(Status.NOT_FOUND).build(); // for case when the cc file does not exist
        }
        catch (StorageException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }


}
