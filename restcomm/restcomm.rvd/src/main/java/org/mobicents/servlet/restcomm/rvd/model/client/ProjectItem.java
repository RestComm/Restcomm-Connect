package org.mobicents.servlet.restcomm.rvd.model.client;

import org.mobicents.servlet.restcomm.rvd.ProjectService.Status;

public class ProjectItem {

    private String name;
    private String startUrl;
    private String kind;
    private Status status;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public void setStartUrl(String startUrl) {
        this.startUrl = startUrl;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
