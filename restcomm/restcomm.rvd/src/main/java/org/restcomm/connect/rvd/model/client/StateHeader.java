package org.restcomm.connect.rvd.model.client;

public class StateHeader {
    // application logging settings for this project. If not null logging is enabled.
    // We are using an object instead of a boolean to easily add properties in the future
    public static class Logging {}

    String projectKind;
    String startNodeName;
    String version;
    String owner; // the Restcomm user id that owns the project or null if it has no owner at all. Added in 7.1.6 release
    //Logging logging; - moved to the separate 'settings' file
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

    public void setVersion(String version) {
        this.version = version;
    }
}