package org.mobicents.servlet.restcomm.rvd.interpreter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.exceptions.ESRequestException;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.exceptions.UndefinedTarget;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.BadExternalServiceResponse;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.ErrorParsingExternalServiceUrl;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.InvalidAccessOperationAction;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.ReferencedModuleDoesNotExist;
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
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay.UssdSayRcml;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay.UssdSayStepConverter;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.XStream;

public class Interpreter {

    static final Logger logger = Logger.getLogger(BuildService.class.getName());

    private ProjectStorage projectStorage;
    private HttpServletRequest httpRequest;

    private XStream xstream;
    private Gson gson;
    private String targetParam;
    private Target target;
    private String appName;
    private Map<String,String> requestParameters; // parameters like digits, callSid etc.
    private String contextPath;

    private String rcmlResult;
    private Map<String, String> variables = new HashMap<String, String>();
    private List<NodeName> nodeNames;


    public Interpreter(ProjectStorage projectStorage, String targetParam, String appName, Map<String,String> requestParameters, String contextPath, HttpServletRequest httpRequest) {
        this.projectStorage = projectStorage;
        this.httpRequest = httpRequest;
        this.targetParam = targetParam;
        this.appName = appName;
        this.requestParameters = requestParameters;
        this.contextPath = contextPath;
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
        xstream.aliasField("Number", RcmlDialStep.class, "number");
        xstream.aliasField("Client", RcmlDialStep.class, "client");
        xstream.aliasField("Conference", RcmlDialStep.class, "conference");
        xstream.aliasField("Uri", RcmlDialStep.class, "sipuri");

        // xstream.aliasField(alias, definedIn, fieldName);
        gson = new GsonBuilder().registerTypeAdapter(Step.class, new StepJsonDeserializer()).create();
    }

    public String getAppName() {
        return appName;
    }


    public void setAppName(String appName) {
        this.appName = appName;
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


    public String interpret() throws StorageException {
        //String projectfile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data" + File.separator + "project"));
        String projectfile_json = projectStorage.loadProjectOptions(appName);
        ProjectOptions projectOptions = gson.fromJson(projectfile_json, new TypeToken<ProjectOptions>() {
        }.getType());
        nodeNames = projectOptions.getNodeNames();

        String response = null;
        try {
            if (targetParam == null || "".equals(targetParam)) {
                // No target has been specified. Load the default from project file
                targetParam = projectOptions.getDefaultTarget();
                if (targetParam == null)
                    throw new UndefinedTarget();
                logger.debug("override default target to " + targetParam);
            }

            response = interpret(targetParam, null);
        } catch (InterpreterException e) {
            logger.error(e.getMessage(), e);
            response = "<Response><Hangup/></Response>";
        }

        return response;
    }

    public Map<String, String> getRequestParameters() {
        return requestParameters;
    }


    public void setRequestParameters(Map<String, String> requestParameters) {
        this.requestParameters = requestParameters;
    }


    public String getContextPath() {
        return contextPath;
    }


    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }


