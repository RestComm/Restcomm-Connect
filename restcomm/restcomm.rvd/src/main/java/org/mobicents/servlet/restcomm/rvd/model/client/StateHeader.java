package org.mobicents.servlet.restcomm.rvd.model.client;

public class StateHeader {
    String projectKind;
    String startNodeName;
    String version;
    public StateHeader() {
    }

    public StateHeader(String projectKind, String startNodeName, String version) {
        super();
        this.projectKind = projectKind;
        this.startNodeName = startNodeName;
        this.version = version;
    }

    public String getProjectKind() {
        return projectKind;
    }
    public String getStartNodeName() {
        return startNodeName;
    }
    public String getVersion() {
        return version;
    }
}