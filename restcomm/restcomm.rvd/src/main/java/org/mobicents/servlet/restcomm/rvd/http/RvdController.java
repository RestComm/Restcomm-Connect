package org.mobicents.servlet.restcomm.rvd.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
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
import org.mobicents.servlet.restcomm.rvd.ProjectAwareRvdContext;
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
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectItem;
import org.mobicents.servlet.restcomm.rvd.model.client.SettingsModel;
import org.mobicents.servlet.restcomm.rvd.serverapi.CreateCallResponse;
import org.mobicents.servlet.restcomm.rvd.serverapi.RestcommClient;
import org.mobicents.servlet.restcomm.rvd.serverapi.RestcommClient.RestcommClientException;
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

@Path("apps")
public class RvdController extends RestService {
    static final Logger logger = Logger.getLogger(RvdController.class.getName());

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest request;

    private RvdConfiguration rvdSettings;
    //private ProjectStorage projectStorage;
    private ProjectService projectService;
    //private Gson gson;
    private RvdContext rvdContext;

    private WorkspaceStorage workspaceStorage;
    private ModelMarshaler marshaler;


    void init(RvdContext rvdContext) {
        this.rvdContext = rvdContext;
        rvdSettings = rvdContext.getSettings();
        marshaler = rvdContext.getMarshaler();
        workspaceStorage = new WorkspaceStorage(rvdSettings.getWorkspaceBasePath(), marshaler);
        projectService = new ProjectService(rvdContext, workspaceStorage);
    }


    /*@PostConstruct
    void init() {
        gson = new Gson();
        rvdContext = new ProjectAwareRvdContext(request, servletContext);
        rvdSettings = rvdContext.getSettings();
        projectStorage = rvdContext.getProjectStorage();
        projectService = new ProjectService(rvdContext);

        this.marshaler = rvdContext.getMarshaler();
        this.workspaceStorage = new WorkspaceStorage(rvdSettings.getWorkspaceBasePath(), marshaler);
    }*/

    private Response runInterpreter( ProjectAwareRvdContext rvdContext, String appname, HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams ) {
        String rcmlResponse;
        try {
            if (!FsProjectStorage.projectExists(appname,workspaceStorage))
                return Response.status(Status.NOT_FOUND).build();

            String targetParam = requestParams.getFirst("target");
            Interpreter interpreter = new Interpreter(rvdContext, targetParam, appname, httpRequest, requestParams, workspaceStorage);
            rcmlResponse = interpreter.interpret();

        } catch ( RvdException e ) {
            logger.error(e.getMessage(), e);
            rvdContext.getProjectLogger().log(e.getMessage()).tag("app", appname).tag("EXCEPTION").done();
            rcmlResponse = "<Response><Hangup/></Response>";
        }


        logger.debug(rcmlResponse);
        return Response.ok(rcmlResponse, MediaType.APPLICATION_XML).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listApps(@Context HttpServletRequest request) {
        RvdContext rvdContext = new RvdContext(request, servletContext);
        init(rvdContext);
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
        ProjectAwareRvdContext rvdContext = new ProjectAwareRvdContext(appname, request, servletContext);
        init(rvdContext);
        logger.info("Received Restcomm GET request");
        logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
        //logger.info("Using restcommPublicIP: " + rvdSettings.getEffectiveRestcommIp(httpRequest));
        MultivaluedMap<String, String> requestParams = ui.getQueryParameters();

        return runInterpreter(rvdContext, appname, httpRequest, requestParams);
    }

    @POST
    @Path("{appname}/controller")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerPost(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams) {
        ProjectAwareRvdContext rvdContext = new ProjectAwareRvdContext(appname, request, servletContext);
        init(rvdContext);
        logger.info("Received Restcomm POST request");
        logger.debug( httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
        logger.debug("POST Params: " + requestParams.toString());

        return runInterpreter(rvdContext, appname, httpRequest, requestParams);
    }

    @GET
    @Path("{appname}/resources/{filename}")
    public Response getWav(@PathParam("appname") String projectName, @PathParam("filename") String filename ) {
        ProjectAwareRvdContext rvdContext = new ProjectAwareRvdContext(projectName, request, servletContext);
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

    /**
     * Runs a query on Restcomm numbers api and tries to match an application named X with its number. If any match is found it returns it
     * @param apiHost
     * @param apiPort
     * @param apiUsername
     * @param apiPort2
     * @param projectName
     * @return
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

    @GET
    @Path("{appname}/start")
    public Response executeAction(@PathParam("appname") String projectName, @Context HttpServletRequest request, @QueryParam("to") String toParam, @QueryParam("from") String fromParam, @Context UriInfo ui ) {

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
                    // skip builtin parameters supplied by restcomm
                    if ( ! rvdSettings.getRestcommParameterNames().contains(paramName))
                        if ( !("From".equals(paramName) || "To".equals(paramName) || "Url".equals(paramName ) ) )  // filter out params for the executeAction() itself. Pass only parameters intended for the rcml application.
                                uriBuilder.addParameter(paramName, requestParams.getFirst(paramName));
                }
                rcmlUrl = uriBuilder.build().toString();
            } catch (URISyntaxException e) {
                throw new CallControlException("Error copying user supplied parameters to rcml url", e);
            }


            String to = toParam;
            if ( RvdUtils.isEmpty(to) )
                to = info.lanes.get(0).startPoint.to;

            String from = fromParam;
            if ( RvdUtils.isEmpty(from) )
                from = info.lanes.get(0).startPoint.from;
            //if ( RvdUtils.isEmpty(from) )
            //    from = guessApplicationDID(apiHost, apiPort, apiUsername, apiPort, projectName);

            if ( RvdUtils.isEmpty(apiHost) || apiPort == null || RvdUtils.isEmpty(apiUsername) || RvdUtils.isEmpty(apiPassword) || RvdUtils.isEmpty(rcmlUrl) )
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            if ( RvdUtils.isEmpty(from) || RvdUtils.isEmpty(to) )
                return Response.status(Status.BAD_REQUEST).build();


            RestcommClient client = new RestcommClient(apiHost, apiPort, apiUsername, apiPassword);
            CreateCallResponse response = client.post("/restcomm/2012-04-24/Accounts/" + "ACae6e420f425248d6a26948c17a9e2acf" + "/Calls.json")
                .addParam("From", from)
                .addParam("To", to)
                .addParam("Url", rcmlUrl).done(marshaler.getGson(), CreateCallResponse.class);

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
        } catch (RestcommClientException e) {
            logger.error(e,e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }




    @GET
    @Path("{appname}/log")
    public Response appLog(@PathParam("appname") String appName) {
        ProjectAwareRvdContext rvdContext = new ProjectAwareRvdContext(appName, request, servletContext);
        //init(new ProjectAwareRvdContext(appName, request, servletContext));
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
            return Response.ok().build(); // nothing to return. There is no log file
        }
    }

    @GET
    @Path("{appname}/log/reset")
    public Response resetAppLog(@PathParam("appname") String appName) {
        ProjectAwareRvdContext rvdContext = new ProjectAwareRvdContext(appName, request, servletContext);
        rvdContext.getProjectLogger().reset();
        return Response.ok().build();
    }

}
