package org.mobicents.servlet.restcomm.rvd.model.steps.es;

import java.util.List;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.client.UrlParam;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;


public class ExternalServiceStep extends Step {

    private String url;
    private List<UrlParam> urlParams;
    private List<Assignment> assignments;
    private String next;
    private String nextVariable;
    private Boolean doRouting;
    private String nextType;
    private ValueExtractor nextValueExtractor;

    public ValueExtractor getNextValueExtractor() {
        return nextValueExtractor;
    }

    public void setNextValueExtractor(ValueExtractor nextValueExtractor) {
        this.nextValueExtractor = nextValueExtractor;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public Boolean getDoRouting() {
        return doRouting;
    }

    public void setDoRouting(Boolean doRouting) {
        this.doRouting = doRouting;
    }

    public String getNextType() {
        return nextType;
    }

    public void setNextType(String nextType) {
        this.nextType = nextType;
    }

    public String getNextVariable() {
        return nextVariable;
    }

    public void setNextVariable(String nextVariable) {
        this.nextVariable = nextVariable;
    }
    public List<UrlParam> getUrlParams() {
        return this.urlParams;
    }

    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        // TODO Auto-generated method stub
        return null;
    }
}
