package org.mobicents.servlet.restcomm.rvd.http.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.IDToken;
import org.mobicents.servlet.restcomm.rvd.ProjectAwareRvdContext;
import org.mobicents.servlet.restcomm.rvd.ProjectService;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.CallControlBadRequestException;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.CallControlException;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.CallControlInvalidConfigurationException;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.RestcommConfigNotFound;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.RvdErrorParsingRestcommXml;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.UnauthorizedCallControlAccess;
import org.mobicents.servlet.restcomm.rvd.http.RestService;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.RemoteServiceError;
import org.mobicents.servlet.restcomm.rvd.model.ApiServerConfig;
import org.mobicents.servlet.restcomm.rvd.model.CallControlInfo;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.ProjectSettings;
import org.mobicents.servlet.restcomm.rvd.model.callcontrol.CallControlAction;
import org.mobicents.servlet.restcomm.rvd.model.callcontrol.CallControlStatus;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.SettingsModel;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommAccountInfoResponse;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommCreateCallResponse;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommClient;
import org.mobicents.servlet.restcomm.rvd.storage.FsCallControlInfoStorage;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadWorkspaceDirectoryStructure;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageEntityNotFound;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;
import org.w3c.dom.Document;

import com.google.gson.Gson;

// This goes under /services. See more in ControllerApplication.java
@Path("/apps")
public class RvdController extends RestService {
    static final Logger logger = Logger.getLogger(RvdController.class.getName());

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest request;

    private RvdConfiguration rvdSettings;
    private RvdContext rvdContext;

    private WorkspaceStorage workspaceStorage;
    private ModelMarshaler marshaler;


    void init(RvdContext rvdContext) {
        this.rvdContext = rvdContext;
        rvdSettings = rvdContext.getSettings();
        marshaler = rvdContext.getMarshaler();
        workspaceStorage = rvdContext.getWorkspaceStorage();
    }

