package org.mobicents.servlet.restcomm.rvd.interpreter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdContext;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.ESRequestException;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.exceptions.UndefinedTarget;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.BadExternalServiceResponse;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.ErrorParsingExternalServiceUrl;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.InvalidAccessOperationAction;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.RemoteServiceError;
import org.mobicents.servlet.restcomm.rvd.model.StepJsonDeserializer;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.client.UrlParam;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlResponse;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;
import org.mobicents.servlet.restcomm.rvd.model.server.NodeName;
import org.mobicents.servlet.restcomm.rvd.model.server.ProjectOptions;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.ClientNounConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.ConferenceNounConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.NumberNounConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlClientNoun;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlConferenceNoun;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlDialStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlNumberNoun;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.RcmlSipuriNoun;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.SipuriNounConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.es.AccessOperation;
import org.mobicents.servlet.restcomm.rvd.model.steps.es.Assignment;
import org.mobicents.servlet.restcomm.rvd.model.steps.es.ExternalServiceStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.es.RouteMapping;
import org.mobicents.servlet.restcomm.rvd.model.steps.es.ValueExtractor;
import org.mobicents.servlet.restcomm.rvd.model.steps.fax.FaxStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.fax.RcmlFaxStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.gather.RcmlGatherStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.hangup.RcmlHungupStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.pause.RcmlPauseStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.play.PlayStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.play.RcmlPlayStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.record.RcmlRecordStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.redirect.RcmlRedirectStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.redirect.RedirectStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.reject.RcmlRejectStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.say.RcmlSayStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.say.SayStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.sms.RcmlSmsStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.sms.SmsStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdcollect.UssdCollectRcml;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdlanguage.UssdLanguageConverter;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdlanguage.UssdLanguageRcml;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay.UssdSayRcml;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay.UssdSayStepConverter;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.thoughtworks.xstream.XStream;


public class Interpreter {

    static final Logger logger = Logger.getLogger(Interpreter.class.getName());

    private RvdConfiguration rvdSettings;
    private ProjectStorage projectStorage;
    private HttpServletRequest httpRequest;

    private XStream xstream;
    private Gson gson;
    private String targetParam;
    private Target target;
    private String appName;
   // private Map<String,String> requestParameters; // parameters like digits, callSid etc.
    MultivaluedMap<String, String> requestParams;

    private String contextPath;

    private String rcmlResult;
    private Map<String, String> variables = new HashMap<String, String>();
    private List<NodeName> nodeNames;


    public Interpreter(RvdContext rvdContext, String targetParam, String appName, HttpServletRequest httpRequest, MultivaluedMap<String, String> requestParams) {
        this.rvdSettings = rvdContext.getSettings();
        this.projectStorage = rvdContext.getProjectStorage();
        this.httpRequest = httpRequest;
        this.targetParam = targetParam;
        this.appName = appName;
        this.requestParams = requestParams;

        this.contextPath = httpRequest.getContextPath();
        init();
    }

