package org.mobicents.servlet.restcomm.rvd.interpreter;

import java.io.File;
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

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.exceptions.UndefinedTarget;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.BadExternalServiceResponse;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.ErrorParsingExternalServiceUrl;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.InvalidAccessOperationAction;
import org.mobicents.servlet.restcomm.rvd.model.FaxStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.PlayStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.RedirectStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.SayStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.SmsStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.StepJsonDeserializer;
import org.mobicents.servlet.restcomm.rvd.model.client.AccessOperation;
import org.mobicents.servlet.restcomm.rvd.model.client.ExternalServiceStep;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.client.UrlParam;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlDialStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlFaxStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlGatherStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlHungupStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlPauseStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlPlayStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlRecordStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlRedirectStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlRejectStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlResponse;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlSayStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlSmsStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;
import org.mobicents.servlet.restcomm.rvd.model.server.NodeName;
import org.mobicents.servlet.restcomm.rvd.model.server.ProjectOptions;
import org.mobicents.servlet.restcomm.rvd.model.client.Assignment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.XStream;

public class Interpreter {

    private XStream xstream;
    private Gson gson;
    private Target target;
    private String projectBasePath;
    private String appName;
    private HttpServletRequest httpRequest;
    private String rcmlResult;
    private Map<String, String> variables = new HashMap<String, String>();
    private List<NodeName> nodeNames;

    public Interpreter() {
        xstream = new XStream();
        xstream.registerConverter(new SayStepConverter());
        xstream.registerConverter(new PlayStepConverter());
        xstream.registerConverter(new RedirectStepConverter());
        xstream.registerConverter(new SmsStepConverter());
        xstream.registerConverter(new FaxStepConverter());
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
        xstream.addImplicitCollection(RcmlGatherStep.class, "steps");
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
        xstream.aliasField("Number", RcmlDialStep.class, "number");
        xstream.aliasField("Client", RcmlDialStep.class, "client");
        xstream.aliasField("Conference", RcmlDialStep.class, "conference");
        xstream.aliasField("Uri", RcmlDialStep.class, "sipuri");

        // xstream.aliasField(alias, definedIn, fieldName);
        gson = new GsonBuilder().registerTypeAdapter(Step.class, new StepJsonDeserializer()).create();
    }


    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }


    public void setHttpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
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


    public String interpret(String targetParam, String projectBasePath, String appName, HttpServletRequest httpRequest)
            throws IOException, InterpreterException {
        this.projectBasePath = projectBasePath;
        this.appName = appName;
        this.httpRequest = httpRequest;

        String projectfile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data" + File.separator + "project"));
        ProjectOptions projectOptions = gson.fromJson(projectfile_json, new TypeToken<ProjectOptions>() {
        }.getType());
        nodeNames = projectOptions.getNodeNames();

        if (targetParam == null || "".equals(targetParam)) {
            // No target has been specified. Load the default from project file
            targetParam = projectOptions.getDefaultTarget();
            if (targetParam == null)
                throw new UndefinedTarget();
            System.out.println("override default target to " + targetParam);
        }
        return interpret(targetParam, null);

    }

    public String interpret(String targetParam, RcmlResponse rcmlModel ) throws IOException, InterpreterException {

        System.out.println("starting interpeter for " + targetParam);

        target = Interpreter.parseTarget(targetParam);

        // TODO make sure all the required components of the target are available here

        if (target.action != null) {
            // Event handling
            loadStep(target.stepname).handleAction(this);
        } else {
            // RCML Generation

            if (rcmlModel == null )
                rcmlModel = new RcmlResponse();
            String nodefile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data/"
                    + target.getNodename() + ".node"));
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

    private Step loadStep(String stepname) throws IOException, InterpreterException {
        String stepfile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data/"
                + target.getNodename() + "." + stepname));
        Step step = gson.fromJson(stepfile_json, Step.class);

        return step;
    }

    private String evaluateAssignmentExpression( Assignment assignment, JsonElement response_element) throws InvalidAccessOperationAction, BadExternalServiceResponse {
        String value = "";

        JsonElement element = response_element;
        for ( AccessOperation operation : assignment.getAccessOperations() ) {
            if ( element == null )
                throw new BadExternalServiceResponse();

            if ( "object".equals(operation.getKind()) ) {
                if ("propertyNamed".equals(operation.getAction()) )
                    element = element.getAsJsonObject().get( operation.getProperty() );
                else
                    throw new InvalidAccessOperationAction();
            } else
            if ( "array".equals(operation.getKind()) ) {
                if ("itemAtPosition".equals(operation.getAction()) )
                    element = element.getAsJsonArray().get( operation.getPosition() );
                else
                    throw new InvalidAccessOperationAction();
            } else
            if ( "string".equals(operation.getKind()) ) {
                value = element.getAsString();
            }
        }

        return value;
    }

    /**
     * If the step is capable of executing like ExternalService steps it executes them
     * @param step
     * @throws IOException
     * @throws ClientProtocolException
     * @return String Break the module being currently rendered and continue with rendering the named target.
     * @throws ErrorParsingExternalServiceUrl
     */
    private String processStep(Step step) throws IOException, InterpreterException {
        if (step.getClass().equals(ExternalServiceStep.class)) {

            ExternalServiceStep esStep = (ExternalServiceStep) step;

            CloseableHttpClient client = HttpClients.createDefault();
            //String url = populateVariables(esStep.getUrl());

            URI url;
            try {
                URIBuilder uri_builder = new URIBuilder(esStep.getUrl());
                for ( UrlParam urlParam : esStep.getUrlParams() ) {
                    uri_builder.addParameter(urlParam.getName(), populateVariables(urlParam.getValue()) );
                }
                url = uri_builder.build();
            } catch (URISyntaxException e) {
                throw new ErrorParsingExternalServiceUrl( "URL: " + esStep.getUrl(), e);
            }

            System.out.println( "External Service url: " + url);
            HttpGet get = new HttpGet( url );
            CloseableHttpResponse response = client.execute( get );

            JsonParser parser = new JsonParser();

            try {
                //System.out.println(response);

                HttpEntity entity = response.getEntity();
                if ( entity != null ) {
                    String entity_string = EntityUtils.toString(entity);
                    JsonElement response_element = parser.parse(entity_string);

                    // Initialize the variables of the assignments one by one
                    for ( Assignment assignment : esStep.getAssignments() ) {
                        //try {
                            String value = evaluateAssignmentExpression(assignment, response_element);
                            variables.put(assignment.getDestVariable(), value );
                        //} catch ( BadExternalServiceResponse e ) {
                        //    e.printStackTrace();
                        //}
                    }
                    System.out.println("variables after processing ExternalService step: " + variables.toString() );
                }

            } finally {
                response.close();
            }

            if ( esStep.getDoRouting() ) {
                String nextLabel = "";
                if ( "fixed".equals( esStep.getNextType() ) )
                    nextLabel = esStep.getNext();
                else
                if ( "variable".equals( esStep.getNextType() ))
                    nextLabel = variables.get( esStep.getNextVariable() );
                return getNodeNameByLabel(nextLabel);
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
}
