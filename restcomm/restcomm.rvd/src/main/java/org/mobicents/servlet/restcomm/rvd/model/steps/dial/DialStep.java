package org.mobicents.servlet.restcomm.rvd.model.steps.dial;

import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;

public class DialStep extends Step {
    static final Logger logger = Logger.getLogger(DialStep.class.getName());

    private List<DialNoun> dialNouns;
    private String action;
    private String method;
    private Integer timeout;
    private Integer timeLimit;
    private String callerId;
    private String nextModule;

    public String getNextModule() {
        return nextModule;
    }

    public void setNextModule(String nextModule) {
        this.nextModule = nextModule;
    }

    public List<DialNoun> getDialNouns() {
        return dialNouns;
    }

    public void setDialNouns(List<DialNoun> dialNouns) {
        this.dialNouns = dialNouns;
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

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }
    public RcmlDialStep render(Interpreter interpreter) throws InterpreterException {
        RcmlDialStep rcmlStep = new RcmlDialStep();

        for ( DialNoun noun: getDialNouns() ) {
            rcmlStep.getNouns().add( noun.render(interpreter) );
        }

        // set action (from nextModule)
        String moduleUrl = interpreter.moduleUrl(getNextModule());
        if ( moduleUrl != null )
            rcmlStep.setAction(moduleUrl);
        else {
            logger.warn("Tried to reference a non-existing module while building 'action' property: " + getNextModule() + ". It will be ignored.");
        }

        rcmlStep.setMethod(getMethod());
        rcmlStep.setTimeout(getTimeout() == null ? null : getTimeout().toString());
        rcmlStep.setTimeLimit(getTimeLimit() == null ? null : getTimeLimit().toString());
        rcmlStep.setCallerId(getCallerId());

        return rcmlStep;
    }

}
