package org.restcomm.connect.rvd.model;

public class ProjectSettings {

    Boolean logging;
    Boolean loggingRCML;

    public static ProjectSettings createDefault() {
        ProjectSettings instance = new ProjectSettings();
        instance.logging = false;
        instance.loggingRCML = false;
        return instance;
    }

    public ProjectSettings() {
        // TODO Auto-generated constructor stub
    }

    public Boolean getLogging() {
        return logging;
    }

    public Boolean getLoggingRCML() {
        return loggingRCML;
    }


}