    // common intializations for all constructors
    private void init() {
        xstream = new XStream();
        xstream.registerConverter(new SayStepConverter());
        xstream.registerConverter(new PlayStepConverter());
        xstream.registerConverter(new RedirectStepConverter());
        xstream.registerConverter(new SmsStepConverter());
        xstream.registerConverter(new FaxStepConverter());
        xstream.registerConverter(new NumberNounConverter());
        xstream.registerConverter(new ClientNounConverter());
        xstream.registerConverter(new ConferenceNounConverter());
        xstream.registerConverter(new SipuriNounConverter());
        xstream.registerConverter(new UssdSayStepConverter());
        xstream.registerConverter(new UssdLanguageConverter());
        xstream.addImplicitCollection(RcmlDialStep.class, "nouns");
        xstream.alias("Response", RcmlResponse.class);
        xstream.addImplicitCollection(RcmlResponse.class, "steps");
        xstream.alias("Say", RcmlSayStep.class);
        xstream.alias("Play", RcmlPlayStep.class);
        xstream.alias("Gather", RcmlGatherStep.class);
        xstream.alias("Dial", RcmlDialStep.class);
        xstream.alias("Hangup", RcmlHungupStep.class);
        xstream.alias("Redirect", RcmlRedirectStep.class);
        xstream.alias("Reject", RcmlRejectStep.class);
        xstream.alias("Pause", RcmlPauseStep.class);
        xstream.alias("Sms", RcmlSmsStep.class);
        xstream.alias("Record", RcmlRecordStep.class);
        xstream.alias("Fax", RcmlFaxStep.class);
        xstream.alias("Number", RcmlNumberNoun.class);
        xstream.alias("Client", RcmlClientNoun.class);
        xstream.alias("Conference", RcmlConferenceNoun.class);
        xstream.alias("Sip", RcmlSipuriNoun.class);
        xstream.alias("UssdMessage", UssdSayRcml.class);
        xstream.alias("UssdCollect", UssdCollectRcml.class);
        xstream.alias("Language", UssdLanguageRcml.class);
        xstream.addImplicitCollection(RcmlGatherStep.class, "steps");
        xstream.addImplicitCollection(UssdCollectRcml.class, "messages");
        xstream.useAttributeFor(UssdCollectRcml.class, "action");
        xstream.useAttributeFor(RcmlGatherStep.class, "action");
        xstream.useAttributeFor(RcmlGatherStep.class, "timeout");
        xstream.useAttributeFor(RcmlGatherStep.class, "finishOnKey");
        xstream.useAttributeFor(RcmlGatherStep.class, "method");
        xstream.useAttributeFor(RcmlGatherStep.class, "numDigits");
        xstream.useAttributeFor(RcmlSayStep.class, "voice");
        xstream.useAttributeFor(RcmlSayStep.class, "language");
        xstream.useAttributeFor(RcmlSayStep.class, "loop");
        xstream.useAttributeFor(RcmlPlayStep.class, "loop");
        xstream.useAttributeFor(RcmlRejectStep.class, "reason");
        xstream.useAttributeFor(RcmlPauseStep.class, "length");
        xstream.useAttributeFor(RcmlRecordStep.class, "action");
        xstream.useAttributeFor(RcmlRecordStep.class, "method");
        xstream.useAttributeFor(RcmlRecordStep.class, "timeout");
        xstream.useAttributeFor(RcmlRecordStep.class, "finishOnKey");
        xstream.useAttributeFor(RcmlRecordStep.class, "maxLength");
        xstream.useAttributeFor(RcmlRecordStep.class, "transcribe");
        xstream.useAttributeFor(RcmlRecordStep.class, "transcribeCallback");
        xstream.useAttributeFor(RcmlRecordStep.class, "playBeep");
        xstream.useAttributeFor(RcmlDialStep.class, "action");
        xstream.useAttributeFor(RcmlDialStep.class, "method");
        xstream.useAttributeFor(RcmlDialStep.class, "timeout");
        xstream.useAttributeFor(RcmlDialStep.class, "timeLimit");
        xstream.useAttributeFor(RcmlDialStep.class, "callerId");
        xstream.useAttributeFor(RcmlDialStep.class, "record");
        xstream.aliasField("Number", RcmlDialStep.class, "number");
        xstream.aliasField("Client", RcmlDialStep.class, "client");
        xstream.aliasField("Conference", RcmlDialStep.class, "conference");
        xstream.aliasField("Uri", RcmlDialStep.class, "sipuri");

        // xstream.aliasField(alias, definedIn, fieldName);
        gson = new GsonBuilder().registerTypeAdapter(Step.class, new StepJsonDeserializer()).create();
    }

    public RvdConfiguration getRvdSettings() {
        return rvdSettings;
    }

    public String getAppName() {
        return appName;
    }


