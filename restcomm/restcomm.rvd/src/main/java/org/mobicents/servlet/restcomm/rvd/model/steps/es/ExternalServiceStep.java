package org.mobicents.servlet.restcomm.rvd.model.steps.es;

import java.util.List;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.client.UrlParam;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;


public class ExternalServiceStep extends Step {

    private String url; // supports RVD variable expansion when executing the HTTP request
    private String method;
    private String username;
    private String password;
    private List<UrlParam> urlParams;
    private List<Assignment> assignments;
    private String next;
    private String nextVariable;
    private Boolean doRouting;
    private String nextType;
    private ValueExtractor nextValueExtractor;
    private List<RouteMapping> routeMappings;
    //private String defaultNext;
    private String exceptionNext;


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

    public List<RouteMapping> getRouteMappings() {
        return routeMappings;
    }

    public void setRouteMappings(List<RouteMapping> routeMappings) {
        this.routeMappings = routeMappings;
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

    public String getMethod() {
        return method;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }


   /* public String getDefaultNext() {
        return defaultNext;
    }

    public void setDefaultNext(String defaultNext) {
        this.defaultNext = defaultNext;
    }
    */

    public String getExceptionNext() {
        return exceptionNext;
    }

    public void setExceptionNext(String exceptionNext) {
        this.exceptionNext = exceptionNext;
    }

    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        // TODO Auto-generated method stub
        return null;
    }
}