    public String interpret(String targetParam, RcmlResponse rcmlModel ) throws InterpreterException, StorageException {

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
            //String nodefile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data/" + target.getNodename() + ".node"));
            String nodefile_json = projectStorage.loadNodeStepnames(appName, target.getNodename());//FileUtils.readFileToString(new File(projectBasePath + File.separator + "data/" + target.getNodename() + ".node"));
            List<String> nodeStepnames = gson.fromJson(nodefile_json, new TypeToken<List<String>>() {
            }.getType());

            // if no starting step has been specified in the target, use the first step of the node as default
            if (target.getStepname() == null && !nodeStepnames.isEmpty())
                target.setStepname(nodeStepnames.get(0));

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
                        return interpret(rerouteTo, rcmlModel);
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
     */
    private String processStep(Step step) throws InterpreterException {
        if (step.getClass().equals(ExternalServiceStep.class)) {

            try {

                ExternalServiceStep esStep = (ExternalServiceStep) step;

                CloseableHttpClient client = HttpClients.createDefault();
                //String url = populateVariables(esStep.getUrl());

                URI url;
                try {
                    URIBuilder uri_builder = new URIBuilder(esStep.getUrl());
                    if (uri_builder.getHost() == null ) {
                        logger.info("External Service: Relative url is used. Will override from http request to RVD controller");
                        // if this is a relative url fill in missing fields from the request
                        uri_builder.setScheme(httpRequest.getScheme());
                        uri_builder.setHost(httpRequest.getServerName());
                        uri_builder.setPort(httpRequest.getServerPort());
                        if (  ! uri_builder.getPath().startsWith("/") )
                            uri_builder.setPath("/" + uri_builder.getPath());
                    }

                    for ( UrlParam urlParam : esStep.getUrlParams() ) {
                        uri_builder.addParameter(urlParam.getName(), populateVariables(urlParam.getValue()) );
                    }
                    url = uri_builder.build();
                } catch (URISyntaxException e) {
                    throw new ErrorParsingExternalServiceUrl( "URL: " + esStep.getUrl(), e);
                }

                logger.info( "External Service: Requesting from url: " + url);
                HttpGet get = new HttpGet( url );
                CloseableHttpResponse response = client.execute( get );

                JsonParser parser = new JsonParser();

                try {
                    HttpEntity entity = response.getEntity();
                    if ( entity != null ) {
                        String entity_string = EntityUtils.toString(entity);
                        JsonElement response_element = parser.parse(entity_string);

                        String nextModuleName = null;
                        //boolean dynamicRouting = false;
                        if ( esStep.getDoRouting() && "responseBased".equals(esStep.getNextType()) ) {
                            //dynamicRouting = true;
                            String moduleLabel = evaluateExtractorExpression(esStep.getNextValueExtractor(), response_element);
                            nextModuleName = getNodeNameByLabel( moduleLabel );
                            if ( nextModuleName == null )
                                throw new ReferencedModuleDoesNotExist("No module found with label '" + moduleLabel + "'");

                            logger.debug( "Dynamic routing enabled. Chosen target: " + nextModuleName);
                            for ( Assignment assignment : esStep.getAssignments() ) {
                                logger.debug("working on variable " + assignment.getDestVariable() );
                                logger.debug( "moduleNameScope: " + assignment.getModuleNameScope());
                                if ( assignment.getModuleNameScope() == null || assignment.getModuleNameScope().equals(nextModuleName) ) {
                                    String value = evaluateExtractorExpression(assignment.getValueExtractor(), response_element);
                                    variables.put(assignment.getDestVariable(), value );
                                } else
                                    logger.debug("skipped assignment to " + assignment.getDestVariable() );
                            }
                        }  else {
                            for ( Assignment assignment : esStep.getAssignments() ) {
                                logger.debug("working on variable " + assignment.getDestVariable() );
                                String value = evaluateExtractorExpression(assignment.getValueExtractor(), response_element);
                                variables.put(assignment.getDestVariable(), value );
                            }

                        }
                        logger.debug("variables after processing ExternalService step: " + variables.toString() );
                        if ( esStep.getDoRouting() ) {
                            String next = "";
                            if ( "fixed".equals( esStep.getNextType() ) )
                                next = esStep.getNext();
                            else
                            if ( "responseBased".equals( esStep.getNextType() ))
                                next = nextModuleName;
                            if ( "".equals(next) )
                                throw new ReferencedModuleDoesNotExist("No module specified for ES routing");
                            return next;
                        }
                    }
                } catch (JsonSyntaxException e) {
                    throw new BadExternalServiceResponse("External Service request received a malformed JSON response" );
                } finally {
                    response.close();
                }

            }
            catch (IOException e) {
                throw new ESRequestException("Error processing ExternalService step " + step.getName(), e);
            }


        }
        return null;
    }

    /**
     * Processes a block of text typically used for <Say/>ing that may contain variable expressions. Replaces variable
     * expressions with their corresponding values from interpreter's variables map
     */
    public String populateVariables(String sourceText) {

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

            buffer.replace(v.position, v.position + v.variableName.length() + 1, replaceValue); // +1 is for the $ character
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
            query += key + "=" + pairs.get(key);
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
}
