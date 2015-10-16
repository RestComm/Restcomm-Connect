package org.mobicents.servlet.restcomm.rvd.model;

public class RvdConfig {
    private String workspaceLocation;
    private String restcommPublicIp;
    private String sslMode;

    public RvdConfig() {
    }

    public RvdConfig(String workspaceLocation, String restcommPublicIp, String sslMode) {
        super();
        this.workspaceLocation = workspaceLocation;
        this.restcommPublicIp = restcommPublicIp;
        this.sslMode = sslMode;
    }

    public String getWorkspaceLocation() {
        return workspaceLocation;
    }

    public String getRestcommPublicIp() {
        return restcommPublicIp;
    }

    public String getSslMode() {
        return sslMode;
    }

}
