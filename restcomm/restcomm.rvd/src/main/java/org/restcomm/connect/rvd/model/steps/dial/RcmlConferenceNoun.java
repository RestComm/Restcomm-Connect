package org.restcomm.connect.rvd.model.steps.dial;


public class RcmlConferenceNoun extends RcmlNoun {
    Boolean muted;
    Boolean beep;
    Boolean startConferenceOnEnter;
    Boolean endConferenceOnExit;
    String waitUrl;
    String waitMethod;
    Integer maxParticipants;
    String destination;
    public String getDestination() {
        return destination;
    }
    public void setDestination(String destination) {
        this.destination = destination;
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
    public String getWaitMethod() {
        return waitMethod;
    }
    public void setWaitMethod(String waitMethod) {
        this.waitMethod = waitMethod;
    }
    public Integer getMaxParticipants() {
        return maxParticipants;
    }
    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
}