    private Response runInterpreter( ProjectAwareRvdContext rvdContext, String appname, HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams ) {
        String rcmlResponse;
        try {
            if (!FsProjectStorage.projectExists(appname,workspaceStorage))
                return Response.status(Status.NOT_FOUND).build();

            String targetParam = requestParams.getFirst("target");
            Interpreter interpreter = new Interpreter(rvdContext, targetParam, appname, httpRequest, requestParams, workspaceStorage);
            rcmlResponse = interpreter.interpret();

        } catch (RemoteServiceError e) {
            logger.warn(e.getMessage());
            if ( rvdContext.getProjectSettings().getLogging() )
                rvdContext.getProjectLogger().log(e.getMessage()).tag("app", appname).tag("EXCEPTION").done();
            rcmlResponse = Interpreter.rcmlOnException();
        } catch ( Exception e ) {
            logger.error(e.getMessage(), e);
            if ( rvdContext.getProjectSettings().getLogging() )
                rvdContext.getProjectLogger().log(e.getMessage()).tag("app", appname).tag("EXCEPTION").done();
            rcmlResponse = Interpreter.rcmlOnException();
        }


        logger.debug(rcmlResponse);
        return Response.ok(rcmlResponse, MediaType.APPLICATION_XML).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listApps(@Context HttpServletRequest request) {

        KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        IDToken idToken = session.getIdToken();
        //logger.info("EMAIL: " + idToken.getEmail() );
        //logger.info("NAME: " + idToken.getName() );
        //logger.info("Username: " +  idToken.getPreferredUsername() );

        RvdContext rvdContext = new RvdContext(request, servletContext);
        ProjectService projectService = new ProjectService(rvdContext, workspaceStorage);
        init(rvdContext);
        List<ProjectItem> items;
        try {
            items = ProjectService.getAvailableProjects(workspaceStorage); // there has to be a user in the context. Only logged users are allowed to to run project manager services
            projectService.fillStartUrlsForProjects(items, request);

        } catch (BadWorkspaceDirectoryStructure e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (StorageException e) {
            logger.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (RvdException e) {
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
        ProjectAwareRvdContext rvdContext;
        try {
            rvdContext = new ProjectAwareRvdContext(appname, request, servletContext);

            init(rvdContext);
            logger.info("Received Restcomm GET request");
            Enumeration<String> headerNames = (Enumeration<String>) httpRequest.getHeaderNames();
            while ( headerNames.hasMoreElements() ) {
                String headerName = headerNames.nextElement();
            }

            logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
            MultivaluedMap<String, String> requestParams = ui.getQueryParameters();

            return runInterpreter(rvdContext, appname, httpRequest, requestParams);
        } catch (StorageException e) {
            logger.error(e,e);
            return Response.ok(Interpreter.rcmlOnException(), MediaType.APPLICATION_XML).build();
        }
    }

    @POST
    @Path("{appname}/controller")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerPost(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams) {
        ProjectAwareRvdContext rvdContext;
        try {
            rvdContext = new ProjectAwareRvdContext(appname, request, servletContext);
            init(rvdContext);
            logger.info("Received Restcomm POST request");
            logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
            logger.debug("POST Params: " + requestParams.toString());

            return runInterpreter(rvdContext, appname, httpRequest, requestParams);
        } catch (StorageException e) {
            logger.error(e,e);
            return Response.ok(Interpreter.rcmlOnException(), MediaType.APPLICATION_XML).build();
        }
    }

    @GET
    @Path("{appname}/resources/{filename}")
    public Response getWav(@PathParam("appname") String projectName, @PathParam("filename") String filename ) {
        ProjectAwareRvdContext rvdContext;
        try {
            rvdContext = new ProjectAwareRvdContext(projectName, request, servletContext);
            init(rvdContext);
            InputStream wavStream;

            try {
                wavStream = FsProjectStorage.getWav(projectName, filename, workspaceStorage);
                return Response.ok(wavStream, "audio/x-wav").header("Content-Disposition", "attachment; filename = " + filename).build();
            } catch (WavItemDoesNotExist e) {
                return Response.status(Status.NOT_FOUND).build(); // ordinary error page is returned since this will be consumed either from restcomm or directly from user
            } catch (StorageException e) {
                //return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build(); // ordinary error page is returned since this will be consumed either from restcomm or directly from user
            }
        } catch (StorageException e1) {
            logger.error(e1,e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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

            return recordingsUrl.trim();
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

    /**
     * Runs a query on Restcomm numbers api and tries to match an application named X with its number. If any match is found it returns it
     * @param apiHost
     * @param apiPort
     * @param apiUsername
     * @param apiPort2
     * @param projectName
     * @return
     * @throws StorageException
     * @throws CallControlException
     */
    /*
    private String guessApplicationDID(String apiHost, Integer apiPort, String apiUsername, String accountSid, String projectName) {
        URIBuilder uriBuilder = new URIBuilder().setHost(apiHost).setPort(apiPort).setPath("/restcomm/2012-04-24/Numbers.json")
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGet get = new HttpGet( url );
        get.addHeader("Authorization", "Basic " + RvdUtils.buildHttpAuthorizationToken(esStep.getUsername(), esStep.getPassword()));
        response = client.execute( get );
    }
    */

    private RestcommCreateCallResponse executeAction(String projectName, HttpServletRequest request, String toParam, String fromParam, String accessToken, UriInfo ui) throws StorageException, CallControlException {
        rvdContext = new ProjectAwareRvdContext(projectName, request, servletContext);
        init(rvdContext);

        // Load CC info from project
        CallControlInfo info = FsCallControlInfoStorage.loadInfo(projectName, workspaceStorage);

        // If an access token is present in the project make sure it matches the one in the url
        if (info.accessToken != null ) {
            if ( !info.accessToken.equals(accessToken) )
                throw new UnauthorizedCallControlAccess("Web Trigger token authentication failed for '" + projectName + "'").setRemoteIP(request.getRemoteAddr());
        }

        // Load configuration from Restcomm
        ApiServerConfig apiServerConfig = getApiServerConfig(servletContext.getRealPath(File.separator));
        logger.debug("WebTrigger restcomm client: using restcomm host " + apiServerConfig.getHost() + " and port: " + apiServerConfig.getPort());

        // Load rvd settings
        SettingsModel settingsModel = SettingsModel.createDefault();
        if ( workspaceStorage.entityExists(".settings", "") )
            settingsModel = workspaceStorage.loadEntity(".settings", "", SettingsModel.class);

        // Setup required values depending on existing setup
        String apiHost = settingsModel.getApiServerHost();
        if ( RvdUtils.isEmpty(apiHost) )
            apiHost = apiServerConfig.getHost();

        Integer apiPort = settingsModel.getApiServerRestPort();
        if ( apiPort == null )
            apiPort = apiServerConfig.getPort();

        String apiUsername = settingsModel.getApiServerUsername();

        String apiPassword = settingsModel.getApiServerPass();

        String rcmlUrl = info.lanes.get(0).startPoint.rcmlUrl;
        // try to create a valid URI from it if only the application name has been given
        // ...
        // use the existing application for RCML if none has been given
        if ( RvdUtils.isEmpty(rcmlUrl) ) {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setHost(request.getLocalAddr());
            uriBuilder.setPort(request.getLocalPort());
            uriBuilder.setScheme(request.getScheme());
            uriBuilder.setPath("/restcomm-rvd/services/apps/" + projectName + "/controller");
            try {
                rcmlUrl = uriBuilder.build().toString();
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // Add user supplied params to rcmlUrl
        try {
            URIBuilder uriBuilder = new URIBuilder(rcmlUrl);
            MultivaluedMap<String, String> requestParams = ui.getQueryParameters();
            for ( String paramName : requestParams.keySet() ) {
                if ( "token".equals(paramName) || "from".equals(paramName) || "to".equals(paramName) )
                    continue; // skip parameters that are used by WebTrigger it self
                // also, skip builtin parameters that will be supplied by restcomm when it reaches for the controller
                if ( ! rvdSettings.getRestcommParameterNames().contains(paramName))
                    uriBuilder.addParameter(Interpreter.nameModuleRequestParam(paramName), requestParams.getFirst(paramName));
            }
            rcmlUrl = uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            throw new CallControlException("Error copying user supplied parameters to rcml url", e);
        }


        String to = toParam;
        if ( RvdUtils.isEmpty(to) )
            to = info.lanes.get(0).startPoint.to;

        // set 'from' using url, web trigger conf or default value.
        String from = fromParam;
        if ( RvdUtils.isEmpty(from) )
            from = info.lanes.get(0).startPoint.from;
        if ( RvdUtils.isEmpty(from))
            from = projectName;

        if ( RvdUtils.isEmpty(apiHost) || apiPort == null )
            //return Response.status(Status.INTERNAL_SERVER_ERROR).type(selectedMediaType).build();
            throw new CallControlInvalidConfigurationException("Could not determine restcomm host/port .");
        if ( RvdUtils.isEmpty(rcmlUrl) )
            throw new CallControlInvalidConfigurationException("Could not determine application RCML url.");

        if ( RvdUtils.isEmpty(from) || RvdUtils.isEmpty(to) )
            throw new CallControlBadRequestException("Either <i>from</i> or <i>to</i> value is missing. Make sure they are both passed as query parameters or are defined in the Web Trigger configuration.").setStatusCode(400);

        // Find the account sid for the apiUsername
        RestcommClient client = new RestcommClient(apiHost, apiPort, apiUsername, apiPassword);
        RestcommAccountInfoResponse accountResponse = client.get("/restcomm/2012-04-24/Accounts.json/" + apiUsername).done(marshaler.getGson(), RestcommAccountInfoResponse.class);
        String accountSid = accountResponse.getSid();

        // Create the call
        RestcommCreateCallResponse response = client.post("/restcomm/2012-04-24/Accounts/" + accountSid + "/Calls.json")
            .addParam("From", from)
            .addParam("To", to)
            .addParam("Url", rcmlUrl).done(marshaler.getGson(), RestcommCreateCallResponse.class);

        logger.info("WebTrigger: joined " + to + " with " + rcmlUrl);
        return response;
    }

    @GET
    @Path("{appname}/start{extension: (.html)?}")
    @Produces(MediaType.TEXT_HTML)
    public Response executeActionHtml(@PathParam("appname") String projectName, @Context HttpServletRequest request, @QueryParam("to") String toParam, @QueryParam("from") String fromParam, @QueryParam("token") String accessToken, @Context UriInfo ui ) {
        String selectedMediaType = MediaType.TEXT_HTML;

        ProjectAwareRvdContext rvdContext;
        try {
            RestcommCreateCallResponse createCallResponse = executeAction(projectName, request, toParam, fromParam, accessToken, ui);
            return buildWebTriggerHtmlResponse("Web Trigger", "Create call", "success", "Created call with SID " + createCallResponse.getSid() + " from " + createCallResponse.getFrom() + " to " + createCallResponse.getTo(), 200);
        } catch (UnauthorizedCallControlAccess e) {
            logger.warn("",e);
            return buildWebTriggerHtmlResponse("Web Trigger", "Create call", "failure",  e.getMessage(), 401);
        } catch (CallControlException e) {
            logger.error("",e);
            int httpStatus = 500;
            if (e.getStatusCode() != null)
                httpStatus = e.getStatusCode();
            return buildWebTriggerHtmlResponse("Web Trigger", "Create call", "failure", e.getMessage(), httpStatus);
        } catch (StorageEntityNotFound e) {
            logger.error("",e);
            return Response.status(Status.NOT_FOUND).build(); // for case when the cc file does not exist
        }
        catch (StorageException e) {
            logger.error("",e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(selectedMediaType).build();
        }
    }

    @GET
    @Path("{appname}/start.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeActionJson(@PathParam("appname") String projectName, @Context HttpServletRequest request, @QueryParam("to") String toParam, @QueryParam("from") String fromParam, @QueryParam("token") String accessToken, @Context UriInfo ui ) {
        String selectedMediaType = MediaType.APPLICATION_JSON;

        ProjectAwareRvdContext rvdContext;
        try {
            RestcommCreateCallResponse createCallResponse = executeAction(projectName, request, toParam, fromParam, accessToken, ui);
            return buildWebTriggerJsonResponse(CallControlAction.createCall, CallControlStatus.success, 200, createCallResponse);
        } catch (UnauthorizedCallControlAccess e) {
            logger.warn("",e);
            return buildWebTriggerJsonResponse(CallControlAction.createCall, CallControlStatus.failure, 401, null);
        } catch (CallControlException e) {
            logger.error("",e);
            int httpStatus = 500;
            if (e.getStatusCode() != null)
                httpStatus = e.getStatusCode();
            return buildWebTriggerJsonResponse(CallControlAction.createCall, CallControlStatus.failure, httpStatus, null);
        } catch (StorageEntityNotFound e) {
            logger.error("",e);
            return Response.status(Status.NOT_FOUND).build(); // for case when the cc file does not exist
        }
        catch (StorageException e) {
            logger.error("",e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(selectedMediaType).build();
        }
    }


    @GET
    @Path("{appname}/log")
    public Response appLog(@PathParam("appname") String appName) {
        ProjectAwareRvdContext rvdContext;
        try {
            rvdContext = new ProjectAwareRvdContext(appName, request, servletContext);
            init(rvdContext);

            // make sure logging is enabled before allowing access to sensitive log information
            ProjectSettings projectSettings = FsProjectStorage.loadProjectSettings(appName, workspaceStorage);
            if (projectSettings == null || projectSettings.getLogging() == false)
                return Response.status(Status.NOT_FOUND).build();

            InputStream logStream;
            try {
                logStream = new FileInputStream(rvdContext.getProjectLogger().getLogFilePath());
                return Response.ok(logStream, "text/plain")
                        .header("Cache-Control", "no-cache, no-store, must-revalidate")
                        .header("Pragma", "no-cache")
                        .build();

                //response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
                //response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
                //response.setDateHeader("Expires", 0);
            } catch (FileNotFoundException e) {
                return Response.status(Status.NOT_FOUND).build(); // nothing to return. There is no log file
            }
        } catch (StorageEntityNotFound e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (StorageException e1) {
            logger.error(e1,e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("{appname}/log")
    public Response resetAppLog(@PathParam("appname") String appName) {
        ProjectAwareRvdContext rvdContext;
        try {
            rvdContext = new ProjectAwareRvdContext(appName, request, servletContext);
            init(rvdContext);

            // make sure logging is enabled before allowing access to sensitive log information
            ProjectSettings projectSettings = FsProjectStorage.loadProjectSettings(appName, workspaceStorage);
            if (projectSettings == null || projectSettings.getLogging() == false)
                return Response.status(Status.NOT_FOUND).build();

            rvdContext.getProjectLogger().reset();
            return Response.ok().build();
        } catch (StorageException e) {
            // !!! return hangup!!!
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
