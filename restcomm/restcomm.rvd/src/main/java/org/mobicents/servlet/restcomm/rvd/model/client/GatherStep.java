package org.mobicents.servlet.restcomm.rvd.model.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlGatherStep;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class GatherStep extends Step {

    static final Logger logger = Logger.getLogger(BuildService.class.getName());

    private String action;
    private String method;
    private Integer timeout;
    private String finishOnKey;
    private Integer numDigits;
    private Map<String, Step> steps;
    private List<String> stepnames;
    private String next;
    private List<Mapping> mappings;
    private String collectVariable;
    private String gatherType;
    private Iface iface;

    public static class Mapping {
        private Integer digits;
        private String next;

        public Integer getDigits() {
            return digits;
        }

        public void setDigits(Integer digits) {
            this.digits = digits;
        }

        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }

    }

    public static class Iface {
        private Boolean advancedView;
        private Boolean optionsVisible;

        public Boolean getAdvancedView() {
            return advancedView;
        }

        public void setAdvancedView(Boolean advancedView) {
            this.advancedView = advancedView;
        }

        public Boolean getOptionsVisible() {
            return optionsVisible;
        }

        public void setOptionsVisible(Boolean optionsVisible) {
            this.optionsVisible = optionsVisible;
        }

    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getFinishOnKey() {
        return finishOnKey;
    }

    public void setFinishOnKey(String finishOnKey) {
        this.finishOnKey = finishOnKey;
    }

    public Integer getNumDigits() {
        return numDigits;
    }

    public void setNumDigits(Integer numDigits) {
        this.numDigits = numDigits;
    }

    public Map<String, Step> getSteps() {
        return steps;
    }

    public void setSteps(Map<String, Step> steps) {
        this.steps = steps;
    }

    public List<String> getStepnames() {
        return stepnames;
    }

    public void setStepnames(List<String> stepnames) {
        this.stepnames = stepnames;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public List<Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    public String getCollectVariable() {
        return collectVariable;
    }

    public void setCollectVariable(String collectVariable) {
        this.collectVariable = collectVariable;
    }

    public String getGatherType() {
        return gatherType;
    }

    public void setGatherType(String gatherType) {
        this.gatherType = gatherType;
    }

    public Iface getIface() {
        return iface;
    }

    public void setIface(Iface iface) {
        this.iface = iface;
    }
    public RcmlGatherStep render(Interpreter interpreter) throws InterpreterException {

        RcmlGatherStep rcmlStep = new RcmlGatherStep();
        String newtarget = interpreter.getTarget().getNodename() + "." + getName() + ".handle";
        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("target", newtarget);
        String action = interpreter.buildAction(pairs);

        rcmlStep.setAction(action);
        rcmlStep.setTimeout(getTimeout());
        if (getFinishOnKey() != null && !"".equals(getFinishOnKey()))
            rcmlStep.setFinishOnKey(getFinishOnKey());
        rcmlStep.setMethod(getMethod());
        rcmlStep.setNumDigits(getNumDigits());

        for (String nestedStepName : getStepnames())
            rcmlStep.getSteps().add(getSteps().get(nestedStepName).render(interpreter));

        return rcmlStep;
    }
    public void handleAction(Interpreter interpreter) throws InterpreterException, StorageException {
        logger.debug("handling gather action");
        if ("menu".equals(getGatherType())) {

            boolean handled = false;
            for (GatherStep.Mapping mapping : getMappings()) {
                Integer digits = Integer.parseInt(interpreter.getRequestParameters().get("Digits") );  //getHttpRequest().getParameter("Digits"));

                logger.debug("checking digits: " + mapping.getDigits() + " - " + digits);

                if (mapping.getDigits() != null && mapping.getDigits().equals(digits)) {
                    // seems we found out menu selection
                    logger.debug("seems we found out menu selection");
                    interpreter.interpret(mapping.getNext(),null);
                    handled = true;
                }
            }
            if (!handled) {
                interpreter.interpret(interpreter.getTarget().getNodename() + "." + interpreter.getTarget().getStepname(),null);
            }
        }
        if ("collectdigits".equals(getGatherType())) {

            String variableName = getCollectVariable();
            interpreter.getVariables().put(variableName, interpreter.getRequestParameters().get("Digits"));  //getHttpRequest().getParameter("Digits")); // put the string directly
            interpreter.interpret(getNext(),null);
        }
    }
}
