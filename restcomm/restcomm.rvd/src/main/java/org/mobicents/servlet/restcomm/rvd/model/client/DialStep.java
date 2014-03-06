package org.mobicents.servlet.restcomm.rvd.model.client;

import java.util.List;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlDialStep;

public class DialStep extends Step {
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



        /*if ("number".equals(getDialType()) && getNumber() != null && !"".equals(getNumber()))
            rcmlStep.setNumber(interpreter.populateVariables(getNumber()));
        else if ("client".equals(getDialType()) && getClient() != null && !"".equals(getClient()))
            rcmlStep.setClient(getClient());
        else if ("conference".equals(getDialType()) && getConference() != null && !"".equals(getConference()))
            rcmlStep.setConference(getConference());
        else if ("sipuri".equals(getDialType()) && getSipuri() != null && !"".equals(getSipuri()))
            rcmlStep.setSipuri(getSipuri());
        // TODO else ...
*/
        return rcmlStep;
    }

}
