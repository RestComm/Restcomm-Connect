package org.restcomm.connect.rvd.model.client;

import org.restcomm.connect.rvd.ProjectService;

public class ProjectItem {

    private String name;
    private String startUrl;
    private String kind;
    private ProjectService.Status status;

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

    public ProjectService.Status getStatus() {
        return status;
    }

    public void setStatus(ProjectService.Status status) {
        this.status = status;
    }
}
