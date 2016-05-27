package org.mobicents.servlet.restcomm.rvd.http.resources;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.ProjectAwareRvdContext;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.exceptions.AccessApiException;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.CallControlBadRequestException;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.CallControlException;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.CallControlInvalidConfigurationException;
import org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol.UnauthorizedCallControlAccess;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.RemoteServiceError;
import org.mobicents.servlet.restcomm.rvd.model.CallControlInfo;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.ProjectSettings;
import org.mobicents.servlet.restcomm.rvd.model.UserProfile;
import org.mobicents.servlet.restcomm.rvd.model.callcontrol.CallControlAction;
import org.mobicents.servlet.restcomm.rvd.model.callcontrol.CallControlStatus;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommAccountInfoResponse;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommClient;
import org.mobicents.servlet.restcomm.rvd.restcomm.RestcommCreateCallResponse;
import org.mobicents.servlet.restcomm.rvd.identity.BasicAuthCredentials;
import org.mobicents.servlet.restcomm.rvd.identity.SecurityUtils;
import org.mobicents.servlet.restcomm.rvd.storage.FsProfileDao;
import org.mobicents.servlet.restcomm.rvd.storage.ProfileDao;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.FsCallControlInfoStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageEntityNotFound;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.WavItemDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

@Path("apps")
public class RvdController extends SecuredRestService {
    static final Logger logger = Logger.getLogger(RvdController.class.getName());

    private RvdConfiguration rvdSettings;
    private ProjectAwareRvdContext rvdContext;

    private WorkspaceStorage workspaceStorage;
    private ModelMarshaler marshaler;

    @PostConstruct
    public void init(RvdContext rvdContext) {
        super.init();
        rvdSettings = rvdContext.getSettings();
        marshaler = rvdContext.getMarshaler();
        workspaceStorage = rvdContext.getWorkspaceStorage();
    }

    private Response runInterpreter(String appname, HttpServletRequest httpRequest,
            MultivaluedMap<String, String> requestParams) {
        String rcmlResponse;
        try {
            if (!FsProjectStorage.projectExists(appname, workspaceStorage))
                return Response.status(Status.NOT_FOUND).build();

            String targetParam = requestParams.getFirst("target");
            Interpreter interpreter = new Interpreter(rvdContext, targetParam, appname, httpRequest, requestParams,
                    workspaceStorage);
            rcmlResponse = interpreter.interpret();

            // logging rcml response, if configured
            // make sure logging is enabled before allowing access to sensitive log information
            ProjectSettings projectSettings = rvdContext.getProjectSettings();
            if (projectSettings.getLogging() == true && (projectSettings.getLoggingRCML() != null && projectSettings.getLoggingRCML() == true) ){
                interpreter.getProjectLogger().log( rcmlResponse, false).tag("app", appname).tag("RCML").done();
            }

        } catch (RemoteServiceError e) {
            logger.warn(e.getMessage());
            if (rvdContext.getProjectSettings().getLogging())
                rvdContext.getProjectLogger().log(e.getMessage()).tag("app", appname).tag("EXCEPTION").done();
            rcmlResponse = Interpreter.rcmlOnException();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (rvdContext.getProjectSettings().getLogging())
                rvdContext.getProjectLogger().log(e.getMessage()).tag("app", appname).tag("EXCEPTION").done();
            rcmlResponse = Interpreter.rcmlOnException();
        }
        if(logger.isDebugEnabled()) {
            logger.debug(rcmlResponse);
        }
        return Response.ok(rcmlResponse, MediaType.APPLICATION_XML).build();
    }

    @GET
    @Path("{appname}/controller")
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerGet(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest,
            @Context UriInfo ui) {
        try {
            rvdContext = new ProjectAwareRvdContext(appname, request, servletContext);
            init(rvdContext);
            if(logger.isInfoEnabled()) {
                logger.info("Received Restcomm GET request");
            }
            Enumeration<String> headerNames = (Enumeration<String>) httpRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
            }
            if(logger.isInfoEnabled()) {
                logger.debug(httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
            }
            MultivaluedMap<String, String> requestParams = ui.getQueryParameters();

            return runInterpreter(appname, httpRequest, requestParams);
        } catch (StorageException e) {
            logger.error(e, e);
            return Response.ok(Interpreter.rcmlOnException(), MediaType.APPLICATION_XML).build();
        }
    }

