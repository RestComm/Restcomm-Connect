package org.mobicents.servlet.restcomm.rvd.model.rcml;

import java.util.ArrayList;
import java.util.List;

public class RcmlDialStep extends RcmlStep {
    private List<RcmlNoun> nouns = new ArrayList<RcmlNoun>();
    private String action;
    private String method;
    private String timeout;
    private String timeLimit;
    private String callerId;

    public List<RcmlNoun> getNouns() {
        return nouns;
    }

    public void setNouns(List<RcmlNoun> nouns) {
        this.nouns = nouns;
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

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(String timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }


}
