package org.mobicents.servlet.restcomm.rvd.model;

public class RvdConfig {
    private String workspaceLocation;
    private String sslMode;
    private String restcommBaseUrl;

    public RvdConfig() {
    }

    public RvdConfig(String workspaceLocation, String restcommPublicIp, String sslMode) {
        super();
        this.workspaceLocation = workspaceLocation;
        this.sslMode = sslMode;
    }

    public String getWorkspaceLocation() {
        return workspaceLocation;
    }

    public String getSslMode() {
        return sslMode;
    }

    public String getRestcommBaseUrl() {
        return restcommBaseUrl;
    }
}