    public void setAppName(String appName) {
        this.appName = appName;
    }


    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public Map<String, String> getVariables() {
        return variables;
    }


    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }


    public Target getTarget() {
        return target;
    }


    public void setTarget(Target target) {
        this.target = target;
    }


    public String interpret() throws RvdException {
        String response = null;

        ProjectOptions projectOptions = projectStorage.loadProjectOptions(appName);
        nodeNames = projectOptions.getNodeNames();

        if (targetParam == null || "".equals(targetParam)) {
            // No target has been specified. Load the default from project file
            targetParam = projectOptions.getDefaultTarget();
            if (targetParam == null)
                throw new UndefinedTarget();
            logger.debug("override default target to " + targetParam);
        }

        handleStickyParameters();
        processBootstrapParameters();
        processRequestParameters();

        response = interpret(targetParam, null, null);
        return response;
    }






    public MultivaluedMap<String, String> getRequestParams() {
        return requestParams;
    }

    public String getContextPath() {
        return contextPath;
    }


    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }


    public String interpret(String targetParam, RcmlResponse rcmlModel, Step prependStep ) throws InterpreterException, StorageException {

        logger.debug("starting interpeter for " + targetParam);

        target = Interpreter.parseTarget(targetParam);

        // TODO make sure all the required components of the target are available here

        if (target.action != null) {
            // Event handling
            loadStep(target.stepname).handleAction(this);
        } else {
            // RCML Generation

            if (rcmlModel == null )
                rcmlModel = new RcmlResponse();
            List<String> nodeStepnames = projectStorage.loadNodeStepnames(appName, target.getNodename());

            // if no starting step has been specified in the target, use the first step of the node as default
            if (target.getStepname() == null && !nodeStepnames.isEmpty())
                target.setStepname(nodeStepnames.get(0));

            // Prepend step if required. Usually used for error messages
            if ( prependStep != null ) {
                RcmlStep rcmlStep = prependStep.render(this);
                logger.debug("Prepending say step: " + rcmlStep );
                rcmlModel.steps.add( rcmlStep );
            }

            boolean startstep_found = false;
            for (String stepname : nodeStepnames) {

                if (stepname.equals(target.getStepname()))
                    startstep_found = true;

                if (startstep_found) {
                    // we found our starting step. Let's start processing
                    Step step = loadStep(stepname);
                    String rerouteTo = processStep(step); // is meaningful only for some of the steps like ExternalService steps
                    // check if we have to break the currently rendered module
                    if ( rerouteTo != null )
                        return interpret(rerouteTo, rcmlModel, null);
                    // otherwise continue rendering the current module
                    RcmlStep rcmlStep = step.render(this);
                    if ( rcmlStep != null)
                        rcmlModel.steps.add(rcmlStep);
                }
            }

            rcmlResult = xstream.toXML(rcmlModel);
        }

        return rcmlResult; // this is in case of an error
    }

    private Step loadStep(String stepname) throws StorageException  {
        //String stepfile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data/"
        //        + target.getNodename() + "." + stepname));
        String stepfile_json = projectStorage.loadStep(appName, target.getNodename(), stepname);
        Step step = gson.fromJson(stepfile_json, Step.class);

        return step;
    }


    private String evaluateExtractorExpression( ValueExtractor extractor, JsonElement response_element) throws InvalidAccessOperationAction, BadExternalServiceResponse {
        String value = "";

        JsonElement element = response_element;
        for ( AccessOperation operation : extractor.getAccessOperations() ) {
            if ( element == null )
                throw new BadExternalServiceResponse();

            if ( "object".equals(operation.getKind()) ) {
                if ( !element.isJsonObject() )
                    throw new BadExternalServiceResponse("No JSON object found");
                if ("propertyNamed".equals(operation.getAction()) )
                    element = element.getAsJsonObject().get( operation.getProperty() );
                else
                    throw new InvalidAccessOperationAction();
            } else
            if ( "array".equals(operation.getKind()) ) {
                if ( !element.isJsonArray() )
                    throw new BadExternalServiceResponse("No JSON array found");
                if ("itemAtPosition".equals(operation.getAction()) )
                    element = element.getAsJsonArray().get( operation.getPosition() );
                else
                    throw new InvalidAccessOperationAction();
            } else
            if ( "value".equals(operation.getKind()) ) {
                if ( !element.isJsonPrimitive() )
                    throw new BadExternalServiceResponse("No primitive value found (maybe null returned?)");
                value = element.getAsString();
            }
        }

        return value;
    }



    /**
     * If the step is executable (like ExternalService) it is executed
     * @param step
     * @return String The module name to continue rendering with
     * @throws IOException
     * @throws ClientProtocolException
     */
    private String processStep(Step step) throws InterpreterException {
        if (step.getClass().equals(ExternalServiceStep.class)) {
            ExternalServiceStep esStep = (ExternalServiceStep) step;
            String next = null;
            try {

                // *** Build the request uri ***

                URI url;
                try {
                    URIBuilder uri_builder = new URIBuilder(populateVariables(esStep.getUrl()) ); // supports RVD variable expansion
                    if (uri_builder.getHost() == null ) {
                        logger.debug("External Service: Relative url is used. Will override from http request to RVD controller");
                        // if this is a relative url fill in missing fields from the request
                        uri_builder.setScheme(httpRequest.getScheme());
                        uri_builder.setHost(httpRequest.getServerName());
                        uri_builder.setPort(httpRequest.getServerPort());
                        if (  ! uri_builder.getPath().startsWith("/") )
                            uri_builder.setPath("/" + uri_builder.getPath());
                    }

                    if ( esStep.getMethod() == null || "GET".equals(esStep.getMethod()) )
                        for ( UrlParam urlParam : esStep.getUrlParams() )
                            uri_builder.addParameter(urlParam.getName(), populateVariables(urlParam.getValue()) );

                    url = uri_builder.build();
                } catch (URISyntaxException e) {
                    throw new ErrorParsingExternalServiceUrl( "URL: " + esStep.getUrl(), e);
                }


                // *** Make the request and get a status code and a response. Build a JsonElement from the response  ***

                CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response;
                int statusCode;
                JsonElement response_element = null;

                logger.info("Requesting from url: " + url);
                logger.debug("Requesting from url: " + url);
                if ( "POST".equals(esStep.getMethod()) ) {
                    HttpPost post = new HttpPost(url);
                    List <NameValuePair> values = new ArrayList <NameValuePair>();
                    for ( UrlParam urlParam : esStep.getUrlParams() )
                        values.add(new BasicNameValuePair(urlParam.getName(), urlParam.getValue()));
                    post.setEntity(new UrlEncodedFormEntity(values));
                    post.addHeader("Authorization", "Basic " + RvdUtils.buildHttpAuthorizationToken(esStep.getUsername(), esStep.getPassword()));
                    response = client.execute( post );
                } else
                if ( esStep.getMethod() == null || esStep.getMethod().equals("GET") ) {
                    HttpGet get = new HttpGet( url );
                    get.addHeader("Authorization", "Basic " + RvdUtils.buildHttpAuthorizationToken(esStep.getUsername(), esStep.getPassword()));
                    response = client.execute( get );
                } else
                    throw new InterpreterException("Unknonwn HTTP method specified: " + esStep.getMethod() );

                statusCode = response.getStatusLine().getStatusCode();

                // In  case of error in the service no need to proceed. Just continue the "onException" module if set
                if ( statusCode >= 400 && statusCode < 600 ) {
                    logger.info("Remote service failed with: " + response.getStatusLine());
                    if ( ! RvdUtils.isEmpty(esStep.getExceptionNext()) )
                        return esStep.getExceptionNext();
                    else
                        throw new RemoteServiceError("Service " + url + " failed with: " + response.getStatusLine() +". Throwing an error since no 'On Remote Exception' has been defined.");
                }

                // Build a JsonElement from the response
               HttpEntity entity = response.getEntity();
                if ( entity != null ) {
                    JsonParser parser = new JsonParser();
                    String entity_string = EntityUtils.toString(entity);
                    //logger.info("ES: Received " + entity_string.length() + " bytes");
                    //logger.debug("ES Response: " + entity_string);
                    response_element = parser.parse(entity_string);
                }


                // *** Determine what to do next. Find the next module name or whether to continue in the current module ***

                if ( esStep.getDoRouting() ) {
                    if ( "fixed".equals( esStep.getNextType() ) )
                        next = esStep.getNext();
                    else
                    if ( "responseBased".equals(esStep.getNextType()) || "mapped".equals(esStep.getNextType())) {
                        String nextValue = evaluateExtractorExpression(esStep.getNextValueExtractor(), response_element);

                        if ( "responseBased".equals(esStep.getNextType()) ) {
                            next = getNodeNameByLabel( nextValue );
                        } else
                        if ( "mapped".equals(esStep.getNextType()) ) {
                            if ( esStep.getRouteMappings() != null ) {
                                for ( RouteMapping mapping : esStep.getRouteMappings() ) {
                                    if ( nextValue != null && nextValue.equals(mapping.getValue()) ) {
                                        next = mapping.getNext();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    // if no next route has been found throw an error
                    if ( RvdUtils.isEmpty(next) ) {
                        throw new InterpreterException("No valid module could be found for ES routing"); // use a general exception for now.
                        //next = esStep.getDefaultNext();
                        //if ( RvdUtils.isEmpty(next) )
                        //    throw new ReferencedModuleDoesNotExist("No module specified for ES routing and no default route exists either");
                        //logger.debug("No valid route returned. Will use default route: " + next );
                    }
                    logger.info( "Routing enabled. Chosen target: " + next);
                }


                // *** Perform the assignments ***

                try {
                    if ( esStep.getDoRouting() && ("responseBased".equals(esStep.getNextType()) || "mapped".equals(esStep.getNextType())) ) {
                        for ( Assignment assignment : esStep.getAssignments() ) {
                            logger.debug("working on variable " + assignment.getDestVariable() );
                            logger.debug( "moduleNameScope: " + assignment.getModuleNameScope());
                            if ( assignment.getModuleNameScope() == null || assignment.getModuleNameScope().equals(next) ) {
                                String value = null;
                                try {
                                    value = evaluateExtractorExpression(assignment.getValueExtractor(), response_element);
                                } catch ( BadExternalServiceResponse e ) {
                                    logger.error("Could not parse variable "  + assignment.getDestVariable() + ". Variable not found in response");
                                    throw e;
                                }

                                if ( "application".equals(assignment.getScope()) )
                                    putStickyVariable(assignment.getDestVariable(), value);
                                variables.put(assignment.getDestVariable(), value );
                            } else
                                logger.debug("skipped assignment to " + assignment.getDestVariable() );
                        }
                    }  else {
                        for ( Assignment assignment : esStep.getAssignments() ) {
                            logger.debug("working on variable " + assignment.getDestVariable() );
                            String value = null;
                            try {
                                value = evaluateExtractorExpression(assignment.getValueExtractor(), response_element);
                            } catch ( BadExternalServiceResponse e ) {
                                logger.error("Could not parse variable "  + assignment.getDestVariable() + ". Variable not found in response");
                                throw e;
                            }

                            if ( "application".equals(assignment.getScope()) )
                                putStickyVariable(assignment.getDestVariable(), value);

                            variables.put(assignment.getDestVariable(), value );
                        }
                    }
                    logger.debug("variables after processing ExternalService step: " + variables.toString() );
                } catch (JsonSyntaxException e) {
                    throw new BadExternalServiceResponse("External Service request received a malformed JSON response" );
                }

            } catch (IOException e) {
                throw new ESRequestException("Error processing ExternalService step " + step.getName(), e);
            }
            return next;

        } // if (step.getClass().equals(ExternalServiceStep.class))

        return null;
    }

    /**
     * Processes a block of text typically used for <Say/>ing that may contain variable expressions. Replaces variable
     * expressions with their corresponding values from interpreter's variables map
     */
    public String populateVariables(String sourceText) {
        if ( sourceText == null )
            return sourceText;

        // This class serves strictly the purposes of the following algorithm
        final class VariableInText {
            String variableName;
            Integer position;

            VariableInText(String variableName, Integer position) {
                this.variableName = variableName;
                this.position = position;
            }
        }

        Pattern pattern = Pattern.compile("\\$([A-Za-z]+[A-Za-z0-9_-]*)");
        Matcher matches = pattern.matcher(sourceText);

        int searchStart = 0;
        List<VariableInText> variablesInText = new ArrayList<VariableInText>();
        while (matches.find(searchStart)) {
            variablesInText.add(new VariableInText(matches.group(1), matches.start())); // always at position 1 (second
                                                                                        // position)
            searchStart = matches.end();
        }

        // for ( VariableInText v : variablesInText ) {
        // System.out.printf( "found variable %s at %d\n", v.variableName, v.position );
        // }

        StringBuffer buffer = new StringBuffer(sourceText);
        Collections.reverse(variablesInText);
        for (VariableInText v : variablesInText) {
            String replaceValue = "";
            if (variables.containsKey(v.variableName))
                replaceValue = variables.get(v.variableName);

            buffer.replace(v.position, v.position + v.variableName.length() + 1, replaceValue == null ? "" : replaceValue); // +1 is for the $ character
        }

        return buffer.toString();
    }

    public String buildAction(Map<String, String> pairs) {
        String query = "";
        for (String key : pairs.keySet()) {
            if ("".equals(query))
                query += "?";
            else
                query += "&";

            String encodedValue = "";
            String value = pairs.get(key);
            if ( value != null )
                try {
                    encodedValue = URLEncoder.encode( value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    logger.warn("Error encoding RVD variable " + key + ": " + value, e);
                }

            query += key + "=" + encodedValue;
        }

        // append sticky parameters
        for ( String variableName : variables.keySet() ) {
            if( variableName.startsWith(RvdConfiguration.STICKY_PREFIX) ) {
                if ("".equals(query))
                    query += "?";
                else
                    query += "&";

                String encodedValue = "";
                String value = variables.get(variableName);
                if ( value != null )
                    try {
                        encodedValue = URLEncoder.encode( value, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        logger.warn("Error encoding RVD variable " + variableName + ": " + value, e);
                    }

                query += variableName + "=" + encodedValue;
            }
        }

        return "controller" + query;
    }

    /*
     * //$url = $scope['conf']['appContext'] . '/index.php'; $url = 'index.php'; $pairs = array(); foreach ( $scope['sticky'] as
     * $name => $value ) { $name = 'sticky_' . $name; $pairs[] = $name . '=' . $value; } foreach ($added_pairs as $name =>
     * $value) $pairs[] = $name.'='.$value;
     *
     * if ( !empty($pairs) ) $url .= '?' . implode( '&amp;', $pairs );
     *
     * return $url;
     */

    public static Target parseTarget(String targetParam) {
        Target target = new Target();

        // TODO accept only valid characters in the target i.e. alphanumeric

        Pattern pattern = Pattern.compile("^([^.]+)(.([^.]+))?(.([^.]+))?");
        Matcher matcher = pattern.matcher(targetParam);
        if (matcher.find()) {
            if (matcher.groupCount() >= 1)
                target.setNodename(matcher.group(1));
            if (matcher.groupCount() >= 3)
                target.setStepname(matcher.group(3));
            if (matcher.groupCount() >= 5)
                target.setAction(matcher.group(5));
        }

        return target;
    }

    /**
     * @param label
     * @return The 'name' of the first node with the specified label. If not found returns null
     */
    private String getNodeNameByLabel( String label ) {
        for ( NodeName nodename : nodeNames ) {
            if ( label.equals(nodename.getLabel()) )
                return nodename.getName();
        }
        return null;
    }

    /**
     * Build a relative url to the named module
     * @param moduleName
     * @return the url or null if the module does not exist
     */
    public String moduleUrl(String moduleName) {
        String url = null;
        for ( NodeName nodeName : nodeNames )  {
            if ( nodeName.getName().equals(moduleName)) {
                Map<String, String> pairs = new HashMap<String, String>();
                pairs.put("target", moduleName);
                url = buildAction(pairs);
                break; // found it
            }
        }
        return url;
    }

    /**
     * Converts a file resource to a recorded wav file into an http resource accessible over HTTP. The path generated path for the wav files is hardcoded to /restcomm/recordings
     * @param fileResource
     * @param interpreter
     * @return
     */
    public String convertRecordingFileResourceHttp(String fileResource, HttpServletRequest request) throws URISyntaxException {
        String httpResource = fileResource; // assume this is already an http resource

        URIBuilder fileUriBuilder = new URIBuilder(fileResource);

        if ( ! fileUriBuilder.isAbsolute() ) {
            logger.warn("Cannot convert file URL to http URL - " + fileResource);
            return "";
        }

        if ( fileUriBuilder.getScheme().startsWith("http") ) // http or https - nothing to worry about
            return fileResource;

        if ( fileUriBuilder.getScheme().startsWith("file") ) {
            String wavFilename = "";
            int filenameBeforeStartPos = fileResource.lastIndexOf('/');
            if ( filenameBeforeStartPos != -1 ) {
                wavFilename = fileResource.substring(filenameBeforeStartPos+1);
                String hostname = rvdSettings.getEffectiveRestcommIp(request);
                //URIBuilder httpUriBuilder = new URIBuilder().setScheme(request.getScheme()).setHost(request.getServerName()).setPort(request.getServerPort()).setPath("/restcomm/recordings/" + wavFilename);
                URIBuilder httpUriBuilder = new URIBuilder().setScheme(request.getScheme()).setHost(hostname).setPort(request.getServerPort()).setPath("/restcomm/recordings/" + wavFilename);
                httpResource = httpUriBuilder.build().toString();
            }
        }

        return httpResource;
    }
    /**
     * Propagate existing sticky variables by putting them in the variables array. Whoever creates an action link from now on should take them into account
     * also make a local copy of them without the sticky_ prefix so that they can be accessed as ordinary module variables
     */
    public void handleStickyParameters() {
        for ( String anyVariableName : getRequestParams().keySet() ) {
            if ( anyVariableName.startsWith(RvdConfiguration.STICKY_PREFIX) ) {
                // set up sticky variables
                String variableValue = getRequestParams().getFirst(anyVariableName);
                getVariables().put(anyVariableName, variableValue );

                // make local copies
                // First, rip off the sticky_prefix
                String localVariableName = anyVariableName.substring(RvdConfiguration.STICKY_PREFIX.length());
                getVariables().put(localVariableName, variableValue);
            }
        }
    }

    public void putStickyVariable(String name, String value) {
            variables.put(RvdConfiguration.STICKY_PREFIX + name, value);
    }

    /**
     * Create rvd variables out of Restcomm request parameters such as 'CallSid', 'AccountSid' etc. Use the 'core_'
     * prefix in their names.
     */
    private void processRequestParameters() {
        Set<String> validNames = new HashSet<String>(Arrays.asList(new String[] {"CallSid","AccountSid","From","To","Body","CallStatus","ApiVersion","Direction","CallerName"}));
        for ( String anyVariableName : getRequestParams().keySet() ) {
            if ( validNames.contains(anyVariableName) ) {
                String variableValue = getRequestParams().getFirst(anyVariableName);
                getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + anyVariableName, variableValue );
            }
        }
    }

    /** Add bootstrap parameters to the variables array. Usually these are used in application downloaded
     * from the app store.
     * @throws StorageException
     *
     *
     */
    private void processBootstrapParameters() throws StorageException {

        if ( ! projectStorage.hasBootstrapInfo(appName) )
            return; // nothing to do

        JsonElement rootElement = projectStorage.loadBootstrapInfo(appName);

        if ( rootElement.isJsonObject() ) {
            JsonObject rootObject = rootElement.getAsJsonObject();
            for ( Entry<String, JsonElement> entry : rootObject.entrySet() ) {
                String name = entry.getKey();
                JsonElement valueElement = entry.getValue();
                String value;
                if ( valueElement.isJsonPrimitive() && valueElement.getAsJsonPrimitive().isString() ) {
                    value = valueElement.getAsJsonPrimitive().getAsString();
                    getVariables().put(name, value);
                    logger.debug("Loaded bootstrap parameter: " + name + " - " + value);
                } else
                    logger.warn("Warning. Not-string bootstrap value found for parameter: " + name);
            }
        }
    }

    /*
     *  JsonParser parser = new JsonParser();

                try {
                    HttpEntity entity = response.getEntity();
                    if ( entity != null ) {
                        String entity_string = EntityUtils.toString(entity);
                        logger.info("ES: Received " + entity_string.length() + " bytes");
                        logger.debug("ES Response: " + entity_string);
                        JsonElement response_element = parser.parse(entity_string);

                        String nextModuleName = null;
                        //boolean dynamicRouting = false;
                        if ( esStep.getDoRouting() && "responseBased".equals(esStep.getNextType()) ) {
                            //dynamicRouting = true;
                            String moduleLabel = evaluateExtractorExpression(esStep.getNextValueExtractor(), response_element);
                            nextModuleNam
                         */

}
