package org.restcomm.connect.rvd.model.steps.dial;

import java.util.HashMap;
import java.util.Map;

import org.restcomm.connect.rvd.utils.RvdUtils;
import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;

public class ConferenceDialNoun extends DialNoun {
    private String destination;
    private Boolean muted;
    private Boolean beep;
    private Boolean startConferenceOnEnter;
    private Boolean endConferenceOnExit;
    private String waitUrl;
    private String waitMethod;
    private String waitModule;
    private Integer maxParticipants;
    private String nextModule;

    public String getWaitMethod() {
        return waitMethod;
    }
    public void setWaitMethod(String waitMethod) {
        this.waitMethod = waitMethod;
    }
    public String getDestination() {
        return destination;
    }
    public void setDestination(String destination) {
        this.destination = destination;
    }
    public String getNextModule() {
        return nextModule;
    }
    public void setNextModule(String nextModule) {
        this.nextModule = nextModule;
    }
    public Boolean getMuted() {
        return muted;
    }
    public void setMuted(Boolean muted) {
        this.muted = muted;
    }
    public Boolean getBeep() {
        return beep;
    }
    public void setBeep(Boolean beep) {
        this.beep = beep;
    }
    public Boolean getStartConferenceOnEnter() {
        return startConferenceOnEnter;
    }
    public void setStartConferenceOnEnter(Boolean startConferenceOnEnter) {
        this.startConferenceOnEnter = startConferenceOnEnter;
    }
    public Boolean getEndConferenceOnExit() {
        return endConferenceOnExit;
    }
    public void setEndConferenceOnExit(Boolean endConferenceOnExit) {
        this.endConferenceOnExit = endConferenceOnExit;
    }
    public String getWaitUrl() {
        return waitUrl;
    }
    public void setWaitUrl(String waitUrl) {
        this.waitUrl = waitUrl;
    }
    public String getWaitModule() {
        return waitModule;
    }
    public void setWaitModule(String waitModule) {
        this.waitModule = waitModule;
    }
    public Integer getMaxParticipants() {
        return maxParticipants;
    }
    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    @Override
    public RcmlNoun render(Interpreter interpreter) throws InterpreterException {
        RcmlConferenceNoun rcmlNoun = new RcmlConferenceNoun();

        // set waitUrl
        if ( ! RvdUtils.isEmpty(getWaitModule()) ) {
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("target", getWaitModule());
            String action = interpreter.buildAction(pairs);
            rcmlNoun.setWaitUrl( interpreter.getRvdSettings().getApplicationsRelativeUrl() + "/" + interpreter.getAppName() + "/" + action  );
        } else
        if ( ! RvdUtils.isEmpty(getWaitUrl())) {
            rcmlNoun.setWaitUrl(interpreter.populateVariables(getWaitUrl()));
        }

        rcmlNoun.setBeep(getBeep());
        rcmlNoun.setMuted(getMuted());
        rcmlNoun.setEndConferenceOnExit(getEndConferenceOnExit());
        rcmlNoun.setStartConferenceOnEnter(getStartConferenceOnEnter());
        rcmlNoun.setMaxParticipants(getMaxParticipants());
        rcmlNoun.setWaitMethod(getWaitMethod());
        rcmlNoun.setDestination( interpreter.populateVariables(getDestination() ));

        return rcmlNoun;
    }
}