    @POST
    @Path("{appname}/controller")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    public Response controllerPost(@PathParam("appname") String appname, @Context HttpServletRequest httpRequest,
            MultivaluedMap<String, String> requestParams) {
        try {
            rvdContext = new ProjectAwareRvdContext(appname, request, servletContext);
            init(rvdContext);

            if(logger.isInfoEnabled()) {
                logger.info("Received Restcomm POST request");
            }
            if(logger.isDebugEnabled()) {
                logger.debug(httpRequest.getMethod() + " - " + httpRequest.getRequestURI() + " - " + httpRequest.getQueryString());
                logger.debug("POST Params: " + requestParams.toString());
            }
            return runInterpreter(appname, httpRequest, requestParams);
        } catch (StorageException e) {
            logger.error(e, e);
            return Response.ok(Interpreter.rcmlOnException(), MediaType.APPLICATION_XML).build();
        }
    }

    @GET
    @Path("{appname}/resources/{filename}")
    public Response getWav(@PathParam("appname") String projectName, @PathParam("filename") String filename) {
        try {
            rvdContext = new ProjectAwareRvdContext(projectName, request, servletContext);
            init(rvdContext);
            InputStream wavStream;

            try {
                wavStream = FsProjectStorage.getWav(projectName, filename, workspaceStorage);
                return Response.ok(wavStream, "audio/x-wav")
                        .header("Content-Disposition", "attachment; filename = " + filename).build();
            } catch (WavItemDoesNotExist e) {
                return Response.status(Status.NOT_FOUND).build(); // ordinary error page is returned since this will be consumed
                                                                  // either from restcomm or directly from user
            } catch (StorageException e) {
                // return buildErrorResponse(Status.INTERNAL_SERVER_ERROR, RvdResponse.Status.ERROR, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build(); // ordinary error page is returned since this will
                                                                              // be consumed either from restcomm or directly
                                                                              // from user
            }
        } catch (StorageException e1) {
            logger.error(e1, e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Web Trigger -----

    private RestcommCreateCallResponse executeAction(String projectName, HttpServletRequest request, String toParam,
                                                     String fromParam, String accessToken, UriInfo ui, String basicAuthUsername, String basicAuthPassword, String accountSid) throws StorageException, CallControlException {
        if(logger.isInfoEnabled()) {
            logger.info( "WebTrigger: Application '" + projectName + "' initiated. User request URL: " + ui.getRequestUri().toString());
        }
        if (rvdContext.getProjectSettings().getLogging())
            rvdContext.getProjectLogger().log("WebTrigger incoming request: " + ui.getRequestUri().toString(),false).tag("app", projectName).tag("WebTrigger").done();

        // load CC/WebTrigger project info
        CallControlInfo info = FsCallControlInfoStorage.loadInfo(projectName, workspaceStorage);
        // find the owner of the project
        StateHeader projectHeader = FsProjectStorage.loadStateHeader(projectName,workspaceStorage);
        String owner = projectHeader.getOwner();
        if (RvdUtils.isEmpty(owner))
            throw new CallControlException("Project '" + projectName + "' has no owner and can't be started using WebTrigger.");

        // Determine authentication type. Either rely on basic http credentials OR WebTrigger token parameter
        String username;
        String password;
        if ( ! RvdUtils.isEmpty(basicAuthUsername) ) {
            // check project owner against Restcomm authenticated user
            if( RvdUtils.safeEquals(basicAuthUsername,owner) ) {
                username = basicAuthUsername;
                password = basicAuthPassword;
            } else
                throw new UnauthorizedCallControlAccess("User '" + basicAuthUsername + "' is not authorized to access project '" + projectName + "'");
        } else {
            // If the token *is missing* or is wrong throw an error
            if (RvdUtils.isEmpty(info.accessToken) || !info.accessToken.equals(accessToken) ) {
                throw new UnauthorizedCallControlAccess("Web Trigger token authentication failed for '" + projectName + "'")
                    .setRemoteIP(request.getRemoteAddr());
            }
            // load user profile
            ProfileDao profileDao = new FsProfileDao(workspaceStorage);
            UserProfile profile = profileDao.loadUserProfile(owner);
            if (profile == null)
                throw new UnauthorizedCallControlAccess("No user profile found for user '" + owner + "'. Web trigger cannot be used for project belonging to this user.");
            username = profile.getUsername();
            password = profile.getToken();
        }
        // username & password set, ownership checked

        // guess restcomm location
        URI restcommBaseUri = RvdConfiguration.getInstance().getRestcommBaseUri();
        // initialize a restcomm client object using various information sources
        RestcommClient restcommClient;
        try {
            restcommClient = new RestcommClient(restcommBaseUri, getUserIdentityContext().getEffectiveAuthorizationHeader());
        } catch (RestcommClient.RestcommClientInitializationException e) {
            throw new CallControlException("WebTrigger",e);
        }
        if(logger.isDebugEnabled()) {
            logger.debug("WebTrigger: reaching restcomm at '" + restcommBaseUri + "'");
        }

        String rcmlUrl = info.lanes.get(0).startPoint.rcmlUrl;
        // use the existing application for RCML if none has been given
        if (RvdUtils.isEmpty(rcmlUrl)) {
            URIBuilder uriBuilder = new URIBuilder(restcommBaseUri);
            uriBuilder.setPath("/restcomm-rvd/services/apps/" + projectName + "/controller");
            try {
                rcmlUrl = uriBuilder.build().toString();
            } catch (URISyntaxException e) {
                throw new CallControlException("URI parsing error while generating the rcml url", e);
            }
        }

        // Add user supplied params to rcmlUrl
        try {
            URIBuilder uriBuilder = new URIBuilder(rcmlUrl);
            MultivaluedMap<String, String> requestParams = ui.getQueryParameters();
            for (String paramName : requestParams.keySet()) {
                if ("token".equals(paramName) || "from".equals(paramName) || "to".equals(paramName))
                    continue; // skip parameters that are used by WebTrigger it self
                // also, skip builtin parameters that will be supplied by restcomm when it reaches for the controller
                if (!rvdSettings.getRestcommParameterNames().contains(paramName))
                    uriBuilder.addParameter(Interpreter.nameModuleRequestParam(paramName), requestParams.getFirst(paramName));
            }
            rcmlUrl = uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            throw new CallControlException("Error copying user supplied parameters to rcml url", e);
        }

        if(logger.isDebugEnabled()) {
            logger.debug("WebTrigger: rcmlUrl: " + rcmlUrl);
        }

        // to
        String to = toParam;
        if (RvdUtils.isEmpty(to))
            to = info.lanes.get(0).startPoint.to;

        // from - use url, web trigger conf or default value.
        String from = fromParam;
        if (RvdUtils.isEmpty(from))
            from = info.lanes.get(0).startPoint.from;
        // fallback to the project name (sid). Only the first 10 characters are used.
        if (RvdUtils.isEmpty(from)) {
            if (!RvdUtils.isEmpty(projectName))
                from = projectName.substring(0, projectName.length() < 10 ? projectName.length() : 10);
        }

        if (RvdUtils.isEmpty(rcmlUrl))
            throw new CallControlInvalidConfigurationException("Could not determine application RCML url.");
        if (RvdUtils.isEmpty(from) || RvdUtils.isEmpty(to))
            throw new CallControlBadRequestException(
                    "Either <i>from</i> or <i>to</i> value is missing. Make sure they are both passed as query parameters or are defined in the Web Trigger configuration.")
                    .setStatusCode(400);

        try {

            // Find the account sid for the apiUsername is not available
            if (RvdUtils.isEmpty(accountSid)) {
                RestcommAccountInfoResponse accountResponse = restcommClient.get("/restcomm/2012-04-24/Accounts.json/" + getLoggedUsername()).done(
                        marshaler.getGson(), RestcommAccountInfoResponse.class);
                accountSid = accountResponse.getSid();
            }
            // Create the call
            RestcommCreateCallResponse response = restcommClient.post("/restcomm/2012-04-24/Accounts/" + accountSid + "/Calls.json")
                    .addParam("From", from).addParam("To", to).addParam("Url", rcmlUrl)
                    .done(marshaler.getGson(), RestcommCreateCallResponse.class);

            if(logger.isInfoEnabled()) {
                logger.info("WebTrigger: joined " + to + " with " + rcmlUrl);
            }
            return response;
        } catch (AccessApiException e) {
            throw new CallControlException(e.getMessage(), e).setStatusCode(e.getStatusCode());
        }
    }

    @GET
    @Path("{appname}/start{extension: (.html)?}")
    @Produces(MediaType.TEXT_HTML)
    public Response executeActionHtml(@PathParam("appname") String projectName, @Context HttpServletRequest request,
            @QueryParam("to") String toParam, @QueryParam("from") String fromParam, @QueryParam("token") String accessToken,
            @Context UriInfo ui) {
        String selectedMediaType = MediaType.TEXT_HTML;
        try {
            // if basic auth headers are present try to authenticate against restcomm first
            String authorizationHeader = request.getHeader("Authorization");
            BasicAuthCredentials basicCredentials = SecurityUtils.parseBasicAuthHeader(authorizationHeader);
            String username = null;
            String password = null;
            String accountSid = null;
            if (basicCredentials != null) {
                RestcommAccountInfoResponse accountInfo = getUserIdentityContext().getAccountInfo();
                if (accountInfo != null) {
                    username = accountInfo.getEmail_address();
                    password = basicCredentials.getPassword();
                    accountSid = accountInfo.getSid();
                }
            }
            rvdContext = new ProjectAwareRvdContext(projectName, request, servletContext);
            init(rvdContext);
            RestcommCreateCallResponse createCallResponse = executeAction(projectName, request, toParam, fromParam,
                    accessToken, ui, username, password, accountSid);
            return buildWebTriggerHtmlResponse("Web Trigger", "Create call", "success",
                    "Created call with SID " + createCallResponse.getSid() + " from " + createCallResponse.getFrom() + " to "
                            + createCallResponse.getTo(), 200);
        } catch (UnauthorizedCallControlAccess e) {
            logger.warn(e);
            return buildWebTriggerHtmlResponse("Web Trigger", "Create call", "failure", "Authentication error", 401);
        } catch (CallControlException e) {
            logger.error("", e);
            int httpStatus = 500;
            if (e.getStatusCode() != null)
                httpStatus = e.getStatusCode();
            return buildWebTriggerHtmlResponse("Web Trigger", "Create call", "failure", "", httpStatus);
        } catch (StorageEntityNotFound e) {
            logger.error("", e);
            return Response.status(Status.NOT_FOUND).build(); // for case when the cc file does not exist
        } catch (StorageException e) {
            logger.error("", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(selectedMediaType).build();
        }
    }

    @GET
    @Path("{appname}/start.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeActionJson(@PathParam("appname") String projectName, @Context HttpServletRequest request,
            @QueryParam("to") String toParam, @QueryParam("from") String fromParam, @QueryParam("token") String accessToken,
            @Context UriInfo ui) {
        String selectedMediaType = MediaType.APPLICATION_JSON;
        try {
            // if basic auth headers are present try to authenticate against restcomm first
            String authorizationHeader = request.getHeader("Authorization");
            BasicAuthCredentials basicCredentials = SecurityUtils.parseBasicAuthHeader(authorizationHeader);
            String username = null;
            String password = null;
            String accountSid = null;
            if (basicCredentials != null) {
                    RestcommAccountInfoResponse accountInfo = getUserIdentityContext().getAccountInfo();
                    if (accountInfo != null) {
                        username = accountInfo.getEmail_address();
                        password = basicCredentials.getPassword();
                        accountSid = accountInfo.getSid();
                    }
            }
            rvdContext = new ProjectAwareRvdContext(projectName, request, servletContext);
            init(rvdContext);
            RestcommCreateCallResponse createCallResponse = executeAction(projectName, request, toParam, fromParam,
                    accessToken, ui, username, password, accountSid);
            return buildWebTriggerJsonResponse(CallControlAction.createCall, CallControlStatus.success, 200, createCallResponse);
        } catch (UnauthorizedCallControlAccess e) {
            logger.warn(e);
            return buildWebTriggerJsonResponse(CallControlAction.createCall, CallControlStatus.failure, 401, null);
        } catch (CallControlException e) {
            logger.error("", e);
            int httpStatus = 500;
            if (e.getStatusCode() != null)
                httpStatus = e.getStatusCode();
            return buildWebTriggerJsonResponse(CallControlAction.createCall, CallControlStatus.failure, httpStatus, null);
        } catch (StorageEntityNotFound e) {
            logger.error("", e);
            return Response.status(Status.NOT_FOUND).build(); // for case when the cc file does not exist
        } catch (StorageException e) {
            logger.error("", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(selectedMediaType).build();
        }
    }

    @GET
    @Path("{appname}/log")
    public Response appLog(@PathParam("appname") String appName) {
        secure();
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
                return Response.ok(logStream, "text/plain").header("Cache-Control", "no-cache, no-store, must-revalidate")
                        .header("Pragma", "no-cache").build();

                // response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
                // response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
                // response.setDateHeader("Expires", 0);
            } catch (FileNotFoundException e) {
                return Response.status(Status.NOT_FOUND).build(); // nothing to return. There is no log file
            }
        } catch (StorageEntityNotFound e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (StorageException e1) {
            logger.error(e1, e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("{appname}/log")
    public Response resetAppLog(@PathParam("appname") String appName) {
        secure();
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
