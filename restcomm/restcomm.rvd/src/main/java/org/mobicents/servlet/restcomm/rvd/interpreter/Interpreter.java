package org.mobicents.servlet.restcomm.rvd.interpreter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.mobicents.servlet.restcomm.rvd.exceptions.UndefinedTarget;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.RVDUnsupportedHandlerVerb;
import org.mobicents.servlet.restcomm.rvd.model.PlayStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.SayStepConverter;
import org.mobicents.servlet.restcomm.rvd.model.StepJsonDeserializer;
import org.mobicents.servlet.restcomm.rvd.model.client.DialStep;
import org.mobicents.servlet.restcomm.rvd.model.client.GatherStep;
import org.mobicents.servlet.restcomm.rvd.model.client.PlayStep;
import org.mobicents.servlet.restcomm.rvd.model.client.SayStep;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlDialStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlGatherStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlHungupStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlPlayStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlResponse;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlSayStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;
import org.mobicents.servlet.restcomm.rvd.model.server.ProjectOptions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    public Interpreter() {
        xstream = new XStream();
        xstream.registerConverter(new SayStepConverter());
        xstream.registerConverter(new PlayStepConverter());
        xstream.alias("Response", RcmlResponse.class);
        xstream.addImplicitCollection(RcmlResponse.class, "steps");
        xstream.alias("Say", RcmlSayStep.class);
        xstream.alias("Play", RcmlPlayStep.class);
        xstream.alias("Gather", RcmlGatherStep.class);
        xstream.alias("Dial", RcmlDialStep.class);
        xstream.alias("Hangup", RcmlHungupStep.class);
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
        xstream.aliasField("Number", RcmlDialStep.class, "number");
        xstream.aliasField("Client", RcmlDialStep.class, "client");
        xstream.aliasField("Conference", RcmlDialStep.class, "conference");
        xstream.aliasField("Uri", RcmlDialStep.class, "sipuri");

        // xstream.aliasField(alias, definedIn, fieldName);
        gson = new GsonBuilder().registerTypeAdapter(Step.class, new StepJsonDeserializer()).create();
    }

    public String interpret(String targetParam, String projectBasePath, String appName, HttpServletRequest httpRequest)
            throws IOException, RVDUnsupportedHandlerVerb, UndefinedTarget {
        this.projectBasePath = projectBasePath;
        this.appName = appName;
        this.httpRequest = httpRequest;

        if (targetParam == null || "".equals(targetParam)) {
            // No target has been spacified. Load the default from project file
            String projectfile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data"
                    + File.separator + "project"));
            ProjectOptions projectOptions = gson.fromJson(projectfile_json, new TypeToken<ProjectOptions>() {
            }.getType());
            targetParam = projectOptions.getDefaultTarget();
            if (targetParam == null)
                throw new UndefinedTarget();
            System.out.println("override default target to " + targetParam);
        }
        return interpret(targetParam);

    }

    private String interpret(String targetParam) throws RVDUnsupportedHandlerVerb, IOException {

        System.out.println("starting interpeter for " + targetParam);

        target = Interpreter.parseTarget(targetParam);

        // TODO make sure all the required components of the target are available here

        if (target.action != null) {
            // Event handling
            handleAction(target.action);
        } else {
            // RCML Generation

            RcmlResponse rcmlModel = new RcmlResponse();
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
                    rcmlModel.steps.add(renderStep(step));
                }
            }

            rcmlResult = xstream.toXML(rcmlModel);
        }

        return rcmlResult; // this is in case of an error
    }

    public Step loadStep(String stepname) throws IOException {
        String stepfile_json = FileUtils.readFileToString(new File(projectBasePath + File.separator + "data/"
                + target.getNodename() + "." + stepname));
        Step step = gson.fromJson(stepfile_json, Step.class);

        return step;
    }

    public void handleAction(String action) throws IOException, RVDUnsupportedHandlerVerb {

        System.out.println("handling action ");

        Step step = loadStep(target.stepname);
        // <Gather/>
        if (step.getClass().equals(GatherStep.class)) {
            GatherStep gatherStep = (GatherStep) step;

            if ("menu".equals(gatherStep.getGatherType())) {

                boolean handled = false;
                for (GatherStep.Mapping mapping : gatherStep.getMappings()) {
                    Integer digits = Integer.parseInt(httpRequest.getParameter("Digits"));

                    System.out.println("checking digits: " + mapping.getDigits() + " - " + digits);

                    if (mapping.getDigits() != null && mapping.getDigits().equals(digits)) {
                        // seems we found out menu selection
                        System.out.println("seems we found out menu selection");
                        interpret(mapping.getNext());
                        handled = true;
                    }
                }
                if (!handled) {
                    interpret(target.nodename + "." + target.stepname);
                }
            }
            if ("collectdigits".equals(gatherStep.getGatherType())) {

                String variableName = gatherStep.getCollectVariable();
                variables.put(variableName, httpRequest.getParameter("Digits")); // put the string directly
                interpret(gatherStep.getNext());
            }
        } else {
            throw new RVDUnsupportedHandlerVerb();
        }
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

    public RcmlDialStep renderDialStep(DialStep step) {

        RcmlDialStep rcmlStep = new RcmlDialStep();
        if ("number".equals(step.getDialType()) && step.getNumber() != null && !"".equals(step.getNumber()))
            rcmlStep.setNumber(populateVariables(step.getNumber()));
        else if ("client".equals(step.getDialType()) && step.getClient() != null && !"".equals(step.getClient()))
            rcmlStep.setClient(step.getClient());
        else if ("conference".equals(step.getDialType()) && step.getConference() != null && !"".equals(step.getConference()))
            rcmlStep.setConference(step.getConference());
        else if ("sipuri".equals(step.getDialType()) && step.getSipuri() != null && !"".equals(step.getSipuri()))
            rcmlStep.setSipuri(step.getSipuri());
        // TODO else ...

        return rcmlStep;
    }

    public RcmlStep renderStep(Step step) {
        if ("say".equals(step.getKind()))
            return renderSayStep((SayStep) step);
        else if ("play".equals(step.getKind()))
            return renderPlayStep((PlayStep) step);
        else if ("gather".equals(step.getKind()))
            return renderGatherStep((GatherStep) step);
        else if ("dial".equals(step.getKind()))
            return renderDialStep((DialStep) step);
        else if ("hungup".equals(step.getKind()))
            return new RcmlHungupStep(); // trivial implementation. No need for seperate function
        else
            return null; // TODO Raise an exception here!
    }

    public RcmlSayStep renderSayStep(SayStep step) {

        RcmlSayStep sayStep = new RcmlSayStep();
        sayStep.setPhrase(populateVariables(step.getPhrase()));
        sayStep.setVoice(step.getVoice());
        sayStep.setLanguage(step.getLanguage());
        sayStep.setLoop(step.getLoop());

        return sayStep;
    }

    public RcmlPlayStep renderPlayStep(PlayStep step) {
        RcmlPlayStep playStep = new RcmlPlayStep();
        String url = "";
        if ("local".equals(step.getPlayType()))
            url = httpRequest.getContextPath() + "/workspace/" + appName + "/wavs/" + step.getWavLocalFilename();
        else
            url = step.getWavUrl();

        System.out.println("play url: " + url);
        playStep.setWavurl(url);

        return playStep;
    }

    public RcmlGatherStep renderGatherStep(GatherStep step) {

        RcmlGatherStep rcmlStep = new RcmlGatherStep();
        String newtarget = target.nodename + "." + step.getName() + ".handle";
        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("target", newtarget);
        String action = buildAction(pairs);

        rcmlStep.setAction(action);
        rcmlStep.setTimeout(step.getTimeout());
        if (step.getFinishOnKey() != null && !"".equals(step.getFinishOnKey()))
            rcmlStep.setFinishOnKey(step.getFinishOnKey());
        rcmlStep.setMethod(step.getMethod());
        rcmlStep.setNumDigits(step.getNumDigits());

        for (String nestedStepName : step.getStepnames())
            rcmlStep.getSteps().add(renderStep(step.getSteps().get(nestedStepName)));

        return rcmlStep;
    }

    private String buildAction(Map<String, String> pairs) {
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
}
