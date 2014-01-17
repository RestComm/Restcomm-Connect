package org.mobicents.servlet.restcomm.rvd.model.client;

import java.util.List;

public class ExternalServiceStep extends Step {
    private String url;
    private List<Assignment> assignments;
    private String next;
    private String nextVariable;
    private Boolean doRouting;
    private String nextType;

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
}
