package org.mobicents.servlet.restcomm.rvd.model.client;

import java.util.List;
import java.util.Map;

public class GatherStep extends Step {
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

}
