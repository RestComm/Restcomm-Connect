package org.mobicents.servlet.restcomm.rvd.model.client;

public class StateHeader {
    String projectKind;
    String startNodeName;
    String version;
    String owner; // the Restcomm user id that owns the project or null if it has no owner at all. Added in 7.1.6 release
    public StateHeader() {
    }

    public StateHeader(String projectKind, String startNodeName, String version) {
        super();
        this.projectKind = projectKind;
        this.startNodeName = startNodeName;
        this.version = version;
    }

    public StateHeader(String projectKind, String startNodeName, String version, String owner) {
        super();
        this.projectKind = projectKind;
        this.startNodeName = startNodeName;
        this.version = version;
        this.owner = owner;
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
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner2) {
        this.owner = owner2;
    }

}